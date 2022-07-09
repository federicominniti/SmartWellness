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
#include "dev/leds.h"
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

PROCESS_NAME(chlorine_control_process);
AUTOSTART_PROCESSES(&chlorine_control_process);

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

PROCESS(chlorine_control_process, "Chlorine control process");

static bool increase_chlorine = false;
#define MIN_CHLORINE 0
#define MAX_CHLORINE 20
static float chlorine_level = (float)1.5;
static float variation = 0;
static bool manual = false;

static mqtt_status_t status;
static char broker_address[CONFIG_IP_ADDR_STR_LEN];

//TODO GESTIONE PRESSIONE BOTTONE
//TODO GESTIONE LED

static float random_float(float a, float b) {
    float random = ((float) rand()) / (float) RAND_MAX;
    float diff = b - a;
    float r = random * diff;
 
    float res = a + r;
    return res;
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


// Incoming message handling
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	LOG_INFO("Message received: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, "chlorine_regulator") == 0) {
		LOG_INFO("Received Actuator command\n");
		if(strcmp((const char*) chunk, "ON") == 0) {
			increase_chlorine = true;
            LOG_INFO("Chlorine regulator switched ON\n");
		}
		else if(strcmp((const char*) chunk, "OFF") == 0) {
			increase_chlorine = false;
            LOG_INFO("Chlorine regulator switched ON\n");
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

static void manual_handler(){
    manual = !manual;
	increase_chlorine = !increase_chlorine;
}

static void simulate_chlorine_level(){
	// simulate sensed values		
	//TODO GESTIONE MANUAL CHE RIMANGA NEL RANGE SE c'è ARRIVATO	
	float old_chlorine = chlorine_level;
	if(increase_chlorine) {
		// if the pH is in the right interval and the buffer regulator is on (may be caused by manual activation)
		// the pH remains in the right range
		if (old_chlorine >= 1.0 && old_chlorine <= 3.0) {
			chlorine_level = (float)random_in_range(1, 3);
		} else {
			variation = (float)random_in_range(2, 8) * 0.1;
			chlorine_level = old_chlorine + variation;
		}
			
	} else {
		variation = (float)random_in_range(2, 8) * 0.1;
		chlorine_level = old_chlorine - variation;
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
				// Subscribe to a topic
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

				simulate_chlorine_level();

                snprintf(app_buffer, APP_BUFFER_SIZE, 
                            "{\"node\": %d, \"value\": %u.%u, \"manual\": %d, \"sensorType\": \"%s\"}", 
                            (unsigned int) node_id, digitsBefore(chlorine_level), digitsAfter(chlorine_level), 
                            (int)manual, sensorType);

				if(ev == button_hal_press_event){
					manual_handler();
				}

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