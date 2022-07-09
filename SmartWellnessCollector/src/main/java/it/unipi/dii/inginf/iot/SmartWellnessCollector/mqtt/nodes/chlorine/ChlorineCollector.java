package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.chlorine;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.MqttNode;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class ChlorineCollector extends MqttNode<Float, Boolean>{
    private static float MIN_PPM = 1;
    private static float MAX_PPM = (float)2.5;
    
    public ChlorineCollector() {
        super(new Boolean(false),
                new Float(1.5),
                "ppm", "chlorine_regulator");
    }

    public boolean processMessage(String payload){
        DataSample chlorineSample = parser.fromJson(payload, DataSample.class);
        actualValue = chlorineSample.getValue();

        boolean update = false;

        if(chlorineSample.getManual() == 1 && !manual) {
            manual = true;
            if(actuatorOn) {
                actuatorOn = false;
            } else {
                actuatorOn = true;
            }
            update = true;
        }
        else if(chlorineSample.getManual() == 0 && actualValue < MIN_PPM && !actuatorOn) {
            actuatorOn = true;
            update = true;
        }
        else if(chlorineSample.getManual() == 0 && actualValue >= MAX_PPM && actuatorOn) {
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