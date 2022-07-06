CONTIKI_PROJECT = light-regulation
all: $(CONTIKI_PROJECT)

PLATFORMS_ONLY=cc26x0-cc13x0
MODULES_REL += ./resources

CONTIKI = ../../../..

include $(CONTIKI)/Makefile.dir-variables

MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

include $(CONTIKI)/Makefile.include
include $(CONTIKI)/Makefile.identify-target
