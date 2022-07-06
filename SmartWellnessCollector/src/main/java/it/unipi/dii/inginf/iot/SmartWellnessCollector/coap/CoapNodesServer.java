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

public class CoapNodesServer extends CoapServer {
    private WaterQuality waterQuality;
    private AirConditioning airConditioning;
    private LightRegulation lightRegulation;

    public CoapNodesServer() throws SocketException {
        this.add(new CoapRegistrationResource());
        waterQuality = new WaterQuality();
        airConditioning = new AirConditioning(18, 19);
        lightRegulation = new LightRegulation(350, 1500);
    }

    /*      REGISTER AND UNREGISTER DEVICES     */
    public void registerWaterQuality(String ip) {
        waterQuality.registerWaterQuality(ip);
    }

    /*      GET MEASURES FROM SENSORS     */
    public float getPHLevel() {
        return waterQuality.getPHLevel();
    }

    public void setBufferRegulator(boolean on) {
        waterQuality.bufferRegulatorSwitch(on);
    }

    public void unregisterWaterQuality(String ip) {
        waterQuality.unregisterWaterQuality(ip);
    }

    /*      REGISTER AND UNREGISTER DEVICES     */

    /*      GET MEASURES FROM SENSORS     */
    public int getGymTemperature() {
        return airConditioning.getSensedData().get();
    }

    public void setGymACTemperature(int temp) {
        airConditioning.setNORMAL_LEVEL(temp);
    }

    public int getGymACTemperature() {
        return airConditioning.getNORMAL_LEVEL();
    }

    public void setGymMaxTemperature(int temp) {
        airConditioning.setUPPER_BOUND(temp);
    }

    public int getLuxValue() {
        return lightRegulation.getSensedData().get();
    }

    public void unregisterLightRegulation(String ip) {
        lightRegulation.unregisterLightRegulation(ip);
    }

    public void setGymLowerBoundMaxLux(int maxLux) {
        lightRegulation.setLowerBoundMaxLux(maxLux);
    }

    public void setGymLowerBoundIntermediateLux(int intermediateLux) {
        lightRegulation.setLowerBoundIntermediateLux(intermediateLux);
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