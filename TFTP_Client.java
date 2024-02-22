import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class TFTP_Client {

    public static int tftp_port = 69;
    public static byte[] buffer;
    public static InetAddress server_ip;
    public static DatagramSocket client_socket;
    public static boolean isConnected = false;
    public static byte[] rrq_opcode = {0, 1};
    public static byte[] wrq_opcode = {0, 2};
    public static byte[] ack_opcode = {0, 4};
    public static final int MAX_RETRIES = 5;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String message_prompt = "> ";

        while(true){
            System.out.println("\nTeamfight Tactics Protocol (TFTP Client)\n");
            System.out.print(message_prompt);
            String user_input = sc.nextLine();
            String[] input = user_input.split(" ");

            if(input[0]!=null){
                if(input[0].equals("/connect") && input[1]!=null) {
                    if (!isConnected) {
                        try {
                            server_ip = InetAddress.getByName(input[1]);
                            System.out.println("Trying to connect to "+server_ip);
                            if (server_ip.isReachable(5000)) {
                                System.out.println("Connected to the IP Address!");
                                message_prompt = (server_ip + " > ");
                                client_socket = new DatagramSocket();
                                isConnected = true;
                            }
                            else {
                                System.out.println("Unable to connect to that address");
                            }


                        } catch (Exception e) {
                            System.out.println("Error : ");
                        }
                    } else {
                        System.out.println("Client is already connected to " + server_ip.getCanonicalHostName());
                    }
                }

                else if(input[0].equals("/disconnect")) {
                    if(isConnected) {
                        server_ip = null;
                        isConnected = false;
                        client_socket.close();
                        System.out.println("Successfully disconnected from the server");
                    }
                    else{
                        System.out.println("The client is not yet connected to a server !");
                    }

                }

                else if(input[0].equals("/read") && input[1]!=null){
                    if(isConnected){
                        int len = rrq_opcode.length+input[1].length()+"octet".length()+2;
                        ByteArrayOutputStream req_output = new ByteArrayOutputStream(len);
                        buffer = new byte[516];
                        int retries = 0;



                        try{
                            System.out.println("Fetching file...");
                            req_output.write(rrq_opcode);
                            req_output.write(input[1].getBytes());
                            req_output.write(0);
                            req_output.write("octet".getBytes());
                            req_output.write(0);

                            DatagramPacket RRQ = new DatagramPacket(req_output.toByteArray(), req_output.toByteArray().length, server_ip, tftp_port);
                            client_socket.send(RRQ);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        boolean recv_message=true;
                        while(true){
                            try {
                                DatagramPacket RRQ_Response = new DatagramPacket(buffer, buffer.length,server_ip,tftp_port);
                                client_socket.setSoTimeout(15000);
                                client_socket.receive(RRQ_Response);


                                byte[] received_packet = RRQ_Response.getData();
                                System.out.println(received_packet[1]);
                                int rcvd_opcode = ((received_packet[0] & 0xff) << 8) | (received_packet[1] & 0xff);

                                if(rcvd_opcode == 5){
                                    int i, j=0;

                                    for(i=4; i<received_packet.length; i++, j++){
                                        if(received_packet[i]!=0){
                                            System.out.println(received_packet[i]);
                                        }
                                        else{
                                            break;
                                        }
                                    }
                                    break;
                                }

                                if(rcvd_opcode == 3){
                                    if(recv_message){
                                        System.out.println("Transfering file...");
                                        recv_message=false;
                                    }

                                    ByteArrayOutputStream req_packets = new ByteArrayOutputStream(received_packet.length-4);
                                    req_packets.write(received_packet, 4, received_packet.length-4);

                                    if(received_packet.length < 516){
                                        FileOutputStream foutputstream = new FileOutputStream(input[1]);
                                        foutputstream.write(req_packets.toByteArray());
                                        foutputstream.close();

                                        byte[] blk_num = {received_packet[2],received_packet[3]};
                                        ByteArrayOutputStream ack_output = new ByteArrayOutputStream(ack_opcode.length+ blk_num.length);
                                        ack_output.write(ack_opcode);
                                        ack_output.write(blk_num);

                                        DatagramPacket ack_packet = new DatagramPacket(ack_output.toByteArray(), ack_output.toByteArray().length, server_ip, tftp_port);
                                        client_socket.send(ack_packet);

                                        System.out.println("File Transfer Done !");
                                    }

                                    byte[] blk_num = {received_packet[2],received_packet[3]};
                                    ByteArrayOutputStream ack_output = new ByteArrayOutputStream(ack_opcode.length+ blk_num.length);
                                    ack_output.write(ack_opcode);
                                    ack_output.write(blk_num);

                                    DatagramPacket ack_packet = new DatagramPacket(ack_output.toByteArray(), ack_output.toByteArray().length, server_ip, tftp_port);
                                    client_socket.send(ack_packet);
                                    break;
                                }


                            } catch (SocketTimeoutException e) {
                                if(retries!=MAX_RETRIES){
                                    System.out.println("Server timed out. Retrying...");
                                    retries++;
                                }
                                else {
                                    System.out.println("Reached Max Retries, closing socket");
                                    isConnected = false;
                                    client_socket.close();
                                    message_prompt = "> ";
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        System.out.println("Please connect to the server first");
                    }
                }

                else if(input[0].equals("/write") && input[1]!=null){
                    if(isConnected){
                        int len = wrq_opcode.length+input[1].length()+"octet".length()+2;
                        ByteArrayOutputStream req_output = new ByteArrayOutputStream(len);
                        buffer = new byte[512];

                        try{
                            req_output.write(wrq_opcode);
                            req_output.write(input[1].getBytes());
                            req_output.write(0);
                            req_output.write("octet".getBytes());
                            req_output.write(0);

                            DatagramPacket WRQ = new DatagramPacket(req_output.toByteArray(), req_output.toByteArray().length, server_ip, tftp_port);
                            client_socket.send(WRQ);

                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                        try{
                            FileInputStream file_reader = new FileInputStream(input[1]);
                            int file_len;
                            int blk_num = 1;

                            while((file_len = file_reader.read(buffer))!=-1){
                                ByteArrayOutputStream file_output = new ByteArrayOutputStream(file_len+4);
                                file_output.write(wrq_opcode);
                                file_output.write(blk_num++);
                                file_output.write(buffer, 0, file_len);

                                DatagramPacket file_packet = new DatagramPacket(file_output.toByteArray(), file_output.toByteArray().length, server_ip, tftp_port);
                                client_socket.send(file_packet);
                                System.out.println("Sending File...");

                                byte[] ack_data = new byte[4];
                                DatagramPacket ack_packet = new DatagramPacket(ack_data, ack_data.length, server_ip, tftp_port);
                                client_socket.receive(ack_packet);
                                System.out.println("File Sent!");

                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                else if(input[0].equals("/?")){
                    System.out.println("Help:\n"+
                                        "/? - Lists all usable commands\n"+
                                        "/connect [ip address] - Connects to an IP Address\n"+
                                        "/disconnect - Disconnect from an IP Address\n"+
                                        "/read [filename] - Reads a file from the server\n"+
                                        "/write [filename] - Writes a file to the server\n"+
                                        "/exit - Exit the program");
                }
                else if(input[0].equals("/exit")){
                    if(isConnected){
                        client_socket.close();
                    }
                    break;
                }
            }
        }
    }
}
