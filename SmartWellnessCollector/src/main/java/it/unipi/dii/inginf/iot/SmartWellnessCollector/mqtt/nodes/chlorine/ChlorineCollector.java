package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.chlorine;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.MqttNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Stub class for the MQTT device regulating the chlorine of the pool.
 * The class will handle notifications from the MQTT sensor and set the chlorine regulator status accordingly,
 * unless the device is in manual mode.
 * Data from the sensor are also put in the database.
 */
public class ChlorineCollector extends MqttNode<Float, Boolean>{
    private static float MIN_PPM = 1;
    private static float MAX_PPM = (float)2.5;
    
    public ChlorineCollector() {
        super(Boolean.FALSE, 1.5F, "ppm", "chlorine_regulator");
    }

    /**
     * Process the payload of a message from the chlorineSensor
     * @param payload the payload of the MQTT message
     * @return true if a message should be sent to update the status of the regulator, false otherwise
     */
    public boolean processMessage(String payload){
        DataSample chlorineSample = parser.fromJson(payload, DataSample.class);
        MySQLDriver.getInstance().insertDataSample(chlorineSample);
        actualValue = chlorineSample.getValue();

        boolean update = false;

        if(chlorineSample.getManual() == 1 && !manual) {
            manual = true;
            actuatorOn = !actuatorOn;
            logger.logStatus("MANUAL chlorine regulator");

        } else if (chlorineSample.getManual() == 0 && manual) {
            manual = false;
            actuatorOn = !actuatorOn;
            update = true;
            logger.logStatus("END MANUAL chlorine regulator");
        }

        if(!manual && actualValue < MIN_PPM && !actuatorOn) {
            actuatorOn = true;
            update = true;
        }
        else if(!manual && actualValue >= MAX_PPM && actuatorOn) {
            actuatorOn = false;
            update = true;
        }

        return update;
    }

    public float getMinPPM() {
        return MIN_PPM;
    }

    public void setMinPPM(float minPPM) {
        MIN_PPM = minPPM;
    }

    public float getMaxPPM() {
        return MAX_PPM;
    }

    public void setMaxPPM(float maxPPM) {
        MAX_PPM = maxPPM;
    }

    public boolean getChlorineRegulator() {
        return actuatorOn;
    }

}