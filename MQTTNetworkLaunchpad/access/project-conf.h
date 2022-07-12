#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

#define UIP_CONF_TCP 1

#define LOG_LEVEL_APP LOG_LEVEL_DBG

#define CCXXWARE_CONF_ROM_BOOTLOADER_ENABLE 1

#define LOG_LEVEL_APP LOG_LEVEL_DBG
#define IEEE802154_CONF_PANID            0xABCD
#define IEEE802154_CONF_DEFAULT_CHANNEL      25

#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif
#endif /* PROJECT_CONF_H_ */