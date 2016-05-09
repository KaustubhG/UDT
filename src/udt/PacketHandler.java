package udt;

import java.util.ArrayList;
import java.util.logging.Logger;

import packet.ACK;
import packet.ConnectionHandshake;
import packet.ControlPacket;
import packet.DataPacket;
import packet.NAK;
import packet.UDTPacket;

public class PacketHandler implements Runnable {
	private static final Logger logger = Logger.getLogger(PacketHandler.class.getName());
	private UDTSocket udt_socket;
	private UDTPacket packet;

	public PacketHandler(UDTSocket socket, UDTPacket udt_packet) {
		this.udt_socket = socket;
		this.packet = udt_packet;
	}
	
	

	@Override
	public void run() {
		try {

			switch (udt_socket.getState()) {
			case LISTEN:
				handleListen();
				break;
			case ACTIVE_OPEN:
				handleActiveOpen();
				break;
			case ESTABLISHED:
				handleEstablished();
				break;
			case CLOSED:
				handleClosed();
				break;
			default:
				throw new RuntimeException("Invalid state");
			}
		} catch (Exception e) {
			throw new RuntimeException("Packet handler's run method giving error : " + e.getMessage());
		}

	}

	private void handleListen() throws Exception {
		// check packet
		if (packet.isControlPacket()) {
			ControlPacket c_packet = (ControlPacket) packet;
			if (c_packet.getControlPacketType() == ControlPacket.ControlPacketType.CONNECTION_HANDSHAKE.ordinal()) {

				ConnectionHandshake handshake = (ConnectionHandshake) c_packet;
				udt_socket.setDestinationSocketID(handshake.getSocketID());
				// initialize Receiving Sequence Number
				udt_socket.setReceivingSequenceNumber(handshake.getInitialSeqNo());
				//udt_socket.setDestination();

				// initialize ReceivingBuffer Marker
				udt_socket.setReceiverBufferMarker(handshake.getInitialSeqNo());

				// send response handshake
				ConnectionHandshake response_handshake = udt_socket
						.createHandshakePacket(ConnectionHandshake.CONNECTION_TYPE_SERVER);

				// calling socket send method directly
				udt_socket.send(response_handshake);
				// initialize SendersBufferMarker
				udt_socket.initializeSendersBufferMarker();
				logger.info("Handshake packet handled in Listen state");
				udt_socket.goToNextState();
			}
		}
	}

	private void handleActiveOpen() {
		if (packet.isControlPacket()) {
			ControlPacket c_packet = (ControlPacket) packet;
			if (c_packet.getControlPacketType() == ControlPacket.ControlPacketType.CONNECTION_HANDSHAKE.ordinal()) {
				ConnectionHandshake handshake = (ConnectionHandshake) c_packet;
				udt_socket.setDestinationSocketID(handshake.getSocketID());
				// initialize ReceivingSequenceNumber
				udt_socket.setReceivingSequenceNumber(handshake.getInitialSeqNo());
				// initialize SendersBufferMarker
				udt_socket.initializeSendersBufferMarker();
				// initialize ReceiversBuffer Marker
				udt_socket.setReceiverBufferMarker(handshake.getInitialSeqNo());
				logger.info("Handshake packet handled in ActiveOpen state");
				udt_socket.goToNextState();
			}
		}
	}

	private void handleEstablished() throws Exception {
		if (packet.isControlPacket()) {
			ControlPacket c_packet = (ControlPacket) packet;
			if (c_packet.getControlPacketType() == ControlPacket.ControlPacketType.CONNECTION_HANDSHAKE.ordinal()) {
				ConnectionHandshake handshake = (ConnectionHandshake) c_packet;
				if (handshake.getConnectionType() == 1) {
					// send response handshake
					ConnectionHandshake response_handshake = udt_socket.getHandshakePacket();
					// calling socket send method directly
					udt_socket.send(response_handshake);
					logger.info("Handshake packet handled in Established state");
				} else
					logger.info("Handshake packet dropped in Established state");
			} else if (c_packet.getControlPacketType() == ControlPacket.ControlPacketType.ACK.ordinal()) {
				ACK ack_packet = (ACK) c_packet;
				long ackNumber = ack_packet.getAckNumber();
				// remove all packets < ackNumber
				udt_socket.removeAckedPackets(ackNumber);
				logger.info("Ack packet handled in Established state");
			} else if (c_packet.getControlPacketType() == ControlPacket.ControlPacketType.NAK.ordinal()) {
				// update senders loss list
				NAK nak_packet = (NAK) c_packet;
				udt_socket.updateSendersLossList(nak_packet.getDecodedLossInfo());
				logger.info("Nak packet handled in Established state");
			}
			else if(c_packet.getControlPacketType() == ControlPacket.ControlPacketType.SHUTDOWN.ordinal()){
				//shutdown handlerpool
				udt_socket.handler_pool.shutdown();
				udt_socket.goToNextState();
			}

		} else {
			
		
			// data packet handling
			DataPacket data_packet = (DataPacket) packet;
			logger.info("Data Packet Received : size : " + data_packet.getData().length);
			// Data Receivers Algorithm
			ArrayList<Long> receiverLossList = udt_socket.getReceiversLossList();
			long packet_seq_num = data_packet.getPacketSequenceNumber();
			long buffer_marker = udt_socket.getReceiverBufferMarker();
			long receiving_seq_num = udt_socket.getReceivingSequenceNumber();

			long val = packet_seq_num - buffer_marker;
			if (val == 0) {
				// add into receiver buffer
				udt_socket.getReceiversBuffer().put(packet_seq_num, data_packet);
				udt_socket.setReceiverBufferMarker(buffer_marker + 1);
				if (receiving_seq_num == packet_seq_num) {
					udt_socket.setReceivingSequenceNumber(receiving_seq_num + 1);
				}
			} else if (val < 0) {
				// do only if packet exists in receiver loss list
				if (receiverLossList.remove(new Long(packet_seq_num))) {
					// add into receiver buffer
					udt_socket.getReceiversBuffer().put(packet_seq_num, data_packet);
					long temp = (receiverLossList.isEmpty()) ? buffer_marker : receiverLossList.get(0);
					udt_socket.setReceivingSequenceNumber(temp);
				}
			} else {
				// add into receiver buffer
				udt_socket.getReceiversBuffer().put(packet_seq_num, data_packet);
				for (long i = buffer_marker; i < packet_seq_num; ++i) {
					receiverLossList.add(i);
				}
				udt_socket.setReceiverBufferMarker(packet_seq_num + 1);
			}
			logger.info("Data packet handled in Established state");

		}
	}

	private void handleClosed() {
		logger.info("Handling packet in Closed State");
	}

}
