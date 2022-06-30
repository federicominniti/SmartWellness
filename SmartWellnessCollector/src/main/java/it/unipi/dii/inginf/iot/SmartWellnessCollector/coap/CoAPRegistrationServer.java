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

    public void setPHUpperBound(int phUpperBound) {
        coapDevicesHandler.setPHUpperBound(phUpperBound);
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
            if (deviceType.equals("water_quality")) {
                coapDevicesHandler.registerWaterQuality(ip);
                success = true;
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

            if (deviceType.equals("water_quality")) {
                //coapDevicesHandler.unregister...
                success = true;
            }

            if(success)
                exchange.respond(CoAP.ResponseCode.DELETED, "Cancellation Completed!".getBytes(StandardCharsets.UTF_8));
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Cancellation not allowed!".getBytes(StandardCharsets.UTF_8));
        }
    }
}