package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.lightregulation;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoapNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import com.google.gson.Gson;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.sql.Timestamp;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;

public class LightRegulation extends CoapNode<AtomicInteger, AtomicInteger> {
    private AtomicInteger LOWER_BOUND_MAX_LUX = new AtomicInteger();
    private AtomicInteger LOWER_BOUND_INTERMEDIATE_LUX = new AtomicInteger();

    public LightRegulation(int lowB, int lowBInt) {
        super(new AtomicInteger(0), new AtomicInteger(0));
        LOWER_BOUND_MAX_LUX.set(lowB);
        LOWER_BOUND_INTERMEDIATE_LUX.set(lowBInt);
    }

    private void lightSystemSwitch(int level) {
        if(actuator == null)
            return;
        String msg = "level=" + level;
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.println("[ERROR] Light System: PUT request failed");
                }
            }

            @Override
            public void onError() {
                System.err.println("[ERROR] Light System " + actuator.getURI());
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    private int manualSwitchSystem(){
        if(actuatorStatus.get() == 0){
            System.out.println("[MANUAL] Luce attiva");
            return 2;
        } else{
            System.out.println("[MANUAL] Luce spenta");
            return 0;
        }
    }

    private class LuxCoapHandler implements CoapHandler {
		public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample lightRegulationSample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                lightRegulationSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                if(lightRegulationSample.getManual() == 1 && manual.get() == false){
                    manual.set(true);
                    actuatorStatus.set(manualSwitchSystem());
                } else if(lightRegulationSample.getManual() == 0 && manual.get() == true){
                    manual.set(false);
                    actuatorStatus.set(manualSwitchSystem());
                }
                sensedData.set((int)lightRegulationSample.getValue());
                //System.out.print("\n" + waterQualitySample.toString() + "\n>");
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                //computeAverage();
            } catch (Exception e) {
                System.out.println("[ERROR] The CO2 sensor gave non-significant data");
                e.printStackTrace();
            }

            if(manual.get() == false){
                if(sensedData.get() > LOWER_BOUND_MAX_LUX.get()){
                    actuatorStatus.set(0);
                    lightSystemSwitch(actuatorStatus.get());
                    System.out.println("Luce spenta");
                } else if(sensedData.get() < LOWER_BOUND_MAX_LUX.get() && sensedData.get() > LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    actuatorStatus.set(1);
                    lightSystemSwitch(actuatorStatus.get());
                    System.out.println("Luce bassa");
                } else if(sensedData.get() < LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    actuatorStatus.set(2);
                    lightSystemSwitch(actuatorStatus.get());
                    System.out.println("Luce alta");
                }
            }
        }

        public void onError() {
            System.err.println("[ERROR] Light Registration " + sensor.getURI());
        }
    }

    public int getLowerBoundMaxLux() {
        return LOWER_BOUND_MAX_LUX.get();
    }

    public void setLowerBoundMaxLux(int lowerBoundMaxLux) {
        LOWER_BOUND_MAX_LUX.set(lowerBoundMaxLux);
    }

    public int getLowerBoundIntermediateLux() {
        return LOWER_BOUND_INTERMEDIATE_LUX.get();
    }

    public void setLowerBoundIntermediateLux(int lowerBoundIntermediateLux) {
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
