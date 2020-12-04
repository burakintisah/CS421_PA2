package com.company;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Sender {

    static final int PACKET_HEADER_SIZE = 2;
    static final int ACK_PACKET_SIZE = 2;
    static final int PACKET_DATA_SIZE = 1022;
    static final int PACKET_SIZE = 1024;
    static final String IP = "127.0.0.1";

    // command line arguments
    static String image_path = "";
    static int port;
    static int window_size;
    static int timeout; // maybe we can make it long


    static boolean done = false; // indicate we have sent all packets
    static int next_seq_number; // increase by one each packet sent
    static int send_base; // beginning of the window


    static File raw_image = null; // image from the path
    static FileInputStream file_in_str = null;
    static int file_size = 0; //image size
    static int no_of_packet = 0; // total number of packets to send


    // packets in the window to send the packets again if the dublicate ACK received
    static Vector <byte[]> window_packets;

    //Semaphore will be used to protect the shared variable next_seq_number and done variables
    static Semaphore lock = null;

    public Sender (DatagramSocket client_socket) {

        DataSender ds = new DataSender(client_socket);
        ACKListener ack = new ACKListener(client_socket);

        ds.start();
        ack.start();
    }

    // working with 127.0.0.1
    // inputs : image path , 220, 10, 50
    // can change from configurations
    public static void main(String[] args) {

        // getting the arguments from the command line
        image_path = args[0];
        port = Integer.parseInt(args[1]);
        window_size = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);


        try {
            // creating client socket
            DatagramSocket client_socket = new DatagramSocket();

            // getting image file as FileInputStream & finding the total number of packet will sent
            // getting the image file
            raw_image = new File(image_path);
            file_in_str = new FileInputStream(raw_image);
            file_size = file_in_str.available();
            no_of_packet = file_size / PACKET_DATA_SIZE ;
            // System.out.println(no_of_packet); // 4175

            window_packets = new Vector<byte[]>(window_size);
            next_seq_number = 1;
            send_base = 1;
            done = false;
            lock = new Semaphore(1);

            // this will start the DataSender && ACKListener -- check constructor of the Sender class
            new Sender(client_socket);


        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }


    }


    // First thread for sending data as described in the assignment
    public class DataSender extends Thread {

        DatagramSocket client_socket;
        byte[] packet_data;
        byte[] packet_seq;
        DatagramPacket packet_datagram;


        public DataSender (DatagramSocket _client_socket){
            client_socket = _client_socket;
            packet_data = null;
            packet_seq = null;
            packet_datagram = null;
        }

        public void run() {

            try {
                while(!done) {
                    // Send packets in window


                    // Wait for main thread notification or timeout
                    Thread.sleep(timeout);
                }
            }
            // Stop if main thread interrupts this thread
            catch (InterruptedException e) {
                return;
            }
        }

    }


    // Second thread for listening acknowledgements as described in the assignment
    public class ACKListener extends Thread {

        DatagramSocket client_socket;
        DatagramPacket ack_packet;
        byte[] ack_data;
        //Timeout t;


        public ACKListener (DatagramSocket _client_socket){
            client_socket = _client_socket;
            ack_packet = null;
            ack_data = null;
        }

        public int findACKseq (byte [] ack_data) {
            return 0;
        }

        public void run() {

            ack_data = new byte[ACK_PACKET_SIZE];
            ack_packet = new DatagramPacket(ack_data, ACK_PACKET_SIZE);

            while(!done) {
                try {
                    client_socket.receive(ack_packet);
                    // find which packet's ACK is received
                    int noOfACK = findACKseq(ack_data);
                    System.out.println("Received Ack " + noOfACK);



                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            //if transfer is completed, close the socket
            client_socket.close();
        }
    }
}

