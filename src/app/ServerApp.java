package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import packet.DataPacket;
import udt.UDTSocket;

public class ServerApp extends Thread {
	private static final Logger logger = Logger.getLogger(ServerApp.class.getName());

	private UDTSocket udt_socket;

	public ServerApp(UDTSocket socket) {
		udt_socket = socket;
	}

	@Override
	public void run() {

		while (udt_socket.getState() != UDTSocket.SocketState.ESTABLISHED) {
			// do nothing
		}
		handleFileTransfer(udt_socket);
	}

	public static void handleFileTransfer(UDTSocket socket) {
		ArrayList<DataPacket> received_packets;
		String file_path = null;
		while (true) {
			received_packets = socket.getAndFlushReceiversBuffer();
			if (!received_packets.isEmpty()) {
				// Thread.sleep(100);
				file_path = new String(received_packets.get(0).getData());
				// check if file exists
				File file = new File(file_path);
				if (file.length() > 0) {
					sendFile(file, socket);
					break;
				}
			}
		}
	}

	public static void sendFile(File file, UDTSocket socket) {
		logger.info("Sending file: " + file.getName());
		FileInputStream inputStream;
		DataPacket.Position position = DataPacket.Position.FIRST;
		int fileSize = (int) file.length();
		int read = 0;
		int readLength = socket.DATAGRAM_SIZE - 16;
		byte[] byteChunkPart;
		int flag = 0; // checking for file with size < datapacket size (essentially whole file is sent as one packet)
		try {
			inputStream = new FileInputStream(file);
			if (fileSize <= socket.DATAGRAM_SIZE - 16) {
				position = DataPacket.Position.ONLY;
				flag = 1;
			}
			while (fileSize > 0) {
				if (fileSize <= socket.DATAGRAM_SIZE - 16 && flag == 0) {
					readLength = fileSize;
					position = DataPacket.Position.LAST;
				}
				byteChunkPart = new byte[readLength];
				read = inputStream.read(byteChunkPart, 0, readLength);
				fileSize -= read;
				assert (read == byteChunkPart.length);
				// encapsulate data in data packet
				socket.wrapAndStoreData(byteChunkPart, position);
				position = DataPacket.Position.MIDDLE;
				byteChunkPart = null;
			}
			inputStream.close();
		} catch (IOException exception) {
			throw new RuntimeException(exception.getMessage());
		}
	}

}
