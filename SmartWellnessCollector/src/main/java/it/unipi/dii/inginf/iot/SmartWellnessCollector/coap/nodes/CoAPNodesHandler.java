package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality.WaterQuality;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.airconditioning.AirConditioning;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.lightregulation.LightRegulation;
public class CoAPNodesHandler {
    private WaterQuality waterQuality;
    private AirConditioning airConditioning;
    private LightRegulation lightRegulation;

    private static CoAPNodesHandler instance = null;

    private CoAPNodesHandler() {
        waterQuality = new WaterQuality();
        airConditioning = new AirConditioning();
        lightRegulation = new LightRegulation();
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

    public void setBufferRegulator(boolean on) {
        waterQuality.bufferRegulatorSwitch(on);
    }

    public void unregisterWaterQuality(String ip) {
        waterQuality.unregisterWaterQuality(ip);
    }

    /*      REGISTER AND UNREGISTER DEVICES     */
    public void registerAirConditioning(String ip) {
        airConditioning.registerACSystem(ip);
    }

    /*      GET MEASURES FROM SENSORS     */
    public int getGymTemperature() {
        return airConditioning.getTemperature();
    }

    public void setGymACTemperature(int temp) {
        airConditioning.acSetTemperature(temp);
    }

    public int getGymACTemperature() {
        return airConditioning.getNormalLevel();
    }

    public void setGymMaxTemperature(int temp) {
        airConditioning.setMaxTemperature(temp);
    }

    public void unregisterAirConditioning(String ip) {
        airConditioning.unregisterAirConditioning(ip);
    }

    public void registerLightRegulation(String ip) {
        lightRegulation.registerLightRegulation(ip);
    }

    public int getLuxValue() {
        return lightRegulation.getLuxValue();
    }

    public void unregisterLightRegulation(String ip) {
        lightRegulation.unregisterLightRegulation(ip);
    }

    public void setGymLowerBoundMaxLux(int maxLux) {
        lightRegulation.setGymLowerBoundMaxLux(maxLux);
    }

    public void setGymLowerBoundIntermediateLux(int intermediateLux) {
        lightRegulation.setGymLowerBoundIntermediateLux(intermediateLux);
    }

}
