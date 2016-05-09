package timers;

import java.util.logging.Logger;

import packet.ACK;
import udt.UDTSocket;

public class ACKTimer extends Thread {

	private static final Logger logger = Logger.getLogger(ACKTimer.class.getName());
	
	private UDTSocket udt_socket;
	private ACK ackPacket;

	public ACKTimer(UDTSocket socket) {
		udt_socket = socket;
		ackPacket = new ACK();
		ackPacket.setDestinationSocketID(socket.getDestinationSocketID());
		ackPacket.setMessageNumber(0);
	}

	@Override
	public void run() {
		logger.info("Ack Timer Started");
		while (true) {
			
			if(udt_socket.getState() == UDTSocket.SocketState.CLOSED){
				break;
			}
			
			ackPacket.setAckSequenceNumber(1);
			ackPacket.setAckNumber(udt_socket.getReceivingSequenceNumber());
			ackPacket.setTimeStamp(1);
			try {
				udt_socket.send(ackPacket);
				logger.info("Ack Packet sent");
				Thread.sleep(10);
			} catch (Exception e) {
				throw new RuntimeException("error sending ack");
			}
		}
		logger.info("Ack Timer Closed");
	}

}
