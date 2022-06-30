package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality;

import it.unipi.model.WaterQualitySample;
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

public class WaterQuality {
    private CoapClient phSensor;
    private CoapClient pumpSystem;
    private CoapObserveRelation observePH;

    AtomicFloat phValue = new AtomicFloat();
    private Map<Integer, WaterQualitySample> lastSamples;
    private AtomicFloat upperBound;
    private AtomicFloat lowerBound;
    private boolean pumpSystemOn = false;

    private Gson parser;

    public WaterQuality() {
        float startingValue = (float)6.8;
        phValue = new AtomicFloat(startingValue);

        lastSamples = new HashMap<Integer,WaterQualitySample>();
        upperBound = new AtomicFloat((float) 8.0);
        parser = new Gson();
    }

    public void registerWaterQuality(String ip) {
        System.out.print("\n[REGISTRATION] The Water Quality system: [" + ip + "] is now registered\n>");
        phSensor = new CoapClient("coap://[" + ip + "]/water_quality/ph");

        pumpSystem = new CoapClient("coap://[" + ip + "]/water_quality/pump");

        observePH = phSensor.observe(new phCoapHandler());
    }

    private void computeAverage() {
        int size = lastSamples.size();
        float sum = 0;
        for (int index: lastSamples.keySet()) {
            sum += lastSamples.get(index).getPH();
        }

        phValue.set(sum / size);
    }

    /* 
    public void unregisterAirQuality(String ip) {
        for (int i = 0; i < clientCO2SensorList.size(); i++) {
            if (clientCO2SensorList.get(i).getURI().equals(ip)) {
                clientCO2SensorList.remove(i);
                clientVentilationSystemList.remove(i);
                observeCO2List.get(i).proactiveCancel();
                observeCO2List.remove(i);
            }
        }
    }

    */

    public float getPHLevel() {
        return phValue.get();
    }

    public void setUpperBound(int upperBound) {
        this.upperBound.set(upperBound);
    }

    /* 
    private void pumpSystemSwitch(final CoapClient clientPumpSystem, boolean on) {
        if(clientPumpSystem == null)
            return;

        String msg = "mode=" + (on ? "ON" : "OFF");
        clientPumpSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] Ventilation System: PUT request unsuccessful\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Ventilation System " + clientPumpSystem.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    */

    private class phCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            try {
                WaterQualitySample waterQualitySample = parser.fromJson(responseString, WaterQualitySample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                waterQualitySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                lastSamples.put(waterQualitySample.getNode(), waterQualitySample);
                
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                computeAverage();
            } catch (Exception e) {
                System.out.print("\n[ERROR] The CO2 sensor gave non-significant data\n>");
            }

            //DEFINE LOGIC
        }

        public void onError() {
            System.err.print("\n[ERROR] Air Quality " + phSensor.getURI() + "]\n>");
        }
    }
}