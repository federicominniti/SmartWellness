#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "os/dev/leds.h"
#include "os/sys/log.h"
#include "mqtt-client.h"
#include "random.h"

#include <string.h>
#include <strings.h>

#include <sys/node-id.h>

#define LOG_MODULE "chlorine-control"

#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL	        (5 * CLOCK_SECOND)


static uint8_t state;

#define STATE_INIT    		    0
#define STATE_NET_OK    	    1
#define STATE_CONNECTING      	2
#define STATE_CONNECTED       	3
#define STATE_SUBSCRIBED      	4
#define STATE_DISCONNECTED    	5

PROCESS_NAME(chlorine_control_process);
PROCESS_NAME(blinking_led);
AUTOSTART_PROCESSES(&chlorine_control_process, &blinking_led);

#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

#define BUFFER_SIZE 64
static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

#define STATE_MACHINE_PERIODIC (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

PROCESS(chlorine_control_process, "Chlorine control process");

static bool increase_chlorine = false;
#define MIN_CHLORINE 0
#define MAX_CHLORINE 20
static float chlorine_level = (float)1.5;
static float variation = 0;
static bool manual = false;

static mqtt_status_t status;
static char broker_address[CONFIG_IP_ADDR_STR_LEN];

int random_in_range(int a, int b) {
    int v = random_rand() % (b-a);
    return v + a;
}

unsigned short digitsBefore(float f) {
    return((unsigned short)f);
}
 
unsigned short digitsAfter(float f) {
    return(10*(f-digitsBefore(f)));
}

static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	if(strcmp(topic, "chlorine_regulator") == 0) {
		if(strcmp((const char*) chunk, "ON") == 0) {
			increase_chlorine = true;
            LOG_INFO("Chlorine regulator switched ON\n");
		}
		else if(strcmp((const char*) chunk, "OFF") == 0) {
			increase_chlorine = false;
            LOG_INFO("Chlorine regulator switched OFF\n");
		}
	}
	else {
		LOG_ERR("Topic not valid!\n");
	}
}

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {
	switch(event) {
		case MQTT_EVENT_CONNECTED: {
			state = STATE_CONNECTED;
            LOG_INFO("MQTT connection completed\n");
			break;
		}
		case MQTT_EVENT_DISCONNECTED: {
			state = STATE_DISCONNECTED;
			process_poll(&chlorine_control_process);
            printf("MQTT disconnection occurred. Reason: %u\n", *((mqtt_event_t *)data));
			break;
		}
		case MQTT_EVENT_PUBLISH: {
			msg_ptr = data;
			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			break;
		}
		case MQTT_EVENT_SUBACK: {
			#if MQTT_311
                mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
                if(suback_event->success){
                    LOG_INFO("Application has subscribed to topic successfully\n");
                } 
                else {
                    LOG_ERR("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
                }
			#else
			    LOG_INFO("Application has subscribed to the topic\n");
			#endif
			break;
		}
		case MQTT_EVENT_UNSUBACK: {
			LOG_INFO("Application is unsubscribed to topic successfully\n");
			break;
		}
		case MQTT_EVENT_PUBACK: {
			LOG_INFO("Publishing complete.\n");
			break;
		}
		default:
			LOG_INFO("Application got a unhandled MQTT event: %i\n", event);
	}
}

static bool have_connectivity(void) {
	if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL) {
		return false;
	}
	return true;
}

static void manual_handler(){
    manual = !manual;
	increase_chlorine = !increase_chlorine;
}

static void simulate_chlorine_level(){
	float old_chlorine = chlorine_level;
	if(increase_chlorine) {
		if (old_chlorine >= 2.5 && old_chlorine <= 3.0) {
			chlorine_level = (float)random_in_range(25, 30) * 0.1;
		} else {
			variation = (float)random_in_range(2, 5) * 0.1;
			chlorine_level = old_chlorine + variation;
		}
			
	} else {
		if(chlorine_level != 0){
			variation = (float)random_in_range(2, 5) * 0.1;
			chlorine_level = old_chlorine - variation;
		}
	}

	LOG_INFO("New chlorine value: %u.%u\n", digitsBefore(chlorine_level), digitsAfter(chlorine_level));				
}


PROCESS_THREAD(chlorine_control_process, ev, data) {

	PROCESS_BEGIN();

	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	mqtt_register(&conn, &chlorine_control_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);

	static char sensorType[20] = "chlorineSensor";
			
	state=STATE_INIT;
				    
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

	while(1) {
		PROCESS_YIELD();

		if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL || ev == button_hal_press_event) {			  			  
			if(state==STATE_INIT) {
				if(have_connectivity()==true) { 
					state = STATE_NET_OK;
				}
			}   
			if(state == STATE_NET_OK) {
			  	memcpy(broker_address, broker_ip, strlen(broker_ip));
			  
			  	mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
						   (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
						   MQTT_CLEAN_SESSION_ON);
			  	state = STATE_CONNECTING;
                LOG_INFO("Connected to MQTT server\n"); 
			} 
			if(state==STATE_CONNECTED) {
				strcpy(sub_topic,"chlorine_regulator");
				status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
				if(status == MQTT_STATUS_OUT_QUEUE_FULL){
					LOG_ERR("Tried to subscribe but command queue was full!\n");
					PROCESS_EXIT();
				}
				state = STATE_SUBSCRIBED;
			}	  
			if(state == STATE_SUBSCRIBED) {
				sprintf(pub_topic, "%s", "ppm");
				//check the left button pression
				if(ev == button_hal_press_event){
					button_hal_button_t* btn = (button_hal_button_t*)data;
                    if (btn->unique_id == BOARD_BUTTON_HAL_INDEX_KEY_LEFT) {
                    	manual_handler();
                    }
				}

			    simulate_chlorine_level();

                snprintf(app_buffer, APP_BUFFER_SIZE, 
                            "{\"node\": %d, \"value\": %u.%u, \"manual\": %d, \"sensorType\": \"%s\"}", 
                            (unsigned int) node_id, digitsBefore(chlorine_level), digitsAfter(chlorine_level), 
                            (int)manual, sensorType);

				LOG_INFO("message: %s\n", app_buffer);

				mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), 
                                MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

			} 
			else if(state == STATE_DISCONNECTED) {
				LOG_ERR("Disconnected from MQTT broker\n");
				state = STATE_INIT;	
			}

			etimer_set(&periodic_timer, PUBLISH_INTERVAL);
		}
	}
	PROCESS_END();
}

PROCESS(blinking_led, "Led blinking process");

static struct etimer registration_led_timer;
static struct etimer regulator_led_timer;
static struct etimer led_on_timer;

PROCESS_THREAD(blinking_led, ev, data)
{
	PROCESS_BEGIN();

	etimer_set(&registration_led_timer, 1*CLOCK_SECOND);

	//the red led is blinking until the the device is still connecting to the border router or the collector
	leds_on(LEDS_RED);

	while(state != STATE_SUBSCRIBED){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&registration_led_timer)){
				leds_toggle(LEDS_RED);
				etimer_restart(&registration_led_timer);
			}
		}
	}

	leds_off(LEDS_RED);

	etimer_set(&regulator_led_timer, 7*CLOCK_SECOND);
	etimer_set(&led_on_timer, 1*CLOCK_SECOND);

	/*if the chlorine regulator is on the both the red and green leds are blinking, otherwise
      only the green led is blinking
    */
	while(1){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&regulator_led_timer)){
				if(increase_chlorine){
					leds_on(LEDS_RED);
				}
				leds_on(LEDS_GREEN);
				etimer_restart(&regulator_led_timer);
				etimer_restart(&led_on_timer);
			}
			if(etimer_expired(&led_on_timer)){
				leds_off(LEDS_RED);
				leds_off(LEDS_GREEN);
			}
		}
	}
	PROCESS_END();
}
