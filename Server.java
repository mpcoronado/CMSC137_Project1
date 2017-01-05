import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Server{
	private static HashMap<String,Integer> dataToClient = new HashMap<String,Integer>();
    private static int clientPort = 1234; 
    private static byte[] receiveData = new byte[1024]; 
    private static byte[] sendData  = new byte[1024];
    private static String received = "";
	private static Thread t = new Thread();
	private static DatagramSocket serverSocket;

	private static InetAddress IPAddress;
	private static DatagramPacket receivePacket;
	private static DatagramPacket sendPacket;
	private static int synNum;
	private static String dataToClient2 = "";
	
	public static void main(String args[]) throws Exception {

        serverSocket = new DatagramSocket(9876);

        serverSocket.setSoTimeout(10000);     		//set timeout to 10 seconds   
        System.out.println("\nServer is running...\n");
        while(true){
	        try{
		        //receive data from client
		        receiveData = new byte[1024];
		        receivePacket = new DatagramPacket(receiveData, receiveData.length);
		        IPAddress = InetAddress.getByName("127.0.0.1");
		        serverSocket.receive(receivePacket);
		        String msgFromClient = new String(receivePacket.getData());

		        System.out.println("\nFROM CLIENT: " +msgFromClient);

		        //split data from client to get and update the values of ack, syn, isn and ackno 
		        String[] dataReceived = msgFromClient.split(", ");

		        for(int i=0; i<dataReceived.length; i++){
		            String[] tokens = dataReceived[i].split("=");

		            for(int j=0; j<tokens.length; j++){
		                tokens[j] = tokens[j].replaceAll("[^a-zA-Z0-9]", "");
		            }

		            if(tokens[0].equals("ACK")){
		                tokens[1] = "1";
		                dataToClient.put(tokens[0], Integer.parseInt(tokens[1]));
		            }

		            else if(tokens[0].equals("ISN")){
		                dataToClient.put("ACKNO", Integer.parseInt(tokens[1])+1);
		            }

		            else{
		                dataToClient.put(tokens[0], Integer.parseInt(tokens[1]));
		            }
		        }

		        dataToClient.put("ISN", 5000);

		        System.out.println("\nTO CLIENT: " + dataToClient);

		        //send the data back to client
		        sendData = dataToClient.toString().getBytes();
		        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
		        serverSocket.send(sendPacket); 

		        //receive data from client
		        receivePacket = new DatagramPacket(receiveData, receiveData.length);
		        serverSocket.receive(receivePacket);
		        msgFromClient = new String(receivePacket.getData());

		        System.out.println("\nFROM CLIENT: " +msgFromClient);

		        //the server is now connected with the client
		        System.out.println("\nConnection with client has been established.\n");


		        while(true){      

		            //receive message from the client
		            receiveData = new byte[1024];
		            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		            serverSocket.receive(receivePacket);
		            String fromClient = new String(receivePacket.getData());

		            System.out.println("FROM CLIENT: " +fromClient);

		            //split the message sent by the client to get the data sent, sync number and finish bit
		            //append the data sent to the data already sent by the client
		            String[] tokens = fromClient.split(" - ");
		            received += tokens[1];
		            synNum = Integer.parseInt(tokens[3]);
		            tokens[5] = tokens[5].replaceAll("[^a-zA-Z0-9]", "");

		            System.out.println("SYNC NUM " +synNum+ ": " +received +"\n");

		            //if finish bit equals 1, the loop will stop since the message sent by the client is already complete
		            if(Integer.parseInt(tokens[5]) == 1){
		                break;
		            }

		            //update value of sync number and send to the client
		            synNum++;
		            dataToClient2 = "SYNCNUM " +synNum;
		            sendData = dataToClient2.getBytes();
		            sendPacket = 
		            new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
		            serverSocket.send(sendPacket);

		        }
		        received = "";

		    }catch(Exception e){
		    	System.out.println("Server timed out.\n");
		    	break;
		    } 
		}
    }

}