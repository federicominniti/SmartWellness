package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import com.google.gson.Gson;


/**
 * Mother class for all MQTT nodes stubs. It provides a default constructor for all devices, as well as
 * basic setters and getters for the actuator and the sensor
 * @param <SensedDataType> the data type for sensed values
 * @param <ActuatorStatusType> the status type for the actuator
 */
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

    public SensedDataType getSensedData() {
        return actualValue;
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