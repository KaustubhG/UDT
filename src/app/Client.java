package app;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import packet.DataPacket;
import udt.UDPEndpoint;
import udt.UDTSocket;

public class Client {
	//private static final Logger logger = Logger.getLogger(Client.class.getName());
	public static void main(String[] args) throws Exception {

		
		
		UDPEndpoint udp_endpoint = new UDPEndpoint(5000);
		UDTSocket socket = udp_endpoint.createUDTSocket(UDTSocket.Role.CLIENT, InetAddress.getByName("127.0.0.1"), 4000);
		
		long t1 = System.currentTimeMillis();
		sendFileReq("hello.txt", socket);
		receiveFile("output.txt", socket);
		//send shutdown packet
		socket.close();
		System.out.println("File Received, Time Taken: " + (System.currentTimeMillis()-t1));
		
		
	}

	public static void sendFileReq(String pathname, UDTSocket socket) {
		// send file request data packet
		socket.wrapAndStoreData(pathname.getBytes(), DataPacket.Position.ONLY);
	}

	public static void receiveFile(String pathname, UDTSocket socket) throws Exception {

		// poll the receiver's buffer and create file from data packets
		File ofile = new File(pathname);
		FileOutputStream fos;

		fos = new FileOutputStream(ofile, true);

		ArrayList<DataPacket> received_packets;
		while (true) {
			received_packets = socket.getAndFlushReceiversBuffer();
			if (received_packets.isEmpty()) {
				Thread.sleep(10);
				continue;
			}
			for (DataPacket packet : received_packets) {
				byte[] data = packet.getData();
				fos.write(data);
				fos.flush();
			}
			if (received_packets.get(received_packets.size() - 1).getPosition() == DataPacket.Position.LAST
					|| received_packets.get(received_packets.size() - 1).getPosition() == DataPacket.Position.ONLY)
				break;
			Thread.sleep(10);
		}

		fos.close();
		fos = null;

	}

}
