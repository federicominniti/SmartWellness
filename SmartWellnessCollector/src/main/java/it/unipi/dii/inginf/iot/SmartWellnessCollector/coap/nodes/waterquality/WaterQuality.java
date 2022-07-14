package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.sql.Timestamp;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.utils.AtomicFloat;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoapNode;

/**
 * Stub class for the Coap device regulating the pool's pH.
 * The class will receive notifications from the phSensor and regulate the actuator accordingly, unless the
 * device is set in manual mode
 */
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

    /**
     * Performs a PUT request to set the buffer regulator ON/OFF
     */
    public void bufferRegulatorSwitch(boolean on) {
        if(actuator == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        logger.logError("Buffer Regulator: PUT request failed");
                }
            }

            @Override
            public void onError() {
                logger.logError("Buffer Regulator " + actuator.getURI());
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    /**
     * Handler for the notifications from the phSensor.
     * Each sample is put in the database and in the log file
     */
    private class PHCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample waterQualitySample = parser.fromJson(responseString, DataSample.class);
                MySQLDriver.getInstance().insertDataSample(waterQualitySample);
                waterQualitySample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                sensedData.set(waterQualitySample.getValue());
                boolean waterQualitySampleManual = (waterQualitySample.getManual() == 1);
                if(waterQualitySampleManual != manual.get()){
                    manual.set((waterQualitySample.getManual() == 1));
                    actuatorStatus.set(!actuatorStatus.get());
                    if (manual.get())
                        logger.logStatus("MANUAL: Buffer regulator is " + (actuatorStatus.get() ? "ON":"OFF"));
                    else
                        logger.logStatus("END MANUAL: Buffer regulator is " + (actuatorStatus.get() ? "ON":"OFF"));
                }
            } catch (Exception e) {
                logger.logError("The PH sensor gave non-significant data");
                e.printStackTrace();
            }


            if(manual.get()){
                return;
            }
            else if(!actuatorStatus.get() && sensedData.get() <= LOWER_BOUND.get()) {
                bufferRegulatorSwitch(true);
                actuatorStatus.set(true);
                logger.logStatus("Buffer regulator ON");
            }
            else if (actuatorStatus.get() && sensedData.get()  >= NORMAL_LEVEL.get()) {
                bufferRegulatorSwitch(false);
                actuatorStatus.set(false);
                logger.logStatus("Buffer regulator OFF");
            }
        }

        public void onError() {
            logger.logError("Water Quality " + sensor.getURI());
        }
    }

    public void setPHLowerBound(float lowerBound) {
        LOWER_BOUND.set(lowerBound);
    }

    public void setPHNormalLevel(float normalLevel) {
        NORMAL_LEVEL.set(normalLevel);
    }
}
