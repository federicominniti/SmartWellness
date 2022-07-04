#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

#include "sys/log.h"

/* Log configuration */
#define LOG_MODULE "buffer-regulator"
#define LOG_LEVEL LOG_LEVEL_APP

static void buffer_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_buffer_regulator,
         "title=\"Pool Buffer Regulator\";rt=\"Control\"",
         NULL,
         NULL,
         buffer_put_handler,
         NULL);

bool buffer_release = false;
bool manual = false;

static void buffer_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char status[4];
	memset(status, 0, 3);

    bool response_status = true;

	len = coap_get_post_variable(request, "status", &text);
	if(len > 0 && len < 4) {
		memcpy(status, text, len);
		if(strncmp(status, "ON", len) == 0) {
			buffer_release = true;
            //TO-DO BLINKING
			//leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Buffer regulator ON\n");
		} else if(strncmp(status, "OFF", len) == 0) {
			buffer_release = false;
			//leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("Buffer regulator OFF\n");
		} else {
			response_status = false;
		}
	} else {
		response_status = false;
	}
	
	if(!response_status) {
    		coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}
