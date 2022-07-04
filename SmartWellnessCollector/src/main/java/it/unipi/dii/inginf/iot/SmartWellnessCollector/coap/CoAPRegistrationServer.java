package it.unipi.dii.inginf.iot.SmartWellnessCollector.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.nodes.CoAPNodesHandler;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class CoAPRegistrationServer extends CoapServer {
    private final static CoAPNodesHandler coapDevicesHandler = CoAPNodesHandler.getInstance();

    public CoAPRegistrationServer() throws SocketException {
        this.add(new CoapRegistrationResource());
    }

    // GET measures from sensors
    public float getPHLevel() {
        return coapDevicesHandler.getPHLevel();
    }
    public int getGymTemperature() {
        return coapDevicesHandler.getGymTemperature();
    }

    public int getGymACTemperature() {
        return coapDevicesHandler.getGymACTemperature();
    }

    public void setGymMaxTemperature(int temp) {
        coapDevicesHandler.setGymMaxTemperature(temp);
    }

    public void setGymACTemperature(int temp) {
        coapDevicesHandler.setGymACTemperature(temp);
    }

    public int getLuxValue(){
        return coapDevicesHandler.getLuxValue();
    }

    public void setGymLowerBoundMaxLux(int maxLux) {
        coapDevicesHandler.setGymLowerBoundMaxLux(maxLux);
    }

    public void setGymLowerBoundIntermediateLux(int intermediateLux) {
        coapDevicesHandler.setGymLowerBoundIntermediateLux(intermediateLux);
    }

    class CoapRegistrationResource extends CoapResource {
        public CoapRegistrationResource() {
            super("registration");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            String deviceType = exchange.getRequestText();
            String ip = exchange.getSourceAddress().getHostAddress();
            boolean success = false;

            switch (deviceType) {
                case "water_quality":
                    coapDevicesHandler.registerWaterQuality(ip);
                    success = true;
                    break;

                case "air_conditioning":
                    coapDevicesHandler.registerAirConditioning(ip);
                    success = true;
                    break;

                case "light_regulation":
                    coapDevicesHandler.registerLightRegulation(ip);
                    success = true;
                    break;
            }

            if (success)
                exchange.respond(CoAP.ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void handleDELETE(CoapExchange exchange) {
            String[] request = exchange.getRequestText().split("-");
            String ip = request[0];
            String deviceType = request[1];
            boolean success = false;

            switch (deviceType) {
                case "air_quality":
                    coapDevicesHandler.unregisterWaterQuality(ip);
                    success = true;
                    break;

                case "air_conditioning":
                    coapDevicesHandler.unregisterAirConditioning(ip);
                    success = true;
                    break;

                case "light_regulation":
                    coapDevicesHandler.unregisterLightRegulation(ip);
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