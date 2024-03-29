#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "os/dev/leds.h"

#include "sys/log.h"

#define LOG_MODULE "light_regulation"
#define LOG_LEVEL LOG_LEVEL_APP

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_light_system,
         "title=\"Light System\";rt=\"Control\"",
         NULL,
         NULL,
         light_put_handler,
         NULL);

int light_level = 0;
bool manual = false;

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char status[4];
	memset(status, 0, 3);

    bool response_status = true;

	len = coap_get_post_variable(request, "level", &text);
	if(len != 0) {
        light_level = atoi(text);
	} else {
		response_status = false;
	}
	
	if(!response_status) {
    	coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}

void manual_handler(){
    manual = !manual;
    if(light_level == 0){
		light_level = 2;
	}else{
		light_level = 0;
	}
}

