package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt;

import com.google.gson.Gson;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.chlorine.ChlorineCollector;
import org.eclipse.paho.client.mqttv3.*;

public class MqttHandler implements MqttCallback {

    private final String BROKER = "tcp://127.0.0.1:1883";
    private final String CLIENT_ID = "SmartSaunaCollector";
    private final int RECONNECTION_INTERVAL = 5;
    private final int MAX_NUMBER_OF_RECONNECTION_TIMES = 7;

    private MqttClient mqttClient = null;
    private Gson parser;
    private ChlorineCollector chlorineCollector;

    private Logger logger;

    public MqttHandler () {
        parser = new Gson();
        logger = Logger.getInstance();
        chlorineCollector = new ChlorineCollector();
        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                mqttClient.setCallback(this);
                connectToBroker();
                System.out.println("Connected to the broker: " + BROKER);
            }
            catch(MqttException me) {
                System.out.println("I could not connect, Retrying ...");
            }
        }while(!mqttClient.isConnected());
    }

    /**
     * Try to connect to the broker
     */
    private void connectToBroker () throws MqttException {
        mqttClient.connect();
        mqttClient.subscribe(chlorineCollector.getSENSOR_TOPIC());
        System.out.println("Subscribed to topic: " + chlorineCollector.getSENSOR_TOPIC());
    }

    /**
     * Publish a message
     * @param topic     topic of the message
     * @param message   message to send
     */
    public void publishMessage (final String topic, final String message) {
        try {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        }
        catch(MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("Connection lost with the Broker");
        // We have lost the connection, we have to try to reconnect after waiting some time
        // At each iteration we increase the time waited
        int times = 0;
        do {
            times++;
            if (times > MAX_NUMBER_OF_RECONNECTION_TIMES) {
                System.err.println("Error cannot riconnect with the brocker!");
                System.exit(-1);
            }
            try {
                Thread.sleep(RECONNECTION_INTERVAL * 1000);
                System.out.println("Reconnecting to the broker..");
                connectToBroker();
            }
            catch (MqttException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (!this.mqttClient.isConnected());
        System.out.println("Connection with the Broker restored!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        if (topic.equals(chlorineCollector.getSENSOR_TOPIC())) {
            boolean updated = chlorineCollector.processMessage(payload);
            publishMessage(chlorineCollector.getACTUATOR_TOPIC(), (chlorineCollector.getChlorineRegulator() ? "ON" : "OFF"));
            //logger.logChlorineRegulator("Chlorine regulator " + (chlorineCollector.getChlorineRegulator() ? "ON" : "OFF"));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.logInfo("Message correctly delivered");
    }

    public ChlorineCollector getHumidityCollector() {
        return chlorineCollector;
    }
}