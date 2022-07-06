package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import com.google.gson.Gson;

public class MqttNode<SensedDataType, ActuatorStatusType> {
    public final String SENSOR_TOPIC;
    public final String ACTUATOR_TOPIC;

    protected ActuatorStatusType actuatorOn;
    protected boolean manual;
    protected SensedDataType actualValue;

    protected Logger logger;
    protected Gson parser;

    protected MqttNode(ActuatorStatusType status, SensedDataType value, String sensorTopic, String actuatorTopic) {
        actuatorOn = status;
        actualValue = value;
        manual = false;
        SENSOR_TOPIC = sensorTopic;
        ACTUATOR_TOPIC = actuatorTopic;
        logger = Logger.getInstance();
        parser = new Gson();
    }

    public ActuatorStatusType getActuatorStatus() {
        return actuatorOn;
    }

}