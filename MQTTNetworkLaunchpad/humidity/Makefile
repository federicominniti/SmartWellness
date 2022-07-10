CONTIKI_PROJECT = humidifier
all: $(CONTIKI_PROJECT)
PLATFORMS_ONLY = cc26x0-cc13x0

CONTIKI = ../../../..

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

-include $(CONTIKI)/Makefile.identify-target

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include