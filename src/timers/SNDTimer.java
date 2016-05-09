package timers;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Logger;

import packet.UDTPacket;
import udt.UDTSocket;

public class SNDTimer extends Thread {

	private static final Logger logger = Logger.getLogger(SNDTimer.class.getName());
	private UDTSocket udt_socket;

	public SNDTimer(UDTSocket socket) {
		udt_socket = socket;
	}

	@Override
	public void run() {
		logger.info("Starting SND timer");
		while (true) {
			if (udt_socket.getState() == UDTSocket.SocketState.CLOSED) {
				break;
			}
			// poll sender's LossList
			ArrayList<Long> sendersLossList = udt_socket.getSendersLossList();
			TreeMap<Long, UDTPacket> sendersBuffer = udt_socket.getSendersBuffer();
			UDTPacket packet = null;
			if (!sendersLossList.isEmpty()) {
				long lostPacketSeqNum = sendersLossList.remove(0);
				// Send only those packets which are not acked until now
				if (lostPacketSeqNum >= udt_socket.getLastAckedPacket()) {
					packet = udt_socket.getSendersBuffer().get(lostPacketSeqNum);
					try {
						udt_socket.send(packet);
						logger.info("Data packet sent from SenderLossList");
					} catch (Exception e) {
						throw new RuntimeException("Error sending Data packet from Senders Loss list");
					}
				}

			} else if (!sendersBuffer.isEmpty()
					&& udt_socket.getSequenceNumber() != udt_socket.getSendersBufferMarker()) {
				packet = sendersBuffer.get(udt_socket.getSendersBufferMarker());
				try {
					udt_socket.send(packet);
					logger.info("Data packet sent from SendersBuffer");
				} catch (Exception e) {
					throw new RuntimeException("Error sending DataPacket from SendersBuffer " + e.getMessage());
				}
				// update last packet sent sequence number
				udt_socket.incrementSendersBufferMarker();
			}
			// Sleep for 0.01 sec
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("Error in sleeping SND Thread");
			}
		}
		logger.info("SND Timer Closed");

	}

}
