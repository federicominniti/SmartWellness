package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.lightregulation;

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

public class LightRegulation {
    private CoapClient crepuscularSensor;
    private CoapClient lightSystem;
    private CoapObserveRelation observeLUX;
    private Logger logger;

    AtomicInteger luxValue = new AtomicInteger();
    private List<DataSample> lastSamples;
    private static AtomicInteger LOWER_BOUND_MAX_LUX = new AtomicInteger(1500);
    private static AtomicInteger LOWER_BOUND_INTERMEDIATE_LUX = new AtomicInteger(350);

    private AtomicInteger lightLevel = new AtomicInteger(0);
    private AtomicBoolean manualLight = new AtomicBoolean(false);

    private Gson parser;

    public LightRegulation() {
        int startingValue = (int)6.8;
        luxValue = new AtomicInteger(startingValue);

        lastSamples = new ArrayList<DataSample>();
        logger = Logger.getInstance();
        parser = new Gson();
    }

    public void registerLightRegulation(String ip) {
        System.out.println("[REGISTRATION] The Water Quality system: [" + ip + "] is now registered");
        crepuscularSensor = new CoapClient("coap://[" + ip + "]/light_regulation/lux");

        lightSystem = new CoapClient("coap://[" + ip + "]/light_regulation/light");

        observeLUX = crepuscularSensor.observe(new luxCoapHandler());
    }

    // TO DO vedere se usare computeAverage
    private void computeAverage() {
        int size = lastSamples.size();
        float sum = 0;
        for(int i = 0; i<lastSamples.size(); i++){
            sum += lastSamples.get(i).getValue();
        }

        luxValue.set((int)Math.floor(sum / size));
    }
    
    public void unregisterLightRegulation(String ip) {
        //for (int i = 0; i < clientCO2SensorList.size(); i++) {
        if (crepuscularSensor.getURI().equals(ip)) {
            observeLUX.proactiveCancel();
        }
        //}
    }

    public int getLuxValue() {
        return luxValue.get();
    }

    public void setGymLowerBoundMaxLux(int maxLux){
        LOWER_BOUND_MAX_LUX.set(maxLux);
    }

    public void setGymLowerBoundIntermediateLux(int intermediateLux){
        LOWER_BOUND_INTERMEDIATE_LUX.set(intermediateLux);
    }

    private void lightSystemSwitch(final CoapClient clientLightSystem, int level) {
        if(clientLightSystem == null)
            return;
        String msg = "level=" + level;
        clientLightSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.println("[ERROR] Light System: PUT request failed");
                }
            }

            @Override
            public void onError() {
                System.err.println("[ERROR] Light System " + clientLightSystem.getURI());
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    private int manualLightSwitchSystem(){
        if(lightLevel.get() == 0){
            System.out.println("[MANUAL] Luce attiva");
            return 2;
        } else{
            System.out.println("[MANUAL] Luce spenta");
            return 0;
        }
    }

    private class luxCoapHandler implements CoapHandler { 
		public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample lightRegulationSample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                lightRegulationSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                if(lightRegulationSample.getManual() == 1 && manualLight.get() == false){
                    manualLight.set(true);
                    lightLevel.set(manualLightSwitchSystem());
                } else if(lightRegulationSample.getManual() == 0 && manualLight.get() == true){
                    manualLight.set(false);
                    lightLevel.set(manualLightSwitchSystem());
                }
                lastSamples.add(lightRegulationSample);
                luxValue.set((int)lightRegulationSample.getValue());
                //System.out.print("\n" + waterQualitySample.toString() + "\n>");
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                //computeAverage();
            } catch (Exception e) {
                System.out.println("[ERROR] The CO2 sensor gave non-significant data");
                e.printStackTrace();
            }

            if(manualLight.get() == false){
                if(luxValue.get() > LOWER_BOUND_MAX_LUX.get()){
                    lightLevel.set(0);
                    lightSystemSwitch(lightSystem, lightLevel.get());
                    System.out.println("Luce spenta");
                } else if(luxValue.get() < LOWER_BOUND_MAX_LUX.get() && getLuxValue() > LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    lightLevel.set(1);
                    lightSystemSwitch(lightSystem, lightLevel.get());
                    System.out.println("Luce bassa");
                } else if(luxValue.get() < LOWER_BOUND_INTERMEDIATE_LUX.get()){
                    lightLevel.set(2);
                    lightSystemSwitch(lightSystem, lightLevel.get());
                    System.out.println("Luce alta");
                }
            }
        }

        public void onError() {
            System.err.println("[ERROR] Light Registration " + crepuscularSensor.getURI());
        }
    }
}
