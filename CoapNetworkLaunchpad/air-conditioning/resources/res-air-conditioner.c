#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"

#include "sys/log.h"

#define LOG_MODULE "ac-system"
#define LOG_LEVEL LOG_LEVEL_APP


static bool change_ac_status(int len, const char* text);
static bool change_ac_temp(int len, const char* text);
static void ac_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
//c
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
		} else if(strncmp(status, "OFF", len) == 0) {
			ac_on = false;
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
	    response_status = change_ac_status(len, text);
	} else {
	    text = NULL;
	    len = coap_get_post_variable(request, "ac_temp", &text);
	    response_status = change_ac_temp(len, text);
	}

	if (!response_status) {
	    coap_set_status_code(response, BAD_REQUEST_4_00);
	}
}

void manual_handler() {
    manual = !manual;
    ac_on = !ac_on;

    if (ac_on) {
        LOG_INFO("AC is ON \n");
    } else {
        LOG_INFO("AC is OFF\n");
    }
}
