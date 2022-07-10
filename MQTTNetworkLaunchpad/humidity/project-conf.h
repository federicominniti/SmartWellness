#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

/* Enable TCP */
#define UIP_CONF_TCP 1
/*#undef IEEE802154_CONF_PANID
#define IEEE802154_CONF_PANID 0x0023*/

#define LOG_LEVEL_APP LOG_LEVEL_DBG

#define CCXXWARE_CONF_ROM_BOOTLOADER_ENABLE 1

#define LOG_LEVEL_APP LOG_LEVEL_DBG
/* Change to match your configuration */
#define IEEE802154_CONF_PANID            0xABCD
#define IEEE802154_CONF_DEFAULT_CHANNEL      25
#define RF_BLE_CONF_ENABLED                   1
#endif /* PROJECT_CONF_H_ */