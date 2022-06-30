#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include<time.h>
#include <stdint.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "sys/node-id.h"

#include "global_variables.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "pH-sensor"
#define LOG_LEVEL LOG_LEVEL_APP

static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void ph_event_handler(void);

EVENT_RESOURCE(res_ph_sensor,
         "title=\"PH sensor\"; obs",
         ph_get_handler,
         NULL,
         NULL,
         NULL,
	 ph_event_handler);

static float ph_level = 7.0;

float random_float(float a, float b) {
    float random = ((float) rand()) / (float) RAND_MAX;
    float diff = b - a;
    float r = random * diff;

    float res = a + r;
    float rounded_down = floorf(res * 10) / 10;  
    return rounded_down;
}

static bool simulate_ph_values () { 
	bool updated = false;
	float old_ph = ph_level;

    srand(time(NULL));
    int value = 0;

	if(pump_on) {
        // if the pH is in the right interval and the pump is on (may be caused by manual activation)
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
    }

	if(old_ph != ph_level)
		updated = true;

	return updated;
}

static void ph_event_handler(void) {
	if (simulate_ph_values()) { // if the value is changed
		LOG_INFO("pH level: %u \n", ph_level);
		// Notify all the observers
    	coap_notify_observers(&res_ph_sensor);
	}
}

static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	  	char message[64];
      	int length = 64;
      	snprintf(message, length, "{\"node\": %d, \"pH level\": %d}", (unsigned int) node_id, (float) ph_level);

      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);

      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}