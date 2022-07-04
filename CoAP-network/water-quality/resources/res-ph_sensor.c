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
 
float random_float(float a, float b) {
    float random = ((float) rand()) / (float) RAND_MAX;
    float diff = b - a;
    float r = random * diff;
 
    float res = a + r;
    return res;
}

//CONTIKI DOES NOT SUPPORT THE FLOAT FORMAT
// Return digits before point
unsigned short digitsBefore(float f){
    return((unsigned short)f);
}
 
// Return digits after point
unsigned short digitsAfter(float f){
    return(10*(f-digitsBefore(f)));
}
 
static bool simulate_ph_values () { 
	bool updated = false;
	float old_ph = ph_level;
 
    srand(time(NULL));
    float value = 0;
 
	if(buffer_release) {
        // if the pH is in the right interval and the buffer regulator is on (may be caused by manual activation)
        // the pH remains in the right range
        if (old_ph >= 7.2 && old_ph <= 7.8) {
            ph_level = random_float(7.2, 7.8);
        } else {
            value = random_float(0.1, 0.5);
            ph_level = old_ph + value;
        }
 
	} else {
        value = random_float(0.1, 0.5);
        ph_level = old_ph - value;
        LOG_INFO("value: %u.%u\n", digitsBefore(value), digitsAfter(value));
    }
 
	if(old_ph != ph_level)
		updated = true;
 
	return updated;
}
 
static void ph_event_handler(void) {
	if (simulate_ph_values()) { // if the value is changed
		LOG_INFO("pH level: %u.%u \n", digitsBefore(ph_level), digitsAfter(ph_level));
		// Notify all the observers
    	coap_notify_observers(&res_ph_sensor);
	}
}
 
 
static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	  	char message[96];
      	int length = 96;
      	snprintf(message, length, "{\"node\": %d, \"value\": %u.%u, \"manual\": %d, \"sensorType\": \"%s\"}", (unsigned int) node_id, digitsBefore(ph_level), digitsAfter(ph_level), (int)manual, sensorType);
 
      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);
 
        LOG_INFO("message: %s\n", message);
 
      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}
