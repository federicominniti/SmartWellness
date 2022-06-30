package it.unipi.model;
import java.sql.Timestamp;
import java.util.Calendar;

public class WaterQualitySample {
    private int node; // Node ID
    private float pH;

    private Timestamp timestamp; // set by the collector


    public WaterQualitySample(int node, int pH, Timestamp timestamp) {
        this.node = node;
        this.pH = pH;
        this.timestamp = timestamp;
    }

    /**
     * Function used to check if the sample is valid (if it has been done in the last 30sec)
     * @return  true if the timestamp is greater than 30 seconds ago, otherwise false
     * 
     * <--- CHECK THIS
     */
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

    public float getPH() {
        return pH;
    }

    public void setPH(float pH) {
        this.pH = pH;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AirQualitySample{" +
                "node=" + node +
                ", pH=" + pH +
                ", timestamp=" + timestamp +
                '}';
    }
}
