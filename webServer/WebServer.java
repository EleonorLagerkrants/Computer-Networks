package webServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
	public static int PORT = 8888; //Port the server operates on
	static boolean ServerOn = true;
	private static final String sharedResource = "RESOURCEPATHEHERE";
	
	public static void main(String args[]) throws Exception{
        ServerSocket serverSocket = new ServerSocket(PORT);
        
        while(ServerOn){
           Socket connectionSocket = serverSocket.accept();
           ClientServerThread clientThread = new ClientServerThread(connectionSocket, sharedResource);
           clientThread.start();
        }
     }
}

class ClientServerThread extends Thread {
	Socket ClientSocket;
	boolean	runThread = true;
	BufferedReader inFromClient;
	DataOutputStream sendStream;
	String resourcePath;
		
	public ClientServerThread() {
		super();
	}
	ClientServerThread(Socket s, String resourcePath) {
		ClientSocket = s;
		this.resourcePath = resourcePath;
	}
		
	public void run() {
			
		
		System.out.println("Accepted Client Address: " + ClientSocket.getInetAddress().getHostName());
			
		try {
			
			inFromClient = null;
			sendStream = null;
			
			while(runThread) {
				inFromClient =  new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
				sendStream = new DataOutputStream(ClientSocket.getOutputStream());	 
				String clientMessage = inFromClient.readLine();
				if(clientMessage != null) {
					if(clientMessage.startsWith("GET")){ //GET-requests returns both the HEAD and BODY
						if(HEAD(clientMessage))
							BODY(clientMessage);
					}
					else if(clientMessage.startsWith("HEAD")){ //HEAD-requests will only return the HEAD
						HEAD(clientMessage);
					}
					else {
						buildHEAD(400, ""); //Only HEAD and GET is implemented so all other requests recieve BAD REQUEST
					}		
				}
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
	
	/* Build the BODY response and send it */
	public void BODY(String s) {
		String[] parts = s.split(" "); //Split the string where there is a blankspace
		String resource = parts[1]; //Our resource is the second string in the parts array
		
		while(resource.startsWith("/")){
			resource = resource.substring(1); //Remove all slashes in front of the resource
		}
		
		String filePath = resourcePath + resource;
		
		if(new File(filePath).isDirectory()){ //If the path is a directory, see if there is an index.html file in that directory
			if(new File(filePath + "\\index.html").exists())
				filePath = filePath + "\\index.html";
			else if(new File(filePath + "\\index.htm").exists())
				filePath = filePath + "\\index.htm";
		}
		
		File f = new File(filePath);
		FileInputStream fis;
		String response = "";
		
		try {
			fis = new FileInputStream(f);
			int i;
			try {
				while((i = fis.read()) != -1) //continue reading until end of the stream
					response += (char)i;      //append each character to the response message
				sendStream.writeBytes(response);
			} catch (IOException e) {
				System.out.println("Bad request!");
			}
		} catch (FileNotFoundException e) {
			System.out.println("File was not found!");
		}
	}
	
	/* Decide which HEAD response should be sent. Returns true if BODY should also be sent */
	public boolean HEAD(String s) {
		String[] parts = s.split(" ");
		String resource = parts[1];
		
		while(resource.startsWith("/")){
			resource = resource.substring(1);
		}
		String filePath = resourcePath + "\\" + resource;
		
		if(new File(filePath).isDirectory()){ //If the path is a directory, try to find a index.html file instead
			if(new File(filePath + "\\index.html").exists())
				filePath = filePath + "\\index.html";
			else if(new File(filePath + "\\index.htm").exists())
				filePath = filePath + "\\index.htm";
			else{
				buildHEAD(404, ""); //If no index file was found send an 404 not found page.
				return false;
			}
		}
		
		File f = new File(filePath);
		String suffix = getSuffix(filePath);
		System.out.println(filePath);
		
		if(f.exists()) {
			buildHEAD(200, suffix); //All is good, send a 200 OK with the correct suffix so content type is set correctly
			return true;
		}
		else {
			buildHEAD(404, suffix); //File was not sound, send a 404 File not found.
			return false;
		}
			
		
	}
	
	/* Gets the suffix of a filepath and returns it */
	public String getSuffix(String filePath) {
		String suffix = "";
		if(filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
			suffix = ".jpg";
		}
		if(filePath.endsWith(".gif")) 
			suffix = ".gif";
		
		if(filePath.endsWith(".png")) 
			suffix = ".png";
		
		if(filePath.endsWith(".txt")) 
			suffix = ".txt";
		
		if(filePath.endsWith(".htm") || filePath.endsWith(".html"))
			suffix = ".html";
		
		return suffix;
	}
	/* Build the HEAD response */
	public void buildHEAD(int code, String suffix) {
		String error = "";
		String content = "HTTP/1.1 ";
		
		/* Append the correct response code */
		if(code == 400) {
			content = content +="400 Bad request";
			error = "<html><body><h1>400 Bad request</h1></body></html>";
		}
		else if(code == 404) {
			content = content +="404 Not Found";
			error = "<html><body><h1>404 Not Found</h1></body></html>";
		}
		else if(code == 500) {
			content = content +="500 Internal Server Error";
			error = "<html><body><h1>500 Internal Server Error</h1></body></html>";
		}
		else if(code == 200)
			content = content +="200 OK";
		
		/* Append server info */
		content = content + "\r\n"; 
	    content = content + "Connection: close\r\n"; 
	    content = content + "Server: WebServer\r\n";
		
	    /* Append correct content type */
		if(suffix.equals(".jpg"))
			content = content +="Content-Type: image/jpeg\r\n";
		else if(suffix.equals(".gif"))
			content = content +="Content-Type: image/gif\r\n";
		else if(suffix.equals(".png"))
			content = content +="Content-Type: image/png\r\n";
		else if(suffix.equals(".txt"))
			content = content +="Content-Type: text/plain\r\n";
		else {
			content = content +="Content-Type: text/html\r\n";
		}
		content = content + "\r\n"; 
		content += error;
		System.out.println(content); //For debugging
		try {
			sendStream.writeBytes(content);
		} catch (IOException e1) {
			System.out.println("Could not send content to the Server.");
		}
	}
	
}