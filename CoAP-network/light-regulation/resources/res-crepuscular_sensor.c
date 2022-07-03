#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <stdint.h>
#include <math.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "sys/node-id.h"
 
#include "global_variables.h"
 
#include "sys/log.h"
 
/* Log configuration */
#define LOG_MODULE "crepuscular-sensor"
#define LOG_LEVEL LOG_LEVEL_APP
 
static void lux_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void lux_event_handler(void);
 
EVENT_RESOURCE(res_crepuscular_sensor,
         "title=\"LUX sensor\";obs",
         lux_get_handler,
         NULL,
         NULL,
         NULL,
	     lux_event_handler);
 
static int lux = 1600;
static char sensorType[20] = "crepuscularSensor";
 
static bool simulate_lux_values () { 
	bool updated = false;
	int old_lux = lux;
 
    srand(time(NULL));
 
	lux = rand()%25000;
 
	if(old_lux != lux)
		updated = true;
 
	return updated;
}
 
static void lux_event_handler(void) {
	if (simulate_lux_values()) { // if the value is changed
		LOG_INFO("LUX: %d \n", lux);
		// Notify all the observers
    	coap_notify_observers(&res_crepuscular_sensor);
	}
}
 
 
static void lux_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	  	char message[96];
      	int length = 96;
      	snprintf(message, length, "{\"node\": %d, \"value\": %d, \"manual\": %d, \"sensorType\": \"%s\"}", (unsigned int) node_id, (unsigned int)lux, (int)manual, sensorType);
 
      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);
 
        LOG_INFO("message: %s\n", message);
 
      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}