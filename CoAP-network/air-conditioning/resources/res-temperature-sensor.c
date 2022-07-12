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

//the temperature sensed by the sensor
static int temperature = 18;

/*
    when the AC is OFF, 33% of chance the temperature will rise of 1C
    when the AC is ON and the temperature is higher than the AC working temperature, 33% of chance of decrease of 1C
    when the AC is ON and the temperature==AC working temperature, the temperature does not change
    when the AC is ON and the temperature<AC working temperature, 50% of chance of increase of 1C
*/
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
	LOG_INFO("temperature : %d \n", temperature);
    coap_notify_observers(&res_temperature_sensor);
}

static void temperature_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	char message[96];
      	int length = 96;
      	snprintf(message, length, "{\"node\": %d, \"value\": %d, \"manual\": %d, \"sensorType\": \"tempSensor\" }", (unsigned int) node_id, (int)temperature, (int)manual);

      	size_t len = strlen(message);
      	memcpy(buffer, (const void *) message, len);

        LOG_INFO("message: %s\n", message);

      	coap_set_header_content_format(response, TEXT_PLAIN);
      	coap_set_header_etag(response, (uint8_t *)&len, 1);
      	coap_set_payload(response, buffer, len);
}
