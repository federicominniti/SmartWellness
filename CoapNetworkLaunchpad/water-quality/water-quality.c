#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "coap-engine.h"
#include "sys/etimer.h"
#include "sys/clock.h"
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

#define SERVER_EP "coap://[fd00::1]:5683"

#define REGISTRATION_INTERVAL 2

#define RESOURCE_TYPE "water_quality"

#define LOG_MODULE "water-quality"
#define LOG_LEVEL LOG_LEVEL_APP

#define SIMULATION_INTERVAL 10

#define CONNECTION_TEST_INTERVAL 2

extern coap_resource_t res_buffer_regulator;
extern coap_resource_t res_ph_sensor;

char *service_url = "/registration";

static bool registered = false;

static struct etimer simulation_timer;

static struct etimer connectivity_timer;

static struct etimer registration_timer;

static struct etimer registration_led_timer;
static struct etimer buffer_led_timer;
static struct etimer led_on_timer;

PROCESS(water_quality_server, "Water Quality Server");
PROCESS(blinking_led, "Led blinking process");
AUTOSTART_PROCESSES(&water_quality_server, &blinking_led);

static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		return true;
  	}
  	return false;
}

void client_chunk_handler(coap_message_t *response) {
	const uint8_t *chunk;
	if(response == NULL) {
		etimer_set(&registration_timer, CLOCK_SECOND* REGISTRATION_INTERVAL);
		return;
	}

	int len = coap_get_payload(response, &chunk);

	if(strncmp((char*)chunk, "Success", len) == 0){
		registered = true;
	} else
		etimer_set(&registration_timer, CLOCK_SECOND* REGISTRATION_INTERVAL);
}

PROCESS_THREAD(water_quality_server, ev, data){
	PROCESS_BEGIN();

	static coap_endpoint_t server_ep;

    static coap_message_t request[1];

	coap_activate_resource(&res_buffer_regulator, "water_quality/buffer");
	coap_activate_resource(&res_ph_sensor, "water_quality/ph");

	etimer_set(&connectivity_timer, CLOCK_SECOND * CONNECTION_TEST_INTERVAL);
	PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	while(!is_connected()) {
		etimer_reset(&connectivity_timer);
		PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	}

	while(!registered) {
    	coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
    	coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    	coap_set_header_uri_path(request, service_url);
    	coap_set_payload(request, (uint8_t *)RESOURCE_TYPE, sizeof(RESOURCE_TYPE) - 1);

    	COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);

    	PROCESS_WAIT_UNTIL(etimer_expired(&registration_timer));
    }

	etimer_set(&simulation_timer, CLOCK_SECOND * SIMULATION_INTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT();
		if((ev == PROCESS_EVENT_TIMER && data == &simulation_timer) || ev == button_hal_press_event) {
			if(ev == button_hal_press_event){
				button_hal_button_t* btn = (button_hal_button_t*)data;
				if (btn->unique_id == BOARD_BUTTON_HAL_INDEX_KEY_LEFT) {
					manual_handler();
				}
			}
			res_ph_sensor.trigger();	
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

    //the red led is blinking until the the device is still connecting to the border router or the collector
	while(!is_connected() || !registered){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&registration_led_timer)){
				leds_toggle(LEDS_RED);
				etimer_restart(&registration_led_timer);
			}
		}
	}

	leds_off(LEDS_RED);

	etimer_set(&buffer_led_timer, 7*CLOCK_SECOND);
	etimer_set(&led_on_timer, 1*CLOCK_SECOND);

    /*if the buffer regulator is ON, both the leds are blinking
      if the buffer regulator is OFF, only the green led is blinking
    */
	while(1){
		PROCESS_YIELD();
		if (ev == PROCESS_EVENT_TIMER){
			if(etimer_expired(&buffer_led_timer)){
				if(buffer_release){
					leds_on(LEDS_RED);
				}
				leds_on(LEDS_GREEN);
				etimer_restart(&buffer_led_timer);
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
