package it.unipi.dii.inginf.iot.SmartWellnessCollector.app;

import java.net.SocketException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.netty.channel.CoalescingBufferQueue;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.CoapNodesServer;
/*
public class Application {
    public static void main( String[] args ) throws Exception {
        CoapNodesServer coapNodesServer = new CoapNodesServer();
        coapNodesServer.start();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        String[] parts;
        int temp;
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
                    case "get_gym_temperature":
                        temp = coapNodesServer.getGymTemperature();
                        System.out.println("Gym temperature is " + temp);
                        break;

                    case "set_gym_ac_temperature":
                        temp = Integer.parseInt(parts[1]);
                        System.out.println("Setting gym AC temperature to " + temp);
                        coapNodesServer.setGymACTemperature(temp);
                        System.out.println("done!");
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
        String output = "\nSmartWellness Collector commands: \n\n" +
                "AMBIENT: GYM \n" +
                "get_gym_temperature            <--- get current T in the gym \n" +
                "set_gym_ac_temperature [INT]   <--- set AC T in the gym \n" +
                "set_gym_ac [ON/OFF]            <--- force AC ON/OFF in the gym \n" +
                "get_gym_lux                    <--- get gym current lux \n" +
                "set_gym_min_lux [INT]          <--- set gym min lux \n\n" +

                "AMBIENT: POOL\n" +
                "get_pool_ph                    <--- get current pool ph level \n" +
                "set_pool_buffer [ON/OFF]       <--- set pool buffer regulator ON/OFF \n" +
                "get_pool_chlorine              <--- get current chlorine level in the gym \n" +
                "set_pool_ch_regulator [ON/OFF] <--- regulate chlorine level in the gym \n\n" +

                "AMBIENT: STEAM BATH\n" +
                "get_sb_temperature             <--- get temperature in steam bath\n" +
                "get_sb_temperature [INT]       <--- set temperature in steam bath\n" +
                "get_sb_presence                <--- get num of people in steam bath\n" +
                "set_light [R/G/Y]              <--- set light outside of steam bath \n";

        System.out.println(output);
    }


 */


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}
