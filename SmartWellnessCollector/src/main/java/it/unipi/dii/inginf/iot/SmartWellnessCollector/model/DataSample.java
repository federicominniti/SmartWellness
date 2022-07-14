package it.unipi.dii.inginf.iot.SmartWellnessCollector.model;
import java.sql.Timestamp;

public class DataSample {
    private int node; // node id, identifies the device during a single deployment
    private float value; //values of the specific sensor
    private int manual; //if set to 1, the device is in manual mode and the actuator must not be controlled remotely
    private String sensorType; //the name of the sensor, identifies the device across different deployments
    private Timestamp timestamp; // set by the collector


    public DataSample(int node, float value, int manual, String sensorType, Timestamp timestamp) {
        this.node = node;
        this.value = value;
        this.manual = manual;
        this.sensorType = sensorType;
        this.timestamp = timestamp;
    }


    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getManual() {
        return manual;
    }

    public void setManual(int manual) {
        this.manual = manual;
    }

    @Override
    public String toString() {
        return "DataSample{" +
                "node=" + node +
                ", value=" + value +
                ", manual=" + manual +
                ", sensorType=" + sensorType +
                ", timestamp=" + timestamp +
                '}';
    }
}
