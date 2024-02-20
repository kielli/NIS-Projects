import java.io.ByteArrayOutputStream;
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
    public static byte[] data_opcode = {0, 3};
    public static byte[] ack_opcode = {0, 4};
    public static byte[] error_opcode = {0, 5};



    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);


        while(true){
            System.out.println("Welcome!");
            System.out.print("> ");
            String user_input = sc.nextLine();
            String[] input = user_input.split(" ");

            if(input[0]!=null){
                if(input[0].equals("/connect") && input[1]!=null) {
                    System.out.println("Param : "+input[1]);
                    if (!isConnected) {
                        try {
                            server_ip = InetAddress.getByName(input[1]);
                            if (server_ip.isReachable(5000)) {
                                System.out.println("The IP address you inputted is not responding");
                            }

                            client_socket = new DatagramSocket(tftp_port);
                            isConnected = true;
                        } catch (Exception e) {
                            e.printStackTrace();
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

                        try{
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

                        while(true){
                            try {
                                DatagramPacket RRQ_Response = new DatagramPacket(buffer, buffer.length);
                                client_socket.receive(RRQ_Response);

                                byte[] received_packet = RRQ_Response.getData();
                                byte[] rcvd_opcode = Arrays.copyOfRange(received_packet, 0, 2);

                                if(rcvd_opcode == error_opcode){
                                    int i;
                                    for(i=4; i<received_packet.length-1; i++){
                                        System.out.println(received_packet[i]);
                                    }
                                    break;
                                }

                                else if(rcvd_opcode == data_opcode && received_packet.length < 516){
                                    int i, j=0;
                                    FileOutputStream foutputstream = new FileOutputStream(input[1]);
                                    byte[] received_data = new byte[512];

                                    for(i=4; i<received_packet.length; i++, j++){
                                        received_data[j] = received_packet[i];
                                    }

                                    foutputstream.write(received_data);
                                    foutputstream.close();

                                    byte[] blk_num = {received_data[2], received_data[3]};
                                    len = ack_opcode.length + blk_num.length;
                                    ByteArrayOutputStream ack_output = new ByteArrayOutputStream(len);

                                    try{
                                        ack_output.write(ack_opcode);
                                        ack_output.write(blk_num);
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }

                                    DatagramPacket ack_packet = new DatagramPacket(ack_output.toByteArray(), ack_output.toByteArray().length);
                                    client_socket.send(ack_packet);

                                    System.out.println("File Transfer Done !");

                                    break;
                                }

                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        System.out.println("Please connect to the server first");
                    }
                }

                else if(input[0].equals("/write") && input[1]!=null){

                }
                else if(input[0].equals("/?")){
                    System.out.println("Help:\n"+
                                        "/? - Lists all usable commands\n"+
                                        "/connect [ip address] - Connects to an IP Address\n"+
                                        "/disconnect - Disconnect from an IP Address\n"+
                                        "/read [filename] - Reads a file from the server\n"+
                                        "/write [filename] - Writes a file to the server\n");
                }
            }
        }

    }

}
