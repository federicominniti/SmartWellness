package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes;

import com.google.gson.Gson;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapObserveRelation;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;

/**
 * Mother class for all Coap nodes stubs. It provides a default constructor for all devices, as well as
 * default un/registration for Coap devices and basic setters and getters for the actuator and the sensor
 * @param <SensedDataType> the data type for sensed values, must be thread-safe
 * @param <ActuatorStatusType> the status type for the actuator, must be thread-safe
 */
public abstract class CoapNode<SensedDataType, ActuatorStatusType> {
    protected CoapClient sensor;
    protected CoapClient actuator;
    protected CoapObserveRelation observeRelation;
    protected Logger logger;

    protected SensedDataType sensedData;

    protected ActuatorStatusType actuatorStatus;
    protected AtomicBoolean manual;

    protected Gson parser;

    protected CoapNode(SensedDataType startingValue, ActuatorStatusType status) {
        manual = new AtomicBoolean(false);
        sensedData = startingValue;
        actuatorStatus = status;
        logger = Logger.getInstance();
        parser = new Gson();
    }

    public void registerNode(String ip, String coapService, String sensorResName, String actuatorResName) {
        System.out.println("[REGISTRATION] " + coapService + " " + ip + " has been registered");
        sensor = new CoapClient("coap://[" + ip + "]/" + coapService + "/" + sensorResName);
        actuator = new CoapClient("coap://[" + ip + "]/" + coapService + "/" + actuatorResName);
    }

    public void unregisterNode(String ip) {
        if (sensor.getURI().equals(ip)) {
            observeRelation.proactiveCancel();
        }
    }

    public ActuatorStatusType getActuatorStatus() {
        return actuatorStatus;
    }

    public SensedDataType getSensedData() {
        return sensedData;
    }

    public AtomicBoolean getManual() {
        return manual;
    }
}