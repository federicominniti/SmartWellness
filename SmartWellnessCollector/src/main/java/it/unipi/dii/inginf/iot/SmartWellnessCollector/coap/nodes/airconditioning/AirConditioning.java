package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.airconditioning;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.utils.AtomicFloat;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;

public class AirConditioning {
    private CoapClient temperatureSensor;
    private CoapClient acSystem;
    private CoapObserveRelation observeTemperature;
    private Logger logger;

    private AtomicInteger temperature;
    private Map<Integer, DataSample> lastSamples;
    private static final AtomicInteger UPPER_BOUND = new AtomicInteger(20);
    private static final AtomicInteger NORMAL_LEVEL = new AtomicInteger(18);

    private AtomicBoolean acSystemStatus = new AtomicBoolean(false);
    private AtomicBoolean manualAC = new AtomicBoolean(false);

    private Gson parser;

    public AirConditioning() {
        temperature = new AtomicInteger(18);
        lastSamples = new HashMap<Integer,DataSample>();
        logger = Logger.getInstance();
        parser = new Gson();
    }

    public void registerACSystem(String ip) {
        System.out.print("\n[REGISTRATION] The AC system: [" + ip + "] is now registered\n>");
        temperatureSensor = new CoapClient("coap://[" + ip + "]/air_conditioning/temperature");
        acSystem = new CoapClient("coap://[" + ip + "]/air_conditioning/ac");

        observeTemperature = temperatureSensor.observe(new tempCoapHandler());
    }

    public void unregisterAirConditioning(String ip) {
        //for (int i = 0; i < clientCO2SensorList.size(); i++) {
        if (temperatureSensor.getURI().equals(ip)) {
            observeTemperature.proactiveCancel();
        }
        //}
    }

    public int getTemperature() {
        return temperature.get();
    }

    public int getNormalLevel() {
        return NORMAL_LEVEL.get();
    }

    private void acSystemSwitch(final CoapClient clientACSystem, boolean on) {
        if(clientACSystem == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        clientACSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] AC System: PUT request failed\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] AC System " + clientACSystem.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    public void acSetTemperature(int temp) {
        if(acSystem == null)
            return;

        String msg = "ac_temp=" + temp;
        acSystem.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] AC System: PUT request failed\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] AC System " + acSystem.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);

        NORMAL_LEVEL.set(temp);
    }

    public void setMaxTemperature(int temp) {
        UPPER_BOUND.set(temp);
    }


    private class tempCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample temperatureSample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                temperatureSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                lastSamples.put(temperatureSample.getNode(), temperatureSample);
                temperature.set((int)temperatureSample.getValue());
                boolean temperatureSampleManual = (temperatureSample.getManual() == 1 ? true:false);
                if(temperatureSampleManual != manualAC.get()){
                    manualAC.set(temperatureSampleManual);
                    acSystemStatus.set(!acSystemStatus.get());
                    if (acSystemStatus.get())
                        System.out.println("MANUAL: AC is ON");
                    else
                        System.out.println("MANUAL: AC is OFF");
                }
                //System.out.print("\n" + waterQualitySample.toString() + "\n>");
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                //computeAverage();
            } catch (Exception e) {
                System.out.print("\n[ERROR] The temperature sensor gave non-significant data\n>");
                e.printStackTrace();
            }


            if(manualAC.get()){
                return;
            }
            else if(!acSystemStatus.get() && temperature.get() >= UPPER_BOUND.get()) {
                //logger.logAirQuality("CO2 level is HIGH: " + co2Level.get() + " ppm, the ventilation system is switched ON");
                //for (CoapClient clientPumpSystem: clientVentilationSystemList) {
                acSystemSwitch(acSystem,true);
                //}
                acSystemStatus.set(true);
                System.out.println("AC is now ON");
            }

            // We don't turn off the ventilation as soon as the value is lower than the upper bound,
            // but we leave a margin so that we don't have to turn on the system again right away
            else if (acSystemStatus.get() && temperature.get()  == NORMAL_LEVEL.get()) {
                //logger.logAirQuality("CO2 level is now fine: " + co2Level.get() + " ppm. Switch OFF the ventilation system");
                //for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                acSystemSwitch(acSystem,false);
                //}
                acSystemStatus.set(false);
                System.out.println("AC is now OFF");
            }

            //else
            //{
            //    logger.logAirQuality("C02 level is fine: " + co2Level.get() + " ppm");
            //}
        }

        public void onError() {
            System.err.print("\n[ERROR] Air conditioning " + temperatureSensor.getURI() + "]\n>");
        }
    }
}
