package assignment1.Problem3;

import java.io.*;
import java.net.*;

public class TCPEchoServer {
	
	public static int PORT = 4950;
	static boolean ServerOn = true;

	public static void main(String args[]) throws Exception  {
         String clientMessage;
         String responseMessage = "";
         ServerSocket serverSocket = new ServerSocket(PORT);
        

         while(ServerOn)
         {
            Socket connectionSocket = serverSocket.accept();
            ClientServerThread clientThread = new ClientServerThread(connectionSocket);
            clientThread.start();
         }
      }
}

	class ClientServerThread extends Thread {
		Socket ClientSocket;
		boolean	runThread = true;
		
		public ClientServerThread() {
			super();
		}
		ClientServerThread(Socket s) {
			ClientSocket = s;
		}
		
		public void run() {
			
			BufferedReader inFromClient = null;
			PrintWriter sendStream = null;
			System.out.println("Accepted Client Address: " + ClientSocket.getInetAddress().getHostName());
			
			try {
				
				
				while(runThread) {
					inFromClient =  new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
					sendStream = new PrintWriter(new OutputStreamWriter(ClientSocket.getOutputStream()));	 
					String clientMessage = inFromClient.readLine();
					System.out.println("Received message: " + clientMessage);
					String responseMessage = ("Received message: " + clientMessage);
					sendStream.println(responseMessage);
					sendStream.flush();
					runThread = false;
					}

				inFromClient.close();
				sendStream.close();
				ClientSocket.close();
				System.out.println("Thread Stopped");
					
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}