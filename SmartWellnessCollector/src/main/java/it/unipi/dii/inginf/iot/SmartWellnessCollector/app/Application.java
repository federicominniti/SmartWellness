package it.unipi.dii.inginf.iot.SmartWellnessCollector.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.CoapNodesServer;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.MqttHandler;

public class Application {
    public static void main(String[] args) throws Exception {
        CoapNodesServer coapNodesServer = new CoapNodesServer();
        coapNodesServer.start();
        MqttHandler mqttHandler = new MqttHandler();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        String[] parts;
        int tempInt;
        float tempFloat; 
        while (true) {
            System.out.print("> ");
            try {
                command = bufferedReader.readLine().toLowerCase();
                parts = command.split(" ");

                switch (parts[0]) {
                    case "help":
                        printUsage();
                        break;
                    case "h":
                        printUsage();
                        break;
                    /*GYM TEMPERATURE*/
                    case "get_gym_temperature":
                        tempInt = coapNodesServer.getGymTemperature();
                        System.out.println("Gym temperature is " + tempInt);
                        break;
                    case "set_gym_ac_temperature":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting gym AC temperature to " + tempInt);
                        coapNodesServer.setGymACTemperature(tempInt);
                        System.out.println("Done!");
                        break;
                    case "get_gym_ac_temperature":
                        tempInt = coapNodesServer.getGymACTemperature();
                        System.out.println("Gym AC temperature is " + tempInt);
                        break;
                    case "set_gym_ac_upper_bound":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting gym AC upper bound temperature to " + tempInt);
                        coapNodesServer.setGymACUpperBound(tempInt);
                        System.out.println("Done!");
                        break;
                    /*GYM LIGHT*/
                    case "get_gym_lux":
                        tempInt = coapNodesServer.getGymLuxValue();
                        System.out.println("The gym light intensity (lux) is " + tempInt);
                        break;
                    case "set_gym_crepuscular_intermediate_level":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting  gym light intensity intermediate lower bound " + tempInt + " lux");
                        coapNodesServer.setGymCrepuscularIntermediateLevel(tempInt);
                        System.out.println("Done!");
                        break;
                    case "set_gym_crepuscular_max_level":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting gym light intensity max lower bound " + tempInt + " lux");
                        coapNodesServer.setGymCrepuscularMaxLevel(tempInt);
                        System.out.println("Done!");
                        break;
                    /*POOL PH*/
                    case "get_pool_ph_level":
                        tempFloat = coapNodesServer.getPoolPHLevel();
                        System.out.println("The pool ph level " + tempFloat);
                        break;
                    case "set_pool_ph_lower_bound":
                        tempFloat = Float.parseFloat(parts[1]);
                        System.out.println("Setting pool ph lower bound " + tempFloat);
                        coapNodesServer.setPoolPHLowerBound(tempFloat);
                        System.out.println("Done!");
                        break;
                    case "set_pool_ph_normal_level":
                        tempFloat = Float.parseFloat(parts[1]);
                        System.out.println("Setting pool ph lower bound " + tempFloat);
                        coapNodesServer.setPoolPHNormalLevel(tempFloat);
                        System.out.println("Done!");
                        break;
                    /*POOL CHLORINE*/
                    case "get_pool_chlorine_level":
                        tempFloat = mqttHandler.getPoolChlorineLevel();
                        System.out.println("The pool chlorine level " + tempFloat);
                        break;
                    case "set_pool_chlorine_min_ppm":
                        tempFloat = Float.parseFloat(parts[1]);
                        System.out.println("Setting pool chlorine min ppm " + tempFloat);
                        mqttHandler.setPoolMinPPM(tempFloat);
                        System.out.println("Done!");
                        break;
                    case "set_pool_chlorine_max_ppm":
                        tempFloat = Float.parseFloat(parts[1]);
                        System.out.println("Setting pool chlorine max ppm " + tempFloat);
                        mqttHandler.setPoolMaxPPM(tempFloat);
                        System.out.println("Done!");
                        break;
                    /*STEAM BATH ACCESS*/
                    case "get_steam_bath_people_number":
                        tempInt = mqttHandler.getSteamBathNumberOfPeople();
                        System.out.println("Steam bath access: " + tempInt);
                        break;
                    case "set_steam_bath_intermediate_number":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting steam bath instemediate number lower bound " + tempInt);
                        mqttHandler.setSteamBathIntermediateNumber(tempInt);
                        System.out.println("Done!");
                        break;
                    case "set_steam_bath_max_number":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting steam bath max number lower bound " + tempInt);
                        mqttHandler.setSteamBathMaxNumber(tempInt);
                        System.out.println("Done!");
                        break;
                    /*STEAM BATH HUMIDITY*/
                    case "get_steam_bath_humidity_level":
                        tempInt = mqttHandler.getSteamBathHumidityLevel();
                        System.out.println("Steam bath humidity: " + tempInt + "%");
                        break;
                    case "set_steam_bath_humidity_min_percentage":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting steam bath humidity min %: " + tempInt);
                        mqttHandler.setSteamBathMinHumidity(tempInt);
                        System.out.println("Done!");
                        break;
                    case "set_steam_bath_humidity_max_percentage":
                        tempInt = Integer.parseInt(parts[1]);
                        System.out.println("Setting steam bath humidity max %: " + tempInt);
                        mqttHandler.setSteamBathMaxHumidity(tempInt);
                        System.out.println("Done!");
                        break;

                    default:
                        System.out.println("Invalid command\n");
                        break;
                }

            } catch (Exception e) {
                System.out.println("Something was wrong with your command, please retry");
            }
        }
    }

    private static void printUsage() {
        //
        String output = "SmartWellness\n" +
                "Instert a command:\n\n" +
                "--- GYM ---\n" +
                "get_gym_temperature                           <--- get current temperature in the gym \n" +
                "set_gym_ac_temperature [NUM]                  <--- set AC temperature in the gym \n" +
                "get_gym_ac_temperature                        <--- get AC normal level temperature in the gym \n" +
                "set_gym_ac_upper_bound                        <--- get AC upper bound in the gym\n" +
                "get_gym_lux                                   <--- get gym current lux \n" +
                "set_gym_crepuscular_intermediate_level [NUM]  <--- set gym intermediate level of lux for the crepuscular sensor\n" +
                "set_gym_crepuscular_max_level [NUM]           <--- get gym max level of lux for the crepuscular sensor\n\n" +
                "--- POOL ---\n" +
                "get_pool_ph_level                             <--- get current pool ph level \n" +
                "set_pool_ph_lower_bound [NUM]                 <--- set pool lower bound for the ph level\n" +
                "set_pool_ph_normal_level [NUM]                <--- set pool ph normal level\n" +
                "get_pool_chlorine_level                       <--- get current pool chlorine level \n" +
                "set_pool_chlorine_min_ppm [NUM]               <--- set pool minimum value of PPM for the chlorine sensor\n" +
                "set_pool_chlorine_max_ppm [NUM]               <--- set pool max value of PPM for the chlorine sensor\n\n" +
                "--- STEAM BATH ---\n" +
                "get_steam_bath_people_number                  <--- get current number of people in the steam bath\n" +
                "set_steam_bath_intermediate_number [NUM]      <--- set steam bath intermediate people level\n" +
                "set_steam_bath_max_number [NUM]               <--- set steam bath max number of people\n" +
                "get_steam_bath_humidity_level                 <--- get current humidity level in the steam bath\n" + 
                "set_steam_bath_humidity_min_percentage [NUM]  <--- set steam bath humidity min percentage\n" + 
                "set_steam_bath_humidity_max_percentage [NUM]  <--- set steam bath humidity max percentage\n";

        System.out.println(output);
    }
}
