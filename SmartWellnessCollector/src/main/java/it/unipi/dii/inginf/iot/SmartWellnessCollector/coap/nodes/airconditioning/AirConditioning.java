package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.airconditioning;


import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoapNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stub class for the Coap device regulating the air conditioning in the gym.
 * The class will receive notifications from the temperatureSensor and regulate the AC accordingly, unless the
 * device is set in manual mode
 */
public class AirConditioning extends CoapNode<AtomicInteger, AtomicBoolean> {
    private AtomicInteger NORMAL_LEVEL = new AtomicInteger();
    private AtomicInteger UPPER_BOUND = new AtomicInteger();

    public AirConditioning(int normal, int upB) {
        super(new AtomicInteger(), new AtomicBoolean());
        NORMAL_LEVEL.set(normal);
        UPPER_BOUND.set(upB);
    }

    public void registerACSystem(String ip) {
        registerNode(ip, "air_conditioning", "temperature", "ac");
        observeRelation = sensor.observe(new acCoapHandler());
    }

    /**
     * Performs a PUT request to set the AC system ON/OFF
     */
    public void acSystemSwitch(boolean on) {
        if (actuator == null)
            return;

        String msg = "status=" + (on ? "ON" : "OFF");
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        logger.logError("AC System: PUT request failed");
                }
            }

            @Override
            public void onError() {
                logger.logError("AC System " + actuator.getURI() + "]");
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    /**
     * Performs a PUT request to set working temperature of the AC
     */
    public void setACTemperature(int temp) {
        if(actuator == null)
            return;

        String msg = "ac_temp=" + temp;
        actuator.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                if(coapResponse != null) {
                    if(!coapResponse.isSuccess())
                        logger.logError("AC System: PUT request failed");
                }
            }

            @Override
            public void onError() {
                logger.logError("AC System " + actuator.getURI());
            }
        }, msg, MediaTypeRegistry.TEXT_PLAIN);

        NORMAL_LEVEL.set(temp);
    }

    /**
     * Handler for the notifications from the temperatureSensor.
     * Each sample is put in the database and in the log file
     */
    private class acCoapHandler implements CoapHandler {
        public void onLoad(CoapResponse response) {
            String responseString = new String(response.getPayload());
            logger.logInfo(responseString);
            try {
                DataSample temperatureSample = parser.fromJson(responseString, DataSample.class);
                MySQLDriver.getInstance().insertDataSample(temperatureSample);
                temperatureSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
                sensedData.set((int)temperatureSample.getValue());

                boolean temperatureSampleManual = (temperatureSample.getManual() == 1);
                if(temperatureSampleManual != manual.get()){
                    manual.set(temperatureSampleManual);
                    actuatorStatus.set(!actuatorStatus.get());
                    if (manual.get())
                        logger.logStatus("MANUAL: AC is " + (actuatorStatus.get() ? "ON":"OFF"));
                    else
                        logger.logStatus("END MANUAL: AC is " + (actuatorStatus.get() ? "ON":"OFF"));
                }
            } catch (Exception e) {
                logger.logError("The temperature sensor gave non-significant data");
                e.printStackTrace();
            }


            if(manual.get()){
                return;
            }
            else if(!actuatorStatus.get() && sensedData.get() >= UPPER_BOUND.get()) {
                acSystemSwitch(true);
                actuatorStatus.set(true);
                logger.logStatus("AC is now ON");
            }

            else if (actuatorStatus.get() && sensedData.get()  == NORMAL_LEVEL.get()) {
                acSystemSwitch(false);
                actuatorStatus.set(false);
                logger.logStatus("AC is now OFF");
            }
        }

        public void onError() {
            logger.logError("Air conditioning " + sensor.getURI());
        }
    }

    public int getNORMAL_LEVEL() {
        return NORMAL_LEVEL.get();
    }

    public void setNORMAL_LEVEL(int normal) {
        NORMAL_LEVEL.set(normal);
    }

    public int getUPPER_BOUND() {
        return UPPER_BOUND.get();
    }

    public void setUPPER_BOUND(int upper) {
        UPPER_BOUND.set(upper);
    }
}
