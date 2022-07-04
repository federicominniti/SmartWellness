#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

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

bool ac_on = false;
int ac_temperature = 18;
bool manual = false;

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
			//leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("AC System OFF\n");
		} else {
			return false;
		}
	} else {
		return false;
	}

	return true;
}

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

