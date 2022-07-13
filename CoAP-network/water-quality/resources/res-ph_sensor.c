#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <stdint.h>
#include <math.h>
#include "contiki.h"
#include "coap-engine.h"
#include "os/dev/leds.h"
#include "sys/node-id.h"
#include "random.h"
 
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

//the ph level sensed by the sensor
static float ph_level = 7.0;

//the sensor type
static char sensorType[20] = "phSensor";
 
//calculate a random float value between a specified range
int random_in_range(int a, int b) {
    int v = random_rand() % (b-a);
    return v + a;
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
 
/*
    when the buffer regulator is OFF, the ph level decrease
    when the buffer regulator is ON, if the ph level is in the correct rage (7.4 - 7.8) it remains in that range (may be caused by manual activation)
    when the buffer regulator is ON, if the ph level is not in the correct rage it increments its value
*/
static void simulate_ph_values () {
    float value = 0;
	if(buffer_release) {
        if (ph_level >= 7.4 && ph_level <= 7.9) {
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
	simulate_ph_values();
	LOG_INFO("pH level: %u.%u \n", digitsBefore(ph_level), digitsAfter(ph_level));
	// Notify all the observers
    coap_notify_observers(&res_ph_sensor);
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
