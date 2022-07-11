package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.access;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
//import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.DBDriver;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.MqttNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public class AccessCollector extends MqttNode<Integer, Integer>{
    private static int INTERMEDIATE_NUMBER = 10;
    private static int MAX_NUMBER = 15;

    private static boolean entranceDoorLocked;
    
    public AccessCollector() {
        super(0, 0, "number_of_people", "access_regulator");

        entranceDoorLocked = false;
    }

    public int calculateActuatorValue(){
        int light_colour = -1;
        if(actualValue < INTERMEDIATE_NUMBER){
            light_colour = 0;
            entranceDoorLocked = false;
        } 
        else if(actualValue < MAX_NUMBER){
            light_colour = 1;
            entranceDoorLocked = false;
        } 
        else {
            light_colour = 2;
            entranceDoorLocked = true;
        }
        return light_colour;
    }

    public boolean processMessage(String payload){
        DataSample accessSample = parser.fromJson(payload, DataSample.class);
        MySQLDriver.getInstance().insertDataSample(accessSample);
        actualValue = (int)accessSample.getValue();
        boolean update = false;
        int previousLightValue = -1;

        if((accessSample.getManual() == 1 && !manual) || (accessSample.getManual() == 0 && manual)) {
            actuatorOn = calculateActuatorValue();

            if(accessSample.getManual() == 0 && manual){
                update = true;
                logger.logStatus("END MANUAL SB Access");
            } else
                logger.logStatus("MANUAL SB Access");
            manual = !manual;
        }
        else if(accessSample.getManual() == 0) {
            previousLightValue = actuatorOn;
            actuatorOn = calculateActuatorValue();
            update = ((previousLightValue != actuatorOn) || (actualValue > MAX_NUMBER));
        }

        return update;
    }

    public int getIntermediateNumber() {
        return INTERMEDIATE_NUMBER;
    }

    public void setIntermediateNumber(int intermediateNumber) {
        INTERMEDIATE_NUMBER = intermediateNumber;
    }

    public int getMaxNumber() {
        return MAX_NUMBER;
    }

    public void setMaxNumber(int maxNumber) {
        MAX_NUMBER = maxNumber;
    }

    public String getLightColour(){
        if(actuatorOn == 0){
            return "GREEN";
        }
        else if(actuatorOn == 1){
            return "YELLOW";
        }
        else return "RED";
    }

    public String getEntranceLock(){
        if(entranceDoorLocked){
            return "CLOSED";
        } else return "OPENED";
    }
}