#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "os/dev/leds.h"

#include "sys/log.h"

/* Log configuration */
#define LOG_MODULE "ac-system"
#define LOG_LEVEL LOG_LEVEL_APP

static bool change_ac_status(int len, const char* text);
static bool change_ac_temp(int len, const char* text);
static void ac_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_ac_system,
         "title=\"Gym AC System\";rt=\"Control\"",
         NULL,
         NULL,
         ac_put_handler,
         NULL);

//sets the AC ON/OFF
bool ac_on = false;

//sets the working temperature for the AC
int ac_temperature = 18;

//tracks if the AC is in manual mode
bool manual = false;


//change the AC status based on a CoAP request from the collector
static bool change_ac_status(int len, const char* text) {
	char status[4];
	memset(status, 0, 3);

	if(len > 0 && len < 4) {
    		memcpy(status, text, len);
		if(strncmp(status, "ON", len) == 0) {
			ac_on = true;
			LOG_INFO("AC system ON\n");
		} else if(strncmp(status, "OFF", len) == 0) {
			ac_on = false;
			LOG_INFO("AC System OFF\n");
		} else {
			return false;
		}
    	} else {
		return false;
    	}

    	return true;
}

//change the AC working temperature based on a CoAP request from the collector
static bool change_ac_temp(int len, const char* text) {
    	char ac_temp[4];
    	memset(ac_temp, 0, 3);

    	if(len > 0 && len < 4) {
		memcpy(ac_temp, text, len);
		ac_temperature = atoi(ac_temp);

		LOG_INFO("Changed AC temperature to %d \n", ac_temperature);
    	} else {
		return false;
    	}

    	return true;
}


//checks if the CoAP request from the collector wants to set the AC working temperature
//or change the status of the AC
static void ac_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;

    	bool response_status = true;

	len = coap_get_post_variable(request, "status", &text);
	if (len > 0) {
		LOG_INFO("status len: %d \n", len);
	    	response_status = change_ac_status(len, text);
	} else {
	    	text = NULL;
	    	len = coap_get_post_variable(request, "ac_temp", &text);
	   	LOG_INFO("ac temp len: %d \n", len);

	    	response_status = change_ac_temp(len, text);
	}

	if (!response_status) {
	    	coap_set_status_code(response, BAD_REQUEST_4_00);
	}
}

//enter or exit the manual mode and set the status of the AC accordingly
void manual_handler() {
    	manual = !manual;
    	ac_on = !ac_on;

    	if (ac_on) {
        	LOG_INFO("AC is ON \n");
    	} else {
        	LOG_INFO("AC is OFF\n");
    	}
}

