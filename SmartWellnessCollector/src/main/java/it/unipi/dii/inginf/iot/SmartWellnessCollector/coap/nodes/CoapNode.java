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
    protected List<DataSample> lastSamples;

    protected SensedDataType UPPER_BOUND;
    protected SensedDataType LOWER_BOUND;
    protected SensedDataType NORMAL_LEVEL;

    protected ActuatorStatusType actuatorStatus;
    protected AtomicBoolean manual;

    protected Gson parser;

    protected CoapNode(SensedDataType upperBound,
                   SensedDataType lowerBound,
                   SensedDataType normalLevel,
                   ActuatorStatusType status) {

        UPPER_BOUND = upperBound;
        LOWER_BOUND = lowerBound;
        NORMAL_LEVEL = normalLevel;
        actuatorStatus = status;
        manual = new AtomicBoolean(false);
        lastSamples = new ArrayList();
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

    public SensedDataType getUPPER_BOUND() {
        return UPPER_BOUND;
    }

    public void setUPPER_BOUND(SensedDataType UPPER_BOUND) {
        this.UPPER_BOUND = UPPER_BOUND;
    }

    public SensedDataType getLOWER_BOUND() {
        return LOWER_BOUND;
    }

    public void setLOWER_BOUND(SensedDataType LOWER_BOUND) {
        this.LOWER_BOUND = LOWER_BOUND;
    }

    public SensedDataType getNORMAL_LEVEL() {
        return NORMAL_LEVEL;
    }

    public void setNORMAL_LEVEL(SensedDataType NORMAL_LEVEL) {
        this.NORMAL_LEVEL = NORMAL_LEVEL;
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