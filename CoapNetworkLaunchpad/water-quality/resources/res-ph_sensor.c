#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include "random.h"
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "sys/node-id.h"
 
#include "global_variables.h"
 
#include "sys/log.h"
 
#define LOG_MODULE "ph-sensor"
#define LOG_LEVEL LOG_LEVEL_APP
 
static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void ph_event_handler(void);
 
EVENT_RESOURCE(res_ph_sensor,
         "title=\"PH sensor\";obs",
         ph_get_handler,
         NULL,
         NULL,
         NULL,
	 ph_event_handler);
 
static float ph_level = 7.0;
static char sensorType[20] = "phSensor";

int random_in_range(int a, int b) {
    int v = random_rand() % (b-a);
    return v + a;
}

unsigned short digitsBefore(float f){
    return((unsigned short)f);
}

unsigned short digitsAfter(float f){
    return(10*(f-digitsBefore(f)));
}
 
static void simulate_ph_values () {
    float value = 0;
 
	if(buffer_release) {
        if (ph_level >= 7.4 && ph_level <= 7.8) {
	        ph_level = 7.0 + (float)random_in_range(4, 8) / 10.0;
        } else {
            ph_level = ph_level + random_in_range(1,3) / 10.0;
        }
 
	} else {
        value = random_in_range(1,2) / 10.0;
        ph_level = ph_level - value;
    }
}
 
static void ph_event_handler(void) {
	simulate_ph_values()
    coap_notify_observers(&res_ph_sensor);
}
 
 
static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	  	char message[96];
      	int length = 96;
      	snprintf(message, length, "{\"node\": %d, \"value\": %u.%u, \"manual\": %d, \"sensorType\": \"%s\"}", (unsigned int) node_id, digitsBefore(ph_level), digitsAfter(ph_level), (int)manual, sensorType);
 
      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);

      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}
