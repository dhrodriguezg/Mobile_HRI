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
    private boolean enableTransmission;
    private boolean enableReception;

    private int receiveBuffer;
    private byte[] dataToReceive = null;
    private byte[] dataToSend = null;

    public UDPComm(String ip, int port){
        this.ip=ip;
        this.port=port;
        this.enableTransmission = false;
        this.enableReception = false;
        createSocket();
    }



    public void sendData(){
        if( dataToSend!=null && dataToSend.length > 0 )
            sendData(dataToSend);
    }

    public void setDatatoSend(byte[] data){
        dataToSend = data;
    }

    public void setReceiveDataBuffer(int buffer){
        receiveBuffer = buffer;
    }

    public byte[] getReceivedData(){
        return dataToReceive;
    }

    public void sendData(byte[] data){
        if( clientSocket==null )
            createSocket();

        if(!enableTransmission)
            return;

        try {
            enableTransmission = false;
            DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
            clientSocket.send(packet);
            enableTransmission = true;
        } catch (IOException e) {
        }
    }

    public void receiveData(){
        if( clientSocket==null )
            createSocket();

        if(!enableReception || receiveBuffer == 0 )
            return;

        try {
            enableReception = false;
            byte[] data = new byte[receiveBuffer];
            DatagramPacket newPacket =  new DatagramPacket(data, data.length);
            clientSocket.receive(newPacket);
            dataToReceive = data.clone();
            enableReception = true;
        } catch (IOException e) {
        }
    }

    private void createSocket(){
        try {
            clientSocket = new DatagramSocket();
            ipAddress = InetAddress.getByName(ip);
            enableTransmission = true;
            enableReception = true;
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
