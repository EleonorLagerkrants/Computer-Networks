package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class TFTPServer {
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final String READDIR = "/home/eleonor/Desktop/read/";
	public static final String WRITEDIR = "/home/eleonor/Desktop/write/";
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;
	public static String mode;

	public static void main(String[] args) {
		if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		try {
			TFTPServer server= new TFTPServer();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	private void start() throws SocketException {
		byte[] buf= new byte[BUFSIZE];
		
		/* Create socket */
		DatagramSocket socket= new DatagramSocket(null);
		
		/* Create local bind point */
		SocketAddress localBindPoint= new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		while(true) {        /* Loop to handle various requests */
			final InetSocketAddress clientAddress = receiveFrom(socket, buf);
			if (clientAddress == null) /* If clientAddress is null, an error occurred in receiveFrom()*/
				continue;

			final StringBuffer requestedFile= new StringBuffer();
			final int reqtype = ParseRQ(buf, requestedFile);

			new Thread() {
				public void run() {
					try {
						DatagramSocket sendSocket = new DatagramSocket(0);
						sendSocket.connect(clientAddress);
						
						System.out.printf("%s request for %s from %s using port %d\n",
								(reqtype == OP_RRQ)?"Read":"Write", requestedFile.toString(),
								clientAddress.getHostName(), clientAddress.getPort()); 
						
						if (reqtype == OP_RRQ) {      /* read request */
							requestedFile.insert(0, READDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
						}
						else {                       /* write request */
							requestedFile.insert(0, WRITEDIR);
							HandleRQ(sendSocket,requestedFile.toString(),OP_WRQ);  
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
		
		try {
			socket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		InetSocketAddress client = new InetSocketAddress(receivePacket.getAddress(),receivePacket.getPort());
		
		return client;
	}

	private int ParseRQ(byte[] buf, StringBuffer requestedFile) {
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		short opcode = wrap.getShort();
		int readBytes = 0;
		for (int i = 2; i < buf.length; i++) {
			if (buf[i] == 0) {
				readBytes = i;
				break;
			}
		}
		
		String fileName = new String(buf, 2, readBytes-2);
		requestedFile.append(fileName);
		
		int startSearch = readBytes + 1; //Where to start reading the transfer mode
		
		for (int i = startSearch; i < buf.length; i++) {
			if (buf[i] == 0) { //Found end of transfer mode
				int endSearch = i-startSearch; //End of transfer mode at i - (where we started)
				String transferMode = new String(buf,startSearch,endSearch);
				mode = transferMode;
				if (transferMode.equalsIgnoreCase("octet")) {
					return opcode;
				} else {
					System.err.println("Mode was not octet");
					System.exit(0);
				}
			}
		}
		System.err.println("Could not find a transfer mode");
		System.exit(0);
		return 0;
	}

	private void HandleRQ(DatagramSocket sendSocket, String string, int reqType) {
		File f = new File(string);
		byte[] buf = new byte[BUFSIZE-4];
		int bufLength = 0;
		
		if (reqType == OP_RRQ) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				bufLength = fis.read(buf);
			} catch(FileNotFoundException e) {
				System.err.println("The requested file does not exist.");
				sendErrorPacket(sendSocket, 1); //Send file does not exist error
			} catch(IOException e){
				System.err.println("I/O Exception while reading buffer");
			}
			
			short packetsSent = 0; //Keep track on how many packets has been sent
			while (true) {
				
				DatagramPacket sendPacket = createDataPacket(packetsSent, buf, bufLength);
				if (WriteAndReadAck(sendSocket, sendPacket, packetsSent)) {
					packetsSent++;
					System.out.println("Sent packet " + packetsSent);
				} else {
					System.err.println("Something went wrong when sending a packet.");
					return;
				}
				
				try{
					if (bufLength < 512) {
						fis.close();
						break;
					}
				}catch (IOException e) {
					System.err.println("I/O Exception when closing file.");
				}
				
			}
			
		} else if (reqType == OP_WRQ) {
			if (f.exists()) {
				System.out.println("File already exists.");
				sendErrorPacket(sendSocket, 1);
				return;
			} else {
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(f);
				} catch (FileNotFoundException e) {
					System.err.println("FileNotFoundException when trying to create file.");
					return;
				}
				
			short packetsRecieved = 0;
			
			while (true) {
				DatagramPacket dataPacket = ReadAndWriteData(sendSocket, ackPacket(packetsRecieved++), packetsRecieved);
			
					byte[] recievedPacket = dataPacket.getData();
					int startRead = 4;
					int endRead = dataPacket.getLength()-4;
					try {
						fos.write(recievedPacket, startRead, endRead);
					} catch (IOException e) {
						System.err.println("I/O Exception when writing to file");;
					}
					if (dataPacket.getLength()-4 < 512) {
						try {
							sendSocket.send(ackPacket(packetsRecieved));
						} catch (IOException e1) {
							System.err.println("I/O Exception when sending ack packet");
						}
						try {
							fos.close();
						} catch (IOException e) {
							System.err.println("I/O Exception when closing file output stream");
						}
						break;
					}
				}
			}
		}
	}
	
	/* Recieves datapacket and sends ack packet */
	private DatagramPacket ReadAndWriteData(DatagramSocket sendSocket, DatagramPacket sendAck, short packetNr) {
		int retryCount = 0;
		byte[] receiveBuf = new byte[BUFSIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);

        while(true) {
            if (retryCount > 5) {
                System.err.println("Could not send packet in 5 tries.");
                return null;
            }
            try {
            	System.out.println("Sending ack for packet number: " + packetNr);
            	sendSocket.send(sendAck);
                sendSocket.setSoTimeout(5000);
                sendSocket.receive(receivePacket);
                
                short receivedPacketNr = getDataPacketNr(receivePacket);
                if (receivedPacketNr == packetNr) {
                	return receivePacket;
                }
            } catch (SocketTimeoutException e) {
	            System.out.println("SocketTimeOutException when sending packet");
	        } catch (IOException e) {
				System.err.println("I/O Exception when sending packet");
			}
        }
	}
	
	/* Send Datapacket and receives ack packet */
	private boolean WriteAndReadAck(DatagramSocket sendSocket, DatagramPacket sender, short packetsSent) {
		int retryCount = 0;
		byte[] rec = new byte[BUFSIZE];
		short packetNumber = (short)(packetsSent + 1);
		DatagramPacket receiver = new DatagramPacket(rec, rec.length);
		
		while(true) {
			if (retryCount > 5) {
	            System.err.println("No ack packets recieved after 5 attempts.");
	            return false;
	        }
	        try {
	            sendSocket.send(sender);
	            System.out.println("Sent packet nr: " + packetNumber);
	            sendSocket.setSoTimeout(5000); // Set timeout to 5 seconds
	            sendSocket.receive(receiver);
	            
	            short ack = getAckPacketNr(receiver);
	            if (ack == packetNumber){ //This packet has received ack
	            	return true;
	            } else if (ack == -1){ //Error occured
	            	return false;
	            } else {
	            	retryCount = 0;
	            }
	        } catch (SocketTimeoutException e) {
	            System.out.println("SocketTimeoutException when sending packet");
	        } catch (IOException e) {
				System.err.println("I/O Exception when sending packet");
			}
		}
	}
	
	/* Create acknowledgment package with correct packetNr */
	private DatagramPacket ackPacket(short packetNr) {
		
		ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.putShort((short)4); //4 is the OPcode for acknowledgment packets
        buffer.putShort(packetNr);
		DatagramPacket ack = new DatagramPacket(buffer.array(), 4);
        return ack;
	}
	
	/* Create dataPacket*/
	private DatagramPacket createDataPacket(short packetNr, byte[] data, int length) {
		
		ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.putShort((short)OP_DAT); //OPcode for data is 3
        buffer.putShort(packetNr); //Which packet number this is
        buffer.put(data, 0, length);
		
        return new DatagramPacket(buffer.array(), 4+length);
	}
		
	private short getAckPacketNr(DatagramPacket ack) {
		ByteBuffer buffer = ByteBuffer.wrap(ack.getData());
		short opcode = buffer.getShort();
		if (opcode == OP_ERR) {
			return -1;
		}
		
		return buffer.getShort();
	}
	
	private short getDataPacketNr(DatagramPacket data) {
		ByteBuffer buffer = ByteBuffer.wrap(data.getData());
		short opcode = buffer.getShort();
		if (opcode == OP_ERR) {
			return -1;
		}
		
		return buffer.getShort();
	}
	
	/* Send error packet */
	public void sendErrorPacket(DatagramSocket sendSocket, int errorCode){
		ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
		short opCode = 5; //Error packets have OpCode 5
		buf.putShort(opCode); //Put 5 as opCode since that is the code for errors
		buf.putShort((short)errorCode); //Put the errorcode
		
		switch(errorCode){ //Put the correct errormessage
		case 1:
			buf.put("File not found".getBytes());
			break;
		case 2:
			buf.put("Access Violation".getBytes());
			break;
		case 3:
			buf.put("Disk full or allocation exceeded".getBytes());
			break;
		case 4:
			buf.put("Illegal TFTP operation".getBytes());
			break;
		case 5:
			buf.put("Unknown transfer ID".getBytes());
			break;
		case 6:
			buf.put("File allready exists".getBytes());
			break;
		case 7:
			buf.put("No such user".getBytes());
			break;
		}
		
		DatagramPacket errorPacket = new DatagramPacket(buf.array(),buf.array().length);
		try {
			sendSocket.send(errorPacket);
		} catch (IOException e) {
			System.err.println("I/O Exception when sending error packet.");
		}
	}
		
}