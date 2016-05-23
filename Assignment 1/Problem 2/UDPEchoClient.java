/*
  UDPEchoClient.java
  A simple echo client with no error handling
*/

package assignment1.Problem2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Pattern;

public class UDPEchoClient {
    public static final int BUFSIZE = 64; 
    public static int MYPORT = 0;
    public static String MSG = "";
    public static int RATE;
    public static int MSG_SIZE;
    public static String[] array;
    public static String completeMessage = "";
    public static DatagramPacket[] sendPackets;
    public static DatagramSocket socket;
    public static DatagramPacket receivePacket;
    public static byte[] buf;
    

    public static void main(String[] args) throws IOException {
    	
    	/* 
    	 * First the program calls methods to check the validate the input parameters
    	 * and if they are not correct the program stops.
    	 */
    	if(checkArgsLength(args) == false) {
    		System.err.println("No valid arguments");
    		System.exit(1);
    	}	
    	buf= new byte[BUFSIZE];
    	array = copyArgs(args);
    	
    	if(checkIP(array) == false || checkPort(array) == false || checkMsgSize(array) == false || checkMsgRate(array) == false) {
    		System.err.printf("Invalid input parameters");
    		System.exit(1);
    	}

    	if (args.length != 4) {
    		System.err.printf("usage: %s server_name port\n", args[1]);
    		System.exit(1);
    	}
    	
		/* Create socket */
		socket = new DatagramSocket(null);
		
		/* Create local endpoint using bind() */
		SocketAddress localBindPoint = new InetSocketAddress(0);
		socket.bind(localBindPoint);
		
		/* Create remote endpoint */
		SocketAddress remoteBindPoint =
		    new InetSocketAddress(args[0],
					  Integer.valueOf(args[1]));
		
		/* 
		 * Creates the message and
		 * depending on Buffer and Message size the message is split into smaller messages that fit the buffer.
		 */
		adjustMSG(MSG_SIZE);
		String[] messages = messages(MSG, BUFSIZE);
		int parts = messages.length;
		sendPackets = new DatagramPacket[parts];
		
		/* Creates packets for the messages that are to be sent */
		for(int i = 0; i < parts; i++) {
			String message = messages[i];
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), remoteBindPoint); 
			sendPackets[i] = sendPacket;
		}
		
		/* Create datagram packet for receiving echoed message */
		receivePacket = new DatagramPacket(buf, buf.length);
		
		/* 
		 * Depending on Rate, the method of sending and receiving packets are run
		 * and after all the packets are sent the socket is closed.
		 */ 
		if(RATE == 0)
			RATE = 1;
		for(int i = 0; i < RATE; i++)  
			run();
		socket.close();
		}
    
    /*
     *  Method that sends and receives packets 
     * The thread sleeps in between packets according to rate
     */
    public static void run() throws IOException {
    		int sleep = 1000/RATE;
			for(DatagramPacket packet : sendPackets) {
					try {
						socket.send(packet);
						System.out.println("packet sent, " + packet.getLength() + " bytes sent");
						socket.receive(receivePacket);
						String msg = new String(buf, 0, receivePacket.getLength());
						completeMessage = completeMessage + msg;
						System.out.println("packet received " + receivePacket.getAddress().getHostName() + ": "
								+ msg);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
			}
	
		if (completeMessage.compareTo(MSG) == 0) {
			System.out.println("Success!");
			System.out.println("Message recieved: " + completeMessage);
			System.out.printf("%d bytes sent and received\n", completeMessage.length());
		}
		else {
			System.out.println("Failure!");
			System.out.printf("Sent and received msg not equal!\n");
			System.out.println("Message sent: " + MSG.length() + " bytes");
			System.out.println("Message recieved: " + completeMessage.length() + " bytes");
		}
		
		try {
			completeMessage = "";
			Thread.sleep(sleep);
			System.out.println("Slept for: " + sleep);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }	
    
    /* Method that checks that the argument length is 4 */
    public static boolean checkArgsLength  (String [] args) {
    	if(args.length < 4) 
    		return false;
    	return true;
    }
    
    /* Methods that copies the argument array into another array */
    public static String[] copyArgs (String[] args) {
		String[] array = new String[4];
		for(int i = 0; i < 4; i++ ) {
			array[i] = args[i];
		}
		return array;
    }
    
    /* Method that checks if the IP address is valid */
    public static boolean checkIP (String[] array) {
    	String ip = array[0];
    	String IPV4_valid = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
    	Pattern IPV4_pattern = Pattern.compile(IPV4_valid);
    	
    	if(ip.equals("localhost") || IPV4_pattern.matcher(ip).matches()) {
    		return true;
    	}
		return false;
    }
    
    /* Method that checks if the port is valid */
    public static boolean checkPort (String[] array) {
    	String port = array[1];
    	if(isInteger(port) == false) 
    		return false;
    	int port_number = Integer.parseInt(port);
    	if(port_number >= 1024 && port_number <= 9999) {
    		MYPORT = port_number;
    		return true;
    	}
    	return false;
    }

    /* Method that checks if the msg size is valid */
    public static boolean checkMsgSize (String[] array) {
    	String size = array[2];
    	if(isInteger(size) == false) 
    		return false;
    	int msg_size = Integer.parseInt(size);
    	if(msg_size > 0) {
    		MSG_SIZE = msg_size;
    		return true;
    	}
    	return false;
    }
    
    /* Method that checks if Message Rate is valid */
    public static boolean checkMsgRate (String[] array) {
    	String rate = array[3];
    	if(isInteger(rate) == false)
    		return false;
    	int msg_rate = Integer.parseInt(rate);
    	RATE = msg_rate;
    	if(msg_rate >= 0) 
    		return true;
    	return false;
    }
    
    /* Method that creates a message that creates a string with a's with the length of message size */
    public static void adjustMSG (int MSG_SIZE) {
    	for (int i = 0; i < MSG_SIZE; i++) {
    		MSG = MSG + "a";
    	}
    }
    
    /* Method that creates an array with the parts of the original message */
    public static String[] messages(String msg, int len) {
	    String[] result = new String[(int)Math.ceil((double)msg.length()/(double)len)];
	    for (int i = 0; i < result.length; i++)
	        result[i] = msg.substring(i*len, Math.min(msg.length(), (i+1)*len));
	    return result;
	}
    
    /* Method to check if a string is an Integer */
    public static boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        }
        return true;
    }
  
}