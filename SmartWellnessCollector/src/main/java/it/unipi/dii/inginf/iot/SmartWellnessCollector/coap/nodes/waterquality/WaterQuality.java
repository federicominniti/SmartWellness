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
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoapNode;

public class WaterQuality extends CoapNode<AtomicFloat, AtomicBoolean> {
    private AtomicFloat LOWER_BOUND = new AtomicFloat();
    private AtomicFloat NORMAL_LEVEL = new AtomicFloat();

    public WaterQuality(float lowB, float normal) {
        super(new AtomicFloat(), new AtomicBoolean(false));
        LOWER_BOUND.set(lowB);
        NORMAL_LEVEL.set(normal);
    }

    public void registerWaterQuality(String ip) {
        registerNode(ip, "water_quality", "ph", "buffer");
        observeRelation = sensor.observe(new PHCoapHandler());
    }

    public void unregisterWaterQuality(String ip) {
        unregisterNode(ip);
    }

    public void bufferRegulatorSwitch(boolean on) {
        if(actuator == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        System.out.print("\n[ERROR] Buffer Regulator: PUT request failed\n>");
                }
            }

            @Override
            public void onError() {
                System.err.print("\n[ERROR] Buffer Regulator " + actuator.getURI() + "]\n>");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    private class PHCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample waterQualitySample = parser.fromJson(responseString, DataSample.class);
                //DBDriver.getInstance().insertAirQualitySample(airQualitySample);
                waterQualitySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                sensedData.set(waterQualitySample.getValue());
                boolean waterQualitySampleManual = (waterQualitySample.getManual() == 1 ? true:false);
                if(waterQualitySampleManual != manual.get()){
                    manual.set((waterQualitySample.getManual() == 1 ? true:false));
                    actuatorStatus.set(!actuatorStatus.get());
                    if (actuatorStatus.get())
                        System.out.println("MANUAL: buffer regulator ON");
                    else
                        System.out.println("MANUAL: buffer regulator OFF");
                }

                //System.out.print("\n" + waterQualitySample.toString() + "\n>");
                // remove old samples from the lastAirQualitySamples map
                //lastSamples.entrySet().removeIf(entry -> !entry.getValue().isValid());
                //computeAverage();
            } catch (Exception e) {
                System.out.print("\n[ERROR] The CO2 sensor gave non-significant data\n>");
                e.printStackTrace();
            }


            if(manual.get()){
                return;
            }
            else if(!actuatorStatus.get() && sensedData.get() < LOWER_BOUND.get()) {
                //logger.logAirQuality("CO2 level is HIGH: " + co2Level.get() + " ppm, the ventilation system is switched ON");
                //for (CoapClient clientPumpSystem: clientVentilationSystemList) {
                bufferRegulatorSwitch(true);
                //}
                actuatorStatus.set(true);
                System.out.println("Buffer regulator ON");
            }

            // We don't turn off the ventilation as soon as the value is lower than the upper bound,
            // but we leave a margin so that we don't have to turn on the system again right away
            else if (actuatorStatus.get() && sensedData.get()  >= NORMAL_LEVEL.get()) {
                //logger.logAirQuality("CO2 level is now fine: " + co2Level.get() + " ppm. Switch OFF the ventilation system");
                //for (CoapClient clientVentilationSystem: clientVentilationSystemList) {
                bufferRegulatorSwitch(false);
                //}
                actuatorStatus.set(false);
                System.out.println("Buffer regulator OFF");
            }

            //else
            //{
            //    logger.logAirQuality("C02 level is fine: " + co2Level.get() + " ppm");
            //}
        }

        public void onError() {
            System.err.print("\n[ERROR] Water Quality " + sensor.getURI() + "]\n>");
        }
    }
}
