package udt;

import java.net.DatagramPacket;

import packet.ConnectionHandshake;
import packet.PacketFactory;
import packet.UDTPacket;

public class UDPEndpointPacketHandler extends Thread {

	private UDPEndpoint udp_endpoint;
	private DatagramPacket udp_packet;

	public UDPEndpointPacketHandler(UDPEndpoint udp_endpoint, DatagramPacket udp_packet) {
		this.udp_endpoint = udp_endpoint;
		this.udp_packet = udp_packet;
	}

	@Override
	public void run() {

		UDTPacket udt_packet = PacketFactory.createPacket(udp_packet);
		long destSocketID = udt_packet.getDestinationSocketID();
		if (destSocketID == 0) {
			// this is a handshake packet coming from a client, create a new socket
			ConnectionHandshake handshake = (ConnectionHandshake) udt_packet;
			if (!udp_endpoint.getDestSrcSocketIDMap().containsKey(handshake.getSocketID())) {

				try {
					UDTSocket udt_socket = udp_endpoint.createUDTSocket(UDTSocket.Role.SERVER, udp_packet.getAddress(),
							udp_packet.getPort());
					udt_socket.handler_pool.execute(new PacketHandler(udt_socket, udt_packet));

					udp_endpoint.getDestSrcSocketIDMap().put(handshake.getSocketID(), udt_socket.getSocketID());
				} catch (Exception e) {
					throw new RuntimeException("Error creating new Server UDTSocket");
				}
			} else {

				long destSocID = udp_endpoint.getDestSrcSocketIDMap().get(handshake.getSocketID());
				UDTSocket udt_socket = udp_endpoint.getUDTSocket(destSocID);
				if (udt_socket != null) {
					udt_socket.handler_pool.execute(new PacketHandler(udt_socket, udt_packet));
				}
			}
		} else {
			UDTSocket udt_socket = udp_endpoint.getUDTSocket(destSocketID);
			if (udt_socket != null) {
				if (!udt_socket.handler_pool.isTerminated()) {
					udt_socket.handler_pool.execute(new PacketHandler(udt_socket, udt_packet));
				}
			}
		}

	}

}
