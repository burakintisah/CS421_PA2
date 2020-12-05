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
    static long timeout; // maybe we can make it long
    static Timer timer;


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
    static Semaphore timeLock = null;

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
        System.out.println(image_path);

        try {
            // creating client socket
            DatagramSocket client_socket = new DatagramSocket();

            // getting image file as FileInputStream & finding the total number of packet will sent
            // getting the image file
            raw_image = new File(image_path);
            file_in_str = new FileInputStream(raw_image);
            file_size = file_in_str.available();
            no_of_packet = (file_size / PACKET_DATA_SIZE ) + 1 ;
            System.out.println(no_of_packet); // 4176

            int len = window_size;
            window_packets = new Vector<byte[]>(len);
            next_seq_number = 1;
            send_base = 1;
            done = false;
            lock = new Semaphore(1);
            timeLock = new Semaphore(1);

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

        public DataSender (DatagramSocket _client_socket){
            client_socket = _client_socket;
        }

        public void run() {

            try {
                while(!done) {
                    // Send packets in window
                    if (next_seq_number < send_base + window_size) {
                        // acquire lock since we will be changing next seq number
                        lock.acquire();

                        if (send_base == next_seq_number) {
                            startTimer();
                        }

                        byte[] data_out = new byte[PACKET_SIZE];

                        // if we already sent this data package before, we take it from the window_packets
                        if (next_seq_number <= window_packets.size()) {
                            data_out = window_packets.get(next_seq_number - 1);
                        }else {
                            byte[] data_for_package = new byte[PACKET_DATA_SIZE];
                            int data_size = file_in_str.read(data_for_package);

                            if (data_size > 0) {

                                data_out[0] = (byte) ((next_seq_number >> 8) & 0xFF);
                                data_out[1] = (byte) (next_seq_number & 0xFF);

                                // we put data to the package
                                for (int i = 0; i < data_size; i++) {
                                    data_out[i + 2] = data_for_package[i];
                                }

                                // package is ready
                                window_packets.add(data_out);
                            }
                        }

                        //System.out.println(next_seq_number);
                        if (next_seq_number != no_of_packet + 1 ) {
                            DatagramPacket next_packet = new DatagramPacket(data_out, data_out.length, InetAddress.getByName(IP), port);
                            client_socket.send(next_packet);
                            next_seq_number++;
                        }

                        lock.release();

                    }
                    // Wait for main thread notification or timeout
                    Thread.sleep(10);
                }
                // at the end of image
                byte zero_byte = 0;
                byte[] end_of_file = {zero_byte, zero_byte};
                client_socket.send(new DatagramPacket(end_of_file,2, InetAddress.getByName(IP), port));
                file_in_str.close();
                System.exit(1);

            }
            // Stop if main thread interrupts this thread
            catch (SocketException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }catch (InterruptedException e) {
                return;
            }

        }

    }


    // Second thread for listening acknowledgements as described in the assignment
    public class ACKListener extends Thread {

        DatagramSocket client_socket;

        public ACKListener (DatagramSocket _client_socket){
            client_socket = _client_socket;
        }

        public void run() {
            byte[] ack_data = new byte[ACK_PACKET_SIZE];
            DatagramPacket ack_packet = new DatagramPacket(ack_data, ACK_PACKET_SIZE);

            while(!done) {
                try {
                    client_socket.receive(ack_packet);

                    // find which packet's ACK is received
                    int ACKno = ((ack_data[0] & 0xff) << 8) | (ack_data[1] & 0xff);
                    // System.out.println("ACK # " + ACKno);

                    // check is transfer done or not
                    if (ACKno == no_of_packet) {
                        done = true;
                    }

                    // check if we obtained a valid ack number or not
                    else if (send_base <= ACKno && ACKno < no_of_packet) {
                        send_base = ACKno + 1;
                        startTimer();
                    }

                    // check if we obtained a duplicate ACK from receiver
                    else if (ACKno == send_base - 1) {
                        lock.acquire();
                        //System.out.println("Dublicate ACK received");
                        next_seq_number = send_base;
                        lock.release();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    // when scheduled timeout occurs this task is implemented
    public class TimeoutTask extends TimerTask {
        public void run() {
            try{
                lock.acquire();
                //System.out.println("Time out occured");
                next_seq_number = send_base;
                lock.release();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    // we schedule timeout task here
    public void startTimer(){
        try {
            timeLock.acquire();
            //cancel previous schedule here
            if (timer != null)
                timer.cancel();
            timer = new Timer();
            TimeoutTask timeout_task = new TimeoutTask();
            timer.schedule( timeout_task, timeout );
            timeLock.release();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }

    }
}