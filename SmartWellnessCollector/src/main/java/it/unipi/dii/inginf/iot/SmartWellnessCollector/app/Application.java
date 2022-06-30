package it.unipi.dii.inginf.iot.SmartWellnessCollector.app;

import java.net.SocketException;

import io.netty.channel.CoalescingBufferQueue;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.coap.CoAPRegistrationServer;

public class Application 
{
    public static void main( String[] args ) throws SocketException{
        CoAPRegistrationServer coapRegistrationServer = new CoAPRegistrationServer();
        coapRegistrationServer.start();
    }
}
