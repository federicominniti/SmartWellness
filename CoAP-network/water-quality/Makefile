CONTIKI_PROJECT = water_quality

all: $(CONTIKI_PROJECT)

MODULES_REL += ./resources

CONTIKI=../..

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

include $(CONTIKI)/Makefile.include
