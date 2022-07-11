package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.humidity;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.MqttNode;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence.MySQLDriver;

public class HumidityCollector extends MqttNode<Integer, Boolean> {
    private static int MIN_HUMIDITY = 88;
    private static int MAX_HUMIDITY = 92;

    public HumidityCollector() {
        super(Boolean.FALSE, 80, "humidity", "humidity_control");
    }

    public boolean processMessage(String payload){
        DataSample humiditySample = parser.fromJson(payload, DataSample.class);
        MySQLDriver.getInstance().insertDataSample(humiditySample);
        actualValue = (int)humiditySample.getValue();

        boolean update = false;

        if(humiditySample.getManual() == 1 && !manual) {
            manual = true;
            actuatorOn = !actuatorOn;
            System.out.println("MANUAL humidifier");

        } else if (humiditySample.getManual() == 0 && manual) {
            manual = false;
            actuatorOn = !actuatorOn;
            update = true;
            System.out.println("END MANUAL humidifier");
        }

        if(!manual && actualValue < MIN_HUMIDITY && !actuatorOn) {
            actuatorOn = true;
            update = true;

        } else if(!manual && actualValue >= MAX_HUMIDITY && actuatorOn) {
            actuatorOn = false;
            update = true;
        }

        return update;
    }

    public float getMinHumidity() {
        return MIN_HUMIDITY;
    }

    public void setMinHumidity(int min) {
        MIN_HUMIDITY = min;
    }

    public float getMaxHumidity() {
        return MAX_HUMIDITY;
    }

    public void setMaxHumidity(int max) {
        MAX_HUMIDITY = max;
    }

    public boolean getHumidifierStatus() {
        return actuatorOn;
    }
}
