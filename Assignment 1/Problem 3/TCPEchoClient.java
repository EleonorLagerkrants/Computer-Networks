package assignment1.Problem3;

import java.io.*;
import java.net.*;
import java.util.regex.Pattern;


public class TCPEchoClient {
	
	public static String IP;
	public static int MYPORT = 0;
    public static String MSG = "";
    public static int RATE;
    public static int MSG_SIZE;
    public static String[] array;
    
    /* 
	 * First the program calls methods to check the validate the input parameters
	 * and if they are not correct the program stops.
	 */
	public static void main(String args[]) throws Exception
	 {
		if(checkArgsLength(args) == false) {
    		System.err.println("No valid arguments");
    		System.exit(1);
    	}
		 array = copyArgs(args);
	    	
		 if(checkIP(array) == false || checkPort(array) == false || checkMsgSize(array) == false || checkMsgRate(array) == false) {
	    	System.err.printf("Invalid input parameters");
	    	System.exit(1);
	    }
		 
		 /* Creates a message */
		  adjustMSG(MSG_SIZE);
		  
		  /*
		   * Depending on message rate the socket reads the incoming stream for messages and sends replies.
		   * The thread sleeps in between messages according to message rate.
		   */
		  try {
			 
			  if(RATE == 0)
					RATE = 1;
			  int sleep = 1000/RATE;
			  for(int i = 0; i < RATE; i++) {
				  Socket clientSocket = new Socket(IP, MYPORT);
				  PrintWriter sendStream = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				  BufferedReader inFromServer = 
			              new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				  sendStream.println(MSG);
				  System.out.println("Sent message: " + MSG);
				  sendStream.flush();
			  
				  String receivedSentence;
				  while ((receivedSentence = inFromServer.readLine()) != null) {
					  System.out.println("FROM SERVER: " + receivedSentence);
				  }
				  
				  try{
					  Thread.sleep(sleep);
					  System.out.println("Slept for: " + sleep);
				  } catch (InterruptedException e) {
					e.printStackTrace();
				  }  
				  
				  inFromServer.close();
				  sendStream.close();
			}
		  } catch (Exception e) {
		  		e.printStackTrace();
		  }
			
		  /* Thread stops and exits */
			System.out.println("Thread stopped");
			System.exit(0);
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
	    		IP = ip;
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
