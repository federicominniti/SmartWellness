#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

#include "sys/log.h"

/* Log configuration */
#define LOG_MODULE "light"
#define LOG_LEVEL LOG_LEVEL_APP

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_light_system,
         "title=\"Light System\";rt=\"Control\"",
         NULL,
         NULL,
         light_put_handler,
         NULL);

//sets the light OFF(0), LOW(1), ON(2)
int light_level = 0;

//tracks if the light is in manual mode
bool manual = false;

//sets the light actuator based on a CoAP request from the collector
static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char status[4];
	memset(status, 0, 3);

    bool response_status = true;

	len = coap_get_post_variable(request, "level", &text);
	if(len != 0) {
        light_level = atoi(text);
        if(light_level > 0)
            LOG_INFO("Light ON\n");
        else{
            LOG_INFO("Light OFF\n");
        }
        LOG_INFO("Level %d\n", light_level);
	} else {
		response_status = false;
	}
	
	if(!response_status) {
    	coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}

//enter or exit the manual mode and set the status of the light accordingly
void manual_handler(){
    manual = !manual;
    if(light_level == 0){
		light_level = 2;
        LOG_INFO("Light ON\n");
	}else{
		light_level = 0;
        LOG_INFO("Light OFF\n");
	}
}

