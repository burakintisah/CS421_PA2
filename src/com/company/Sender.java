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

    static String image_path = "";
    static int port;
    static int window_size;
    static int timeout; // maybe we can make it long


    static boolean done = false; // indicate we have sent all packets

    // increase by one each packet sent
    static int next_seq_number ;

    // beginning of the window
    static int send_base;

    // total number of packets to send
    static FileInputStream file_in_str = null;
    static int file_size = 0;
    static int no_of_packet = 0;
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

        // getting the image file
        File raw_image;

        // data will be stored in this byte array
        byte[] data_in_byte = new byte[PACKET_DATA_SIZE];

        // this will be 1024 byte while getting the data each time
        ByteBuffer packet_buffer;

        DatagramSocket client_socket;

        try {
            // creating client socket
            client_socket = new DatagramSocket();

            // getting image file as FileInputStream & finding the total number of packet will sent
            raw_image = new File(image_path);
            FileInputStream file_in_str = new FileInputStream(raw_image);
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

    public class DataSender extends Thread {

        private DatagramSocket client_socket;
        //private DatagramPacket packetOut;
        //private byte[] dataRead;
        //private byte[] dataSeq;

        public DataSender (DatagramSocket client_socket){
            this.client_socket = client_socket;

        }

        public void run() {


            try {
                while(true) {
                    // Send packets in window
                    for packet in packets:
                    client_socket.send(packet);


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

    public class ACKListener extends Thread {
        private DatagramSocket client_socket;
        private DatagramPacket ack_packet;
        //private Timeout mytimeout;
        private byte[] ack_data;

        public ACKListener (DatagramSocket client_socket){
            this.client_socket = client_socket;

        }

        public int findACKseq (byte [] ack_data) {


        }
        public void run() {
            System.out.println("In ACKListener");
            ack_data = new byte[2];
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

