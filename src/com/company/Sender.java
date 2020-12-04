package com.company;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Sender {

    static final int PACKET_HEADER_SIZE = 2;
    static final int PACKET_DATA_SIZE = 1022;
    static final int PACKET_SIZE = 1024;


    // working with 127.0.0.1
    // inputs : image path , 220, 10, 50
    // can change from configurations
    public static void main(String[] args) {

        String ip = "127.0.0.1";
        // getting the arguments from the command line
        String image_path = args[0];
        int port = Integer.parseInt(args[1]);
        int window_size = Integer.parseInt(args[2]);
        int timeout = Integer.parseInt(args[3]);

        // getting the image file
        File raw_image;

        // data will be stored in this byte array
        byte[] data_in_byte = new byte[PACKET_DATA_SIZE];

        // this will be 1024 byte while getting the data each time
        ByteBuffer packet_buffer;

        // increase by one each packet sent
        int next_seq_number = 1 ;
        // beginning of the window
        int send_base = 1;
        // total number of packets to send
        int file_size = 0;
        int no_of_packet = 0;


        DatagramSocket clientSocket;

        try {
            // creating client socket
            clientSocket = new DatagramSocket();

            // getting image file as FileInputStream & finding the total number of packet will sent
            raw_image = new File(image_path);
            FileInputStream file_in_str = new FileInputStream(raw_image);
            file_size = file_in_str.available();
            no_of_packet = file_size / PACKET_DATA_SIZE ;
            // System.out.println(no_of_packet); // 4175

            // creating the packet which will be sent in threads
            packet_buffer = ByteBuffer.allocate(PACKET_SIZE);
            byte[] packet_header = new byte[PACKET_HEADER_SIZE];
            packet_header[0] = (byte) next_seq_number;
            packet_header[1] = (byte) (next_seq_number >>> 8);

            // adding the data to the buffer in big endian form ???
            packet_buffer.put(packet_header[1]);
            packet_buffer.put(packet_header[0]);
            packet_buffer.put(data_in_byte);



            // increasing sequence number
            next_seq_number++;

            // at the end of image
            byte zero_byte = 0;
            byte[] end_of_file = {zero_byte, zero_byte};
            clientSocket.send(new DatagramPacket(end_of_file,2, InetAddress.getByName(ip), port));

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }





    }
}
