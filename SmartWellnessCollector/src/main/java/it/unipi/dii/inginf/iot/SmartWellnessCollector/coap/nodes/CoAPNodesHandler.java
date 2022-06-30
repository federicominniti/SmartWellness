package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality.WaterQuality;

public class CoAPNodesHandler {
    private WaterQuality waterQuality;

    private static CoAPNodesHandler instance = null;

    private CoAPNodesHandler() {
        waterQuality = new WaterQuality();
    }

    public static CoAPNodesHandler getInstance() {
        if(instance == null)
            instance = new CoAPNodesHandler();

        return instance;
    }

    /*      REGISTER AND UNREGISTER DEVICES     */
    public void registerWaterQuality(String ip) {
        waterQuality.registerWaterQuality(ip);
    }

    /*      GET MEASURES FROM SENSORS     */
    public float getPHLevel() {
        return waterQuality.getPHLevel();
    }

    public void setPHUpperBound(int upperBound) {
        waterQuality.setUpperBound(upperBound);
    }
}