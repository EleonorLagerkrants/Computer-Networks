/*
  UDPEchoServer.java
  A simple echo server with no error handling
*/

package assignment1.Problem2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPEchoServer {
    public static final int BUFSIZE= 1024;
    public static final int MYPORT= 7777;

    public static void main(String[] args) throws IOException {
	byte[] buf= new byte[BUFSIZE];
	if(checkPort(MYPORT) == false) {
		System.err.print("Invalid port number");
		System.exit(1);
	}
		
	/* Create socket */
	DatagramSocket socket = new DatagramSocket(null);

	/* Create local bind point */
	SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
	socket.bind(localBindPoint);
	while (true) {
	    /* Create datagram packet for receiving message */
	    DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

	    /* Receiving message */
	    socket.receive(receivePacket);

	    /* Create datagram packet for sending message */
	    DatagramPacket sendPacket =
		new DatagramPacket(receivePacket.getData(),
				   receivePacket.getLength(),
				   receivePacket.getAddress(),
				   receivePacket.getPort());

	    /* Send message*/
	    socket.send(sendPacket);
	    System.out.printf("UDP echo request from %s", receivePacket.getAddress().getHostAddress());
	    System.out.printf(" using port %d\n", receivePacket.getPort());
	}
    } 
    
    /* Method to check port number */
    public static boolean checkPort (int i) {
    	if(i >= 1024 && i <= 9999) {
    		return true;
    	}
    	return false;
    }
}