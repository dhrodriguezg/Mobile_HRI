package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by dhrodriguezg on 9/1/16.
 */
public class UDPComm {

    private DatagramSocket clientSocket;
    private InetAddress ipAddress;
    private String ip;
    private int port;
    private boolean enabled;

    public UDPComm(String ip, int port){
        this.ip=ip;
        this.port=port;
        this.enabled = false;
        createSocket();
    }

    public void sendData(byte[] data){
        if( clientSocket==null || clientSocket.isClosed() )
            createSocket();

        if(!enabled)
            return;

        try {
            enabled = false;
            DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
            clientSocket.send(packet);
            enabled = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSocket(){
        try {
            clientSocket = new DatagramSocket();
            ipAddress = InetAddress.getByName(ip);
            enabled = true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void destroy(){
        if( clientSocket!=null && !clientSocket.isClosed() )
            clientSocket.close();
    }

}
