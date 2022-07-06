package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.airconditioning;


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
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.utils.AtomicFloat;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;


public class AirConditioning extends CoapNode<AtomicInteger, AtomicBoolean> {
    public AirConditioning(int lowB, int upB, int normal) {
        super(new AtomicInteger(upB),
                new AtomicInteger(lowB),
                new AtomicInteger(normal),
                new AtomicBoolean(false));
    }

    public void registerACSystem(String ip) {
        registerNode(ip, "air_conditioning", "temperature", "ac");
        observeRelation = sensor.observe(new acCoapHandler());
    }

    public void acSystemSwitch(boolean on) {
        if (actuator == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("[ERROR] AC System: PUT request failed>");
                }
            }

            @Override
            public void onError() {
                System.err.print("[ERROR] AC System " + actuator.getURI() + "]>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    public void acSetTemperature(int temp) {
        if(actuator == null)
            return;

        String msg = "ac_temp=" + temp;
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("[ERROR] AC System: PUT request failed>");
                }
            }

            @Override
            public void onError() {
                System.err.print("[ERROR] AC System " + actuator.getURI() + "]>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);

        NORMAL_LEVEL.set(temp);
    }


    private class acCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample temperatureSample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                temperatureSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                lastSamples.add(temperatureSample);
                sensedData.set((int)temperatureSample.getValue());

                boolean temperatureSampleManual = (temperatureSample.getManual() == 1 ? true:false);
                if(temperatureSample.getManual() == 1 && manual.get()){
                    manual.set(temperatureSampleManual);
                    actuatorStatus.set(!actuatorStatus.get());
                    if (actuatorStatus.get())
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


            if(manual.get()){
                return;
            }
            else if(!actuatorStatus.get() && sensedData.get() >= UPPER_BOUND.get()) {
                //logger.logAirQuality("CO2 level is HIGH: " + co2Level.get() + " ppm, the ventilation system is switched ON");
                //for (CoapClient clientPumpSystem: clientVentilationSystemList) {
                acSystemSwitch(true);
                //}
                actuatorStatus.set(true);
                System.out.println("AC is now ON");
            }

            // We don't turn off the ventilation as soon as the value is lower than the upper bound,
            // but we leave a margin so that we don't have to turn on the system again right away
            else if (actuatorStatus.get() && sensedData.get()  == NORMAL_LEVEL.get()) {
                //logger.logAirQuality("CO2 level is now fine: " + co2Level.get() + " ppm. Switch OFF the ventilation system");
                //for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                acSystemSwitch(false);
                //}
                actuatorStatus.set(false);
                System.out.println("AC is now OFF");
            }

            //else
            //{
            //    logger.logAirQuality("C02 level is fine: " + co2Level.get() + " ppm");
            //}
        }

        public void onError() {
            System.err.print("\n[ERROR] Air conditioning " + sensor.getURI() + "]\n>");
        }
    }

}