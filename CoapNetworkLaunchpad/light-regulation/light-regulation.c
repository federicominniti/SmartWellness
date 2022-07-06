#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "coap-engine.h"
#include "sys/etimer.h"
#include "os/dev/leds.h"
#include "coap-blocking-api.h"
#include "os/dev/button-hal.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#include "global_variables.h"

#include "sys/log.h"

//Observing server End-Point address
#define SERVER_EP "coap://[fd00::1]:5683"

//Interval for registration retries with the observing server
#define REGISTRATION_INTERVAL 2

//Type of device
#define RESOURCE_TYPE "light_regulation"

// Log configuration
#define LOG_MODULE "light-regulation"
#define LOG_LEVEL LOG_LEVEL_APP

//TO DO 
//#define SIMULATION_INTERVAL 300
//Simulation interval between sensor measurements
#define SIMULATION_INTERVAL 6

//Interval for connection retries with the border router
#define CONNECTION_TEST_INTERVAL 2

//Coap Resources for the pump (actuator) and the PH-sensor (sensor)
extern coap_resource_t res_light_system;
extern coap_resource_t res_crepuscular_sensor;

//URL for registration with the observing server
char *service_url = "/registration";

//Registration status
static bool registered = false;

//Timer for simulations of sensor measurements
static struct etimer simulation_timer;

//Timer for connection retries with the border router
static struct etimer connectivity_timer;

//Timer for registration retries with the observing server
static struct etimer registration_timer;

//Timers required for leds blinking
static struct etimer registration_led_timer;
static struct etimer lux_led_timer;
static struct etimer led_on_timer;

//Declare the two protothreads: one for the sensing subsystem,
//the other for handling leds blinking
PROCESS(light_regulation_server, "Light Regulation Server");
PROCESS(blinking_led, "Led blinking process");
AUTOSTART_PROCESSES(&light_regulation_server, &blinking_led);


// Test the connectivity with the border router
static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		LOG_INFO("The Border Router is reachable\n");
		return true;
  	} else {
		LOG_INFO("Waiting for connection with the Border Router\n");
	}
	return false;
}

// Handler for connection requests sent by the water quality server
// In case the reply is 'Success' the water quality server is connected to the collector
void client_chunk_handler(coap_message_t *response) {
	const uint8_t *chunk;
	if(response == NULL) {
		LOG_INFO("Request timed out\n");
		etimer_set(&registration_timer, CLOCK_SECOND* REGISTRATION_INTERVAL);
		return;
	}

	int len = coap_get_payload(response, &chunk);

	if(strncmp((char*)chunk, "Success", len) == 0){
		registered = true;
	} else
		etimer_set(&registration_timer, CLOCK_SECOND* REGISTRATION_INTERVAL);
}

PROCESS_THREAD(light_regulation_server, ev, data){
	PROCESS_BEGIN();

	static coap_endpoint_t server_ep;

    // This way the packet can be treated as pointer as usual
    static coap_message_t request[1];

	LOG_INFO("Starting light regulation CoAP server\n");
	coap_activate_resource(&res_light_system, "light_regulation/light"); 
	coap_activate_resource(&res_crepuscular_sensor, "light_regulation/lux");

	// try to connect to the border router
	etimer_set(&connectivity_timer, CLOCK_SECOND * CONNECTION_TEST_INTERVAL);
	PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	while(!is_connected()) {
		etimer_reset(&connectivity_timer);
		PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	}

	//try to connect to the collector
	while(!registered) {
    	LOG_INFO("Sending registration message\n");
    	coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
    	// Prepare the message
    	coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    	coap_set_header_uri_path(request, service_url);
    	coap_set_payload(request, (uint8_t *)RESOURCE_TYPE, sizeof(RESOURCE_TYPE) - 1);

    	COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);

    	PROCESS_WAIT_UNTIL(etimer_expired(&registration_timer));
    }

	//periodic simulation of sensor measurements
	etimer_set(&simulation_timer, CLOCK_SECOND * SIMULATION_INTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT();
		if((ev == PROCESS_EVENT_TIMER && data == &simulation_timer) || ev == button_hal_press_event) {
			//handle manual pump activation with the button
			if(ev == button_hal_press_event){
				manual_handler();
			}
			res_crepuscular_sensor.trigger();	
			etimer_set(&simulation_timer, CLOCK_SECOND * SIMULATION_INTERVAL);
		}
	}

	PROCESS_END();
}

PROCESS_THREAD(blinking_led, ev, data)
{
	PROCESS_BEGIN();

	etimer_set(&registration_led_timer, 1*CLOCK_SECOND);

	leds_on(LEDS_RED);

	while(!is_connected() || !registered){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&registration_led_timer)){
				leds_toggle(LEDS_RED);
				etimer_restart(&registration_led_timer);
			}
		}
	}

	leds_off(LEDS_RED)

	etimer_set(&lux_led_timer, 7*CLOCK_SECOND);
	etimer_set(&led_on_timer, 1*CLOCK_SECOND);

	while(1){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&lux_led_timer)){
				if(light_level == 0){
					leds_on(LEDS_GREEN);
				} else if(light_level == 1){
                    leds_on(LEDS_RED);
                } else if(light_level == 2){
                    leds_on(LEDS_GREEN);
                    leds_on(LEDS_RED);
                }

				etimer_restart(&lux_led_timer);
				etimer_restart(&led_on_timer);
			}

			if(etimer_expired(&led_on_timer)){
                led_off(LEDS_RED);
                leds_off(LEDS_GREEN);
			}
		}
	}
	PROCESS_END();
}
