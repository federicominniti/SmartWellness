package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import java.util.concurrent.atomic.AtomicInteger;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.waterquality.WaterQuality;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.airconditioning.AirConditioning;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.lightregulation.LightRegulation;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Coap devices will register with the CoapNodesServer through a POST request
 */
public class CoapNodesServer extends CoapServer {
    private final WaterQuality waterQuality;
    private final AirConditioning airConditioning;
    private final LightRegulation lightRegulation;

    /**
     * Initializes Coap nodes stubs with their default working settings for actuators control
     */
    public CoapNodesServer() throws SocketException {
        this.add(new CoapRegistrationResource());
        waterQuality = new WaterQuality((float)7.2, (float)7.6);
        airConditioning = new AirConditioning(18, 19);
        lightRegulation = new LightRegulation(350, 1500);
    }

    /*--------------------GYM TEMPERATURE-------------------*/
    public int getGymTemperature() {
        return airConditioning.getSensedData().get();
    }

    public void setGymACTemperature(int temp) {
        airConditioning.setACTemperature(temp);
    }

    public int getGymACTemperature() {
        return airConditioning.getNORMAL_LEVEL();
    }

    public void setGymACUpperBound(int temp) {
        airConditioning.setUPPER_BOUND(temp);
    }


    /*--------------------GYM LIGHT-------------------*/
    public int getGymLuxValue() {
        return lightRegulation.getSensedData().get();
    }

    public void setGymCrepuscularMaxLevel(int maxLux) {
        lightRegulation.setLuxMaxLevel(maxLux);
    }

    public void setGymCrepuscularIntermediateLevel(int intermediateLux) {
        lightRegulation.setLuxIntermediateLevel(intermediateLux);
    }

    /*--------------------POOL PH-------------------*/
    public float getPoolPHLevel() {
        return waterQuality.getSensedData().get();
    }

    public void setPoolPHLowerBound(float ph){
        waterQuality.setPHLowerBound(ph);
    }

    public void setPoolPHNormalLevel(float ph){
        waterQuality.setPHNormalLevel(ph);
    }

    class CoapRegistrationResource extends CoapResource {
        public CoapRegistrationResource() {
            super("registration");
        }

        /**
         * Handles registration for each type of device
         */
        @Override
        public void handlePOST(CoapExchange exchange) {
            String deviceType = exchange.getRequestText();
            String ip = exchange.getSourceAddress().getHostAddress();
            boolean success = false;

            switch (deviceType) {
                case "water_quality":
                    waterQuality.registerWaterQuality(ip);
                    success = true;
                    break;

                case "air_conditioning":
                    airConditioning.registerACSystem(ip);
                    success = true;
                    break;

                case "light_regulation":
                    lightRegulation.registerLightRegulation(ip);
                    success = true;
                    break;
            }

            if (success)
                exchange.respond(CoAP.ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Handles DELETE requests for each type of device
         */
        @Override
        public void handleDELETE(CoapExchange exchange) {
            String[] request = exchange.getRequestText().split("-");
            String ip = request[0];
            String deviceType = request[1];
            boolean success = false;

            switch (deviceType) {
                case "air_quality":
                    waterQuality.unregisterWaterQuality(ip);
                    success = true;
                    break;

                case "air_conditioning":
                    airConditioning.unregisterNode(ip);
                    success = true;
                    break;

                case "light_regulation":
                    lightRegulation.unregisterLightRegulation(ip);
                    success = true;
                    break;
            }

            if(success)
                exchange.respond(CoAP.ResponseCode.DELETED, "Cancellation Completed!".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Cancellation not allowed!".getBytes(StandardCharsets.UTF_8));
        }
    }
}