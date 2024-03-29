#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "os/dev/button-hal.h"
#include "os/dev/leds.h"
#include "os/sys/log.h"
#include "mqtt-client.h"
#include "random.h"

#include <string.h>
#include <strings.h>
#include <sys/node-id.h>

#define LOG_MODULE "chlorine-control"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

// MQTT broker address
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
// Publish interval of sensed values
#define PUBLISH_INTERVAL	        (5 * CLOCK_SECOND)

/* Various states */
static uint8_t state;

#define STATE_INIT    		    0	// initial state
#define STATE_NET_OK    	    1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

//Declare the two protothreads: one for the sensing subsystem,
//the other for handling leds blinking
PROCESS_NAME(chlorine_control_process);
PROCESS_NAME(blinking_led);
AUTOSTART_PROCESSES(&chlorine_control_process, &blinking_led);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

#define BUFFER_SIZE 64
static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

//The MQTT buffers
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

PROCESS(chlorine_control_process, "Chlorine control process");

//sets the chlorine regulator ON/OFF
static bool increase_chlorine = false;

// min value and max value of chlorine level
#define MIN_CHLORINE 0
#define MAX_CHLORINE 20

// the currente chlorine level
static float chlorine_level = (float)1.5;

//variation of the chlorine level to simulate the sensing process
static float variation = 0;

// tracks if the access regulator system is in manual mode
static bool manual = false;

// the MQTT status
static mqtt_status_t status;
static char broker_address[CONFIG_IP_ADDR_STR_LEN];

//calculate a random float value between a specified range
int random_in_range(int a, int b) {
    int v = random_rand() % (b-a);
    return v + a;
}

//CONTIKI DOES NOT SUPPORT THE FLOAT FORMAT
// Return digits before point
unsigned short digitsBefore(float f) {
    return((unsigned short)f);
}
 
// Return digits after point
unsigned short digitsAfter(float f) {
    return(10*(f-digitsBefore(f)));
}

/* 
	Handling incoming messages from the collector
	If the chlorine controller receive ON, the chlorine regulator is activated
	If the chlorine controller receive OFF, the chlorine regulator is disactivated
*/
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

// MQTT event handler
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {
	switch(event) {
		case MQTT_EVENT_CONNECTED: {
			state = STATE_CONNECTED;
            LOG_INFO("MQTT connection compleated\n");
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

// let the actuator resource handle the manual mode
static void manual_handler(){
    manual = !manual;
	increase_chlorine = !increase_chlorine;
	if (increase_chlorine) {
    	LOG_INFO("[MANUAL] Chlorine regulator is ON\n");
    } else {
        LOG_INFO("[MANUAL] Chlorine regulator is OFF\n");
    }
}

/*
    when the chlorine regulator is OFF, the chlorine level decrease
    when the chlorine regulator is ON, if the chlorine level is in the correct rage (2.5 - 3) it remains in that range (may be caused by manual activation)
    when the chlorine regulator is ON, if the chlorine level is not in the correct rage it increments its value
*/
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
		if(chlorine_level >= 0){
			variation = (float)random_in_range(2, 5) * 0.1;
			chlorine_level = old_chlorine - variation;
			if (chlorine_level < 0)
			    chlorine_level = 0;
		}
	}

	LOG_INFO("New chlorine value: %u.%u\n", digitsBefore(chlorine_level), digitsAfter(chlorine_level));				
}


PROCESS_THREAD(chlorine_control_process, ev, data) {

	PROCESS_BEGIN();

	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &chlorine_control_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);

	// Type of the sensor considered
	static char sensorType[20] = "chlorineSensor";
			
	state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
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
				//topic subscription
				strcpy(sub_topic,"chlorine_regulator");
				status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
				if(status == MQTT_STATUS_OUT_QUEUE_FULL){
					LOG_ERR("Tried to subscribe but command queue was full!\n");
					PROCESS_EXIT();
				}
				state = STATE_SUBSCRIBED;
			}	  
			if(state == STATE_SUBSCRIBED) {
				// simulate sensed values and publishing of the information for the collector
				sprintf(pub_topic, "%s", "ppm");

				simulate_chlorine_level();

				if(ev == button_hal_press_event){
					manual_handler();
				}

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
static struct etimer buffer_led_timer;
static struct etimer led_on_timer;

PROCESS_THREAD(blinking_led, ev, data)
{
	PROCESS_BEGIN();

	etimer_set(&registration_led_timer, 1*CLOCK_SECOND);

	leds_set(LEDS_NUM_TO_MASK(LEDS_YELLOW));

	while(state != STATE_SUBSCRIBED){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&registration_led_timer)){
				leds_toggle(LEDS_NUM_TO_MASK(LEDS_YELLOW));
				etimer_restart(&registration_led_timer);
			}
		}
	}

	etimer_set(&buffer_led_timer, 7*CLOCK_SECOND);
	etimer_set(&led_on_timer, 1*CLOCK_SECOND);

	while(1){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&buffer_led_timer)){
				if(increase_chlorine){
					leds_on(LEDS_NUM_TO_MASK(LEDS_YELLOW));
				}
				leds_on(LEDS_NUM_TO_MASK(LEDS_GREEN));
				etimer_restart(&buffer_led_timer);
				etimer_restart(&led_on_timer);
			}
			if(etimer_expired(&led_on_timer)){
				leds_off(LEDS_NUM_TO_MASK(LEDS_YELLOW));
				leds_off(LEDS_NUM_TO_MASK(LEDS_GREEN));
			}
		}
	}
	PROCESS_END();
}
