package it.unipi.dii.inginf.iot.SmartWellnessCollector.model;
import java.sql.Timestamp;
import java.util.Calendar;

public class DataSample {
    private int node; // Node ID
    private float value; //Values of the specific sensor
    private int manual;

    private String sensorType;
    private Timestamp timestamp; // set by the collector


    public DataSample(int node, float value, int manual, String sensorType, Timestamp timestamp) {
        this.node = node;
        this.value = value;
        this.manual = manual;
        this.sensorType = sensorType;
        this.timestamp = timestamp;
    }

    /**
     * Function used to check if the sample is valid (if it has been done in the last 30sec)
     * @return  true if the timestamp is greater than 30 seconds ago, otherwise false
     * 
     * <--- CHECK THIS
     */

     //TO DO
    public boolean isValid ()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, -30); // -30 seconds
        Timestamp thirtySecondsAgo = new Timestamp(calendar.getTime().getTime());
        return timestamp.after(thirtySecondsAgo);
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
                ", sensorType=" + sensorType +
                ", timestamp=" + timestamp +
                '}';
    }
}
