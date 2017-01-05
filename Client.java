import java.io.*;
import java.net.*;
import java.util.*;

public class Client{
	private static HashMap<String,Integer> dataToServer = new HashMap<String,Integer>();
	private static int serverPort = 9876;
	private static int[] percent = {0, 25, 50, 75};
	private static String MESSAGE = "ABCD";
	private static byte[] sendData = new byte[1024]; 
    private static byte[] receiveData = new byte[1024];
	private static int synNum = 0;
	private static String data= "";
	private static int finBit = 0;
	private static DatagramSocket clientSocket;
	private static DatagramPacket receivePacket;
	private static DatagramPacket sendPacket;

    private static InetAddress IPAddress;

	public static void main(String args[]) throws Exception {
    	clientSocket = new DatagramSocket(1234);
    	IPAddress = InetAddress.getByName("127.0.0.1");

	//3 way handshake connection
	//send data to server 
	dataToServer.put("ACK",0);
	dataToServer.put("SYN",1);
	dataToServer.put("ISN",2000);

	System.out.println("\nTO SERVER: " + dataToServer);

	sendData = dataToServer.toString().getBytes();
	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
	clientSocket.send(sendPacket);

	//receive data from server
	receivePacket = new DatagramPacket(receiveData, receiveData.length);
	clientSocket.receive(receivePacket);
	String msgFromServer = new String(receivePacket.getData());

	System.out.println("\nFROM SERVER:" + msgFromServer);

	String[] dataReceived = msgFromServer.split(", ");
	dataToServer.clear();

	//update values of syn, ack, isn and ackno before sending to the server
	for(int i=0; i<dataReceived.length; i++){
		String[] tokens = dataReceived[i].split("=");

		for(int j=0; j<tokens.length; j++){
			tokens[j] = tokens[j].replaceAll("[^a-zA-Z0-9]", "");
		}

		if(tokens[0].equals("SYN")){
			tokens[1] = "0";
			dataToServer.put(tokens[0], Integer.parseInt(tokens[1]));
		}

		else if(tokens[0].equals("ISN")){
			dataToServer.put("ACKNO", Integer.parseInt(tokens[1])+1);
		}

		else if(tokens[0].equals("ACKNO")){
			dataToServer.put("ISN", Integer.parseInt(tokens[1]));
		}

		else{
			dataToServer.put(tokens[0], Integer.parseInt(tokens[1]));
		}
	}

	System.out.println("\nTO SERVER: " + dataToServer + "\n");

	//send the updated values of data to the server
	sendData = dataToServer.toString().getBytes();
	sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
	clientSocket.send(sendPacket);

	//the client is now connected to the server
	System.out.println("\nConnection with server has been established.");

	//send the message to the server per character
	Thread t = new Thread(new Runnable(){
		public void run(){
			finBit = 0;
			//perform this until the message is complete
			while(synNum != MESSAGE.length()){
				try{
					//if the current character to be sent to the server is the last character of the message
					//set finish bit to 1
					if(synNum == MESSAGE.length()-1){
						finBit = 1;
					}	

					//the data to be sent to the server contains a character of the message, sync number and finish bit
					data = "DATA - " +MESSAGE.charAt(synNum)+ " - SYNCNUM - " + synNum+ " - FINB - " + finBit; 
					sendData = data.getBytes();

					//performs packet dropping
					//the data won't be sent if packet dropped
					if(!drop()){
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
						clientSocket = new DatagramSocket();
						clientSocket.send(sendPacket);
						System.out.println("SYNC NUM: " +synNum+ " DATA: " +MESSAGE.charAt(synNum));
						synNum++;
					}

					else{
						System.out.println("Packet dropped.");
					}

					Thread.sleep(1000);

				}catch(Exception e){}
			}
		}
	});

	t.start();	

	clientSocket.close();

    } 

	public static boolean drop(){//randomizing 0%,25%,50%,75% in dropping packets
		Random rand = new Random();
		int perc = percent[rand.nextInt(4)];	//gets a random number from list of dropping probability
		int randNum = rand.nextInt(101);		//gets a random number from 0-100

		if(randNum<=perc){
			return true;
		}

		else{
			return false;
		}
	}

}