package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import com.google.gson.Gson;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.utils.AtomicFloat;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;

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