package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import com.google.gson.Gson;

public abstract class MqttNode<SensedDataType, ActuatorStatusType> {
    protected final String SENSOR_TOPIC;
    protected final String ACTUATOR_TOPIC;

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

    public String getSENSOR_TOPIC() {
        return SENSOR_TOPIC;
    }

    public String getACTUATOR_TOPIC() {
        return ACTUATOR_TOPIC;
    }

    public ActuatorStatusType getActuatorOn() {
        return actuatorOn;
    }

    public void setActuatorOn(ActuatorStatusType actuatorOn) {
        this.actuatorOn = actuatorOn;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public SensedDataType getActualValue() {
        return actualValue;
    }

    public void setActualValue(SensedDataType actualValue) {
        this.actualValue = actualValue;
    }

}