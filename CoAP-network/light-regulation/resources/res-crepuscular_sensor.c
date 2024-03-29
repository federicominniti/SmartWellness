#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <stdint.h>
#include <math.h>
#include "contiki.h"
#include "coap-engine.h"
#include "random.h"
#include "os/dev/leds.h"
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

//lux sensed by the crepuscular sensor
static int lux = 1600;
static char sensorType[20] = "crepuscularSensor";
 
static void simulate_lux_values () {
	//In a real environment the luxe range is 0 - 25000

	//we pick a random value between 0 and 2000 so we have
	// -> 500/2500 = 20% probability that the light goes OFF(0)
	// -> 350/2500 = 14% probability that the light goes ON(2)
	// -> 1150 / 2500 = 46% probability that the light goes LOW(1)
    lux = random_rand() % 2500;
}

static void lux_event_handler(void) {
    simulate_lux_values();
	LOG_INFO("LUX: %d \n", lux);
    coap_notify_observers(&res_crepuscular_sensor);
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