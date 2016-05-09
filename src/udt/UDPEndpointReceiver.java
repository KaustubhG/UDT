package udt;

import java.net.DatagramPacket;
import java.util.logging.Logger;

public class UDPEndpointReceiver extends Thread {

	private static final Logger logger = Logger.getLogger(UDPEndpointReceiver.class.getName());
	private UDPEndpoint udp_endpoint;

	public UDPEndpointReceiver(UDPEndpoint udp_endpoint) {
		this.udp_endpoint = udp_endpoint;
	}

	@Override
	public void run() {
		logger.info("UDPEndpointReceiver started");
		while (true) {
			try {
				byte[] buffer = new byte[udp_endpoint.DATAGRAM_SIZE];
				DatagramPacket udp_packet = new DatagramPacket(buffer, buffer.length);
				udp_endpoint.getUDPSocket().receive(udp_packet);
				udp_endpoint.handler_pool.execute(new UDPEndpointPacketHandler(udp_endpoint, udp_packet));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
