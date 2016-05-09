package timers;

import java.util.ArrayList;
import java.util.logging.Logger;

import packet.NAK;
import udt.UDTSocket;

public class NAKTimer extends Thread {

	private UDTSocket udt_socket;
	private static final Logger logger = Logger.getLogger(NAKTimer.class.getName());

	public NAKTimer(UDTSocket socket) {
		udt_socket = socket;
	}

	@Override
	public void run() {
		logger.info("NAK Timer started");
		while (true) {
			
			if(udt_socket.getState() == UDTSocket.SocketState.CLOSED){
				break;
			}
			
			ArrayList<Long> lossList = udt_socket.getReceiversLossList();
			if (!lossList.isEmpty()) {
				NAK nak = new NAK();
				nak.setDestinationSocketID(udt_socket.getDestinationSocketID());
				nak.setMessageNumber(0);
				nak.setTimeStamp(1);
				nak.addLossInfo(lossList);
				try {
					udt_socket.send(nak);
					logger.info("NAK Packet sent");
				} catch (Exception e) {
					throw new RuntimeException("error sending NAK");
				}
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("Error sleeping NAK Thread");
			}
		}
		logger.info("NAK Timer Closed");

	}

}
