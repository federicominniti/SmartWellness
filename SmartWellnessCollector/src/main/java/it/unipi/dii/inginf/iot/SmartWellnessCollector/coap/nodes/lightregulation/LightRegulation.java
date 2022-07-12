package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.lightregulation;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoapNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Timestamp;

/**
 * Stub class for the Coap device regulating the illumination in the gym.
 * The class will receive notifications from the luxSensor and regulate the light accordingly, unless the
 * device is set in manual mode
 */
public class LightRegulation extends CoapNode<AtomicInteger, AtomicInteger> {
    private AtomicInteger LOWER_BOUND_MAX_LUX = new AtomicInteger();
    private AtomicInteger LOWER_BOUND_INTERMEDIATE_LUX = new AtomicInteger();

    public LightRegulation(int lowB, int lowBInt) {
        super(new AtomicInteger(0), new AtomicInteger(0));
        LOWER_BOUND_MAX_LUX.set(lowB);
        LOWER_BOUND_INTERMEDIATE_LUX.set(lowBInt);
    }

    /**
     * Performs a PUT request to set the light to OFF(0)/ON(2)/LOW(1)
     */
    private void lightSystemSwitch(int level) {
        if(actuator == null)
            return;
        String msg = "level=" + level;
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        logger.logError("Light System: PUT request failed");
                }
            }

            @Override
            public void onError() {
                logger.logError("Light System " + actuator.getURI());
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    private int manualSwitchSystem(){
        if(actuatorStatus.get() == 0){
            logger.logStatus("MANUAL: Gym light is ON");
            return 2;
        } else{
            logger.logStatus("MANUAL: Gym light is OFF");
            return 0;
        }
    }

    /**
     * Handler for the notifications from the luxSensor.
     * Each sample is put in the database and in the log file
     */
    private class LuxCoapHandler implements CoapHandler {
		public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample lightRegulationSample = parser.fromJson(responseString, DataSample.class);
                MySQLDriver.getInstance().insertDataSample(lightRegulationSample);
                lightRegulationSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                if(lightRegulationSample.getManual() == 1 && !manual.get()){
                    manual.set(true);
                    actuatorStatus.set(manualSwitchSystem());
                } else if(lightRegulationSample.getManual() == 0 && manual.get()){
                    manual.set(false);
                    actuatorStatus.set(manualSwitchSystem());
                }
                sensedData.set((int)lightRegulationSample.getValue());
            } catch (Exception e) {
                logger.logError("The lux sensor gave non-significant data");
                e.printStackTrace();
            }

            if(!manual.get()){
                if(sensedData.get() > LOWER_BOUND_MAX_LUX.get()){
                    actuatorStatus.set(0);
                    lightSystemSwitch(actuatorStatus.get());
                    logger.logStatus("Gym light is OFF");
                } else if(sensedData.get() < LOWER_BOUND_MAX_LUX.get() && sensedData.get() > LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    actuatorStatus.set(1);
                    lightSystemSwitch(actuatorStatus.get());
                    logger.logStatus("Gym light is LOW");
                } else if(sensedData.get() < LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    actuatorStatus.set(2);
                    lightSystemSwitch(actuatorStatus.get());
                    logger.logStatus("Gym light is HIGH");
                }
            }
        }

        public void onError() {
            logger.logError("Light Registration " + sensor.getURI());
        }
    }

    public int getLuxMaxLevel() {
        return LOWER_BOUND_MAX_LUX.get();
    }

    public void setLuxMaxLevel(int lowerBoundMaxLux) {
        LOWER_BOUND_MAX_LUX.set(lowerBoundMaxLux);
    }

    public int getLuxIntermediateLevel() {
        return LOWER_BOUND_INTERMEDIATE_LUX.get();
    }

    public void setLuxIntermediateLevel(int lowerBoundIntermediateLux) {
        LOWER_BOUND_INTERMEDIATE_LUX.set(lowerBoundIntermediateLux);
    }

    public void registerLightRegulation(String ip) {
        registerNode(ip, "light_regulation", "lux", "light");
        observeRelation = sensor.observe(new LuxCoapHandler());
    }

    public void unregisterLightRegulation(String ip) {
        unregisterNode(ip);
    }
}
