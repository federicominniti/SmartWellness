#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

#define UIP_CONF_TCP 1

#define LOG_LEVEL_APP LOG_LEVEL_DBG

#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif
//d
#endif /* PROJECT_CONF_H_ */