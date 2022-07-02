package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import com.google.gson.Gson;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.utils.AtomicFloat;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;

public class WaterQuality {
    private CoapClient phSensor;
    private CoapClient pumpSystem;
    private CoapObserveRelation observePH;
    private Logger logger;

    AtomicFloat phValue = new AtomicFloat();
    private Map<Integer, DataSample> lastSamples;
    private static final AtomicFloat LOWER_BOUND = new AtomicFloat((float)7.2);
    private static final AtomicFloat NORMAL_LEVEL = new AtomicFloat((float)7.5);

    private boolean pumpSystemStatus = false;
    private boolean manualPump = false;

    private Gson parser;

    public WaterQuality() {
        float startingValue = (float)6.8;
        phValue = new AtomicFloat(startingValue);

        lastSamples = new HashMap<Integer,DataSample>();
        logger = Logger.getInstance();
        parser = new Gson();
    }

    public void registerWaterQuality(String ip) {
        System.out.print("\n[REGISTRATION] The Water Quality system: [" + ip + "] is now registered\n>");
        phSensor = new CoapClient("coap://[" + ip + "]/water_quality/ph");

        pumpSystem = new CoapClient("coap://[" + ip + "]/water_quality/pump");

        observePH = phSensor.observe(new phCoapHandler());
    }

    // TO DO vedere se usare computeAverage
    private void computeAverage() {
        int size = lastSamples.size();
        float sum = 0;
        for (int index: lastSamples.keySet()) {
            sum += lastSamples.get(index).getValue();
        }

        phValue.set(sum / size);
    }
    
    public void unregisterWaterQuality(String ip) {
        //for (int i = 0; i < clientCO2SensorList.size(); i++) {
        if (phSensor.getURI().equals(ip)) {
            observePH.proactiveCancel();
        }
        //}
    }

    public float getPHLevel() {
        return phValue.get();
    }

    private void pumpSystemSwitch(final CoapClient clientPumpSystem, boolean on) {
        if(clientPumpSystem == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        clientPumpSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] Pump System: PUT request failed\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Pump System " + clientPumpSystem.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    private class phCoapHandler implements CoapHandler {
		public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample waterQualitySample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                waterQualitySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                lastSamples.put(waterQualitySample.getNode(), waterQualitySample);
                phValue.set(waterQualitySample.getValue());
                //System.out.print("\n" + waterQualitySample.toString() + "\n>");
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                //computeAverage();
            } catch (Exception e) {
                System.out.print("\n[ERROR] The CO2 sensor gave non-significant data\n>");
                e.printStackTrace();
            }
            

            if(manualPump){
                pumpSystemStatus = !pumpSystemStatus;
                if(pumpSystemStatus){
                    System.out.println("[MANUAL] Pompa attiva");
                } else{
                    System.out.println("[MANUAL] Pompa disattiva");
                }
            }
            else if(!pumpSystemStatus && phValue.get() < LOWER_BOUND.get()) {
                //logger.logAirQuality("CO2 level is HIGH: " + co2Level.get() + " ppm, the ventilation system is switched ON");
                //for (CoapClient clientPumpSystem: clientVentilationSystemList) {
                pumpSystemSwitch(pumpSystem,true);
                //}
                pumpSystemStatus = true;
                System.out.println("Pompa attiva");
            }

            // We don't turn off the ventilation as soon as the value is lower than the upper bound,
            // but we leave a margin so that we don't have to turn on the system again right away
            else if (pumpSystemStatus && phValue.get()  >= NORMAL_LEVEL.get()) {
                //logger.logAirQuality("CO2 level is now fine: " + co2Level.get() + " ppm. Switch OFF the ventilation system");
                //for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                pumpSystemSwitch(pumpSystem,false);
                //}
                pumpSystemStatus = false;
                System.out.println("Pompa disattiva");
            }

            //else
            //{
            //    logger.logAirQuality("C02 level is fine: " + co2Level.get() + " ppm");
            //}
        }

        public void onError() {
            System.err.print("\n[ERROR] Air Quality " + phSensor.getURI() + "]\n>");
        }
    }
}