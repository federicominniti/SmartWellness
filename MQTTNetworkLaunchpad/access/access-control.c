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

#define LOG_MODULE "access-control"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL	        (5 * CLOCK_SECOND)

// We assume that the broker does not require authentication

/* Various states */
static uint8_t state;

#define STATE_INIT    		    0	// initial state
#define STATE_NET_OK    	    1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

PROCESS_NAME(access_control_process);
PROCESS_NAME(blinking_led);
AUTOSTART_PROCESSES(&access_control_process, &blinking_led);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

PROCESS(access_control_process, "Access control process");

enum Color {RED = 0, YELLOW = 1, GREEN = 2}; 
static enum Color light_color = GREEN;
static bool entrance_door_locked = false;

static int number_of_people = 0;
static bool manual = false;

static mqtt_status_t status;
static char broker_address[CONFIG_IP_ADDR_STR_LEN];

// Incoming message handling
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	if(strcmp(topic, "access_regulator") == 0) {
		if(strcmp((const char*) chunk, "0") == 0) {
			light_color = GREEN;
			LOG_INFO("Light Color green\n");
			if(entrance_door_locked){
				entrance_door_locked = false;
				LOG_INFO("Entrance door unlocked\n");
			}
        
		}
		else if(strcmp((const char*) chunk, "1") == 0) {
			light_color = YELLOW;
			LOG_INFO("Light Color yellow\n");
			if(entrance_door_locked){
				entrance_door_locked = false;
				LOG_INFO("Entrance door unlocked\n");
			}
		}
		else if(strcmp((const char*) chunk, "2") == 0) {
			light_color = RED;
			LOG_INFO("Light Color red\n");
			if(!entrance_door_locked){
				entrance_door_locked = true;
				LOG_INFO("Entrance door locked\n");
			}

			//Handling of collector chashes
			/*if(number_of_people > 15){
				LOG_INFO("%d people exited for security reason\n", (number_of_people-15));
				number_of_people = 15;
			}*/
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
            LOG_INFO("MQTT connection completed\n");
			break;
		}
		case MQTT_EVENT_DISCONNECTED: {
			state = STATE_DISCONNECTED;
			process_poll(&access_control_process);
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
	entrance_door_locked = !entrance_door_locked;
	if(entrance_door_locked){
		LOG_INFO("[MANUAL]Entrance door closed\n");
	} else LOG_INFO("[MANUAL]Entrance door opened\n");

	if(light_color == RED) {
		light_color = GREEN;
		if(!manual){
			LOG_INFO("[MANUAL]Light color green\n");
		}
	}
	else if(light_color == GREEN){
		light_color = RED;
		if(!manual){
			LOG_INFO("[MANUAL]Light color red\n");
		}
	}
	manual = !manual;
}

int random_in_range(int a, int b) {
    int v = random_rand() % (b-a);
    return v + a;
}
static int factor = -1;
static void simulate_of_entrance(){
	if(!entrance_door_locked && number_of_people == 0 && factor == -1){
		factor = 1;
	}
	else if(entrance_door_locked){
		//entrance door closed or number of people greater than 30
		factor = -1;
	}

	number_of_people = number_of_people + factor;

	if(number_of_people < 0) {
		number_of_people = 0;
	}

	LOG_INFO("New number of people: %d\n", number_of_people);				
}


PROCESS_THREAD(access_control_process, ev, data) {

	PROCESS_BEGIN();

	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &access_control_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);

	static char sensorType[20] = "presenceSensor";
			
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
				// Subscribe to a topic
				strcpy(sub_topic,"access_regulator");
				status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
				if(status == MQTT_STATUS_OUT_QUEUE_FULL){
					LOG_ERR("Tried to subscribe but command queue was full!\n");
					PROCESS_EXIT();
				}
				state = STATE_SUBSCRIBED;
			}	  
			if(state == STATE_SUBSCRIBED) {
				sprintf(pub_topic, "%s", "number_of_people");

				if(ev == button_hal_press_event){
					button_hal_button_t* btn = (button_hal_button_t*)data;
                    if (btn->unique_id == BOARD_BUTTON_HAL_INDEX_KEY_LEFT) {
                    	manual_handler();
                    }
				}

				simulate_of_entrance();

                snprintf(app_buffer, APP_BUFFER_SIZE, 
                            "{\"node\": %d, \"value\": %d, \"manual\": %d, \"sensorType\": \"%s\"}", 
                            (unsigned int) node_id, number_of_people, (int)manual, sensorType);

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
static struct etimer access_led_timer;
static struct etimer led_on_timer;

PROCESS_THREAD(blinking_led, ev, data)
{
	PROCESS_BEGIN();

	etimer_set(&registration_led_timer, 1*CLOCK_SECOND);

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

	etimer_set(&access_led_timer, 7*CLOCK_SECOND);
	etimer_set(&led_on_timer, 1*CLOCK_SECOND);

	while(1){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&access_led_timer)){
				if(light_color == RED){
					leds_on(LEDS_RED);
				}
				else if(light_color == YELLOW){
					leds_on(LEDS_RED);
					leds_on(LEDS_GREEN);
				}
				else if(light_color == GREEN){
					leds_on(LEDS_GREEN);
				}
				etimer_restart(&access_led_timer);
				etimer_restart(&led_on_timer);
			}
			if(etimer_expired(&led_on_timer)){
				leds_off(LEDS_GREEN);
				leds_off(LEDS_RED);
			}
		}
	}
	PROCESS_END();
}
