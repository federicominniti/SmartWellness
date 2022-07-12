#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "sys/node-id.h"
#include "random.h"

#include "global_variables.h"

#include "sys/log.h"

#define LOG_MODULE "temperature-sensor"
#define LOG_LEVEL LOG_LEVEL_APP

static void temperature_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void temperature_event_handler(void);

EVENT_RESOURCE(res_temperature_sensor,
         "title=\"Temperature sensor\";obs",
         temperature_get_handler,
         NULL,
         NULL,
         NULL,
	 temperature_event_handler);

static int temperature = 18;

static void simulate_temperature_values () {
    int variation = 0;

	if(ac_on) {
	    if (temperature < ac_temperature) {
	        variation = random_rand() % 3;
	        if (variation != 1) {
	            variation = 0;
	        } else
	            variation = -1;
	    }
	    else if (temperature == ac_temperature) {
	        variation = 0;
	    } else {
	        variation = random_rand() % 3;
	        if (variation != 1)
	            variation = 0;

	    }

	    temperature = temperature - variation;

	} else {
        variation = random_rand() % 2;
        if (variation != 1)
            variation = 0;

        temperature = temperature + variation;
    }

}

static void temperature_event_handler(void) {
	simulate_temperature_values();
    coap_notify_observers(&res_temperature_sensor);
}

static void temperature_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	char message[96];
      	int length = 96;
      	snprintf(message, length, "{\"node\": %d, \"value\": %d, \"manual\": %d, \"sensorType\": \"tempSensor\" }", (unsigned int) node_id, (int)temperature, (int)manual);

      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);

      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}
