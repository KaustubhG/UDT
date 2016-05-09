package packet;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.logging.Logger;

public class PacketFactory {
	private static final Logger logger = Logger.getLogger(PacketFactory.class.getName());

	public static UDTPacket createPacket(DatagramPacket udp_packet) {

		byte[] udt_data = udp_packet.getData();
		int packet_length = udp_packet.getLength();
		UDTPacket udt_packet = null;
		if ((udt_data[0] & 0x80) == 128) {
			long controlPacketType = PacketUtil.decodeClearHighest(udt_data, 0);

			ControlPacket control_packet = generateControlPacket(controlPacketType, udt_data, packet_length);
			udt_packet = control_packet;
		} else {

			DataPacket data_packet = new DataPacket();

			data_packet.setPacketSequenceNumber(PacketUtil.decode(udt_data, 0));
			data_packet.decodeMessageNumber(PacketUtil.decode(udt_data, 4));
			data_packet.setTimeStamp(PacketUtil.decode(udt_data, 8));
			data_packet.setDestinationSocketID(PacketUtil.decode(udt_data, 12));
			// logger.info("Datagram Length = " + udp_packet.getLength());
			byte[] data = Arrays.copyOfRange(udt_data, 16, udp_packet.getLength());
			data_packet.setData(data);

			udt_packet = data_packet;
			logger.info("Received Data packet");
		}
		return udt_packet;
	}

	private static ControlPacket generateControlPacket(long controlPacketType, byte[] udt_data, int packet_length) {

		ControlPacket control_packet = null;
		if (controlPacketType == ControlPacket.ControlPacketType.CONNECTION_HANDSHAKE.ordinal()) {
			control_packet = new ConnectionHandshake(Arrays.copyOfRange(udt_data, 16, packet_length));
			control_packet.setMessageNumber(PacketUtil.decode(udt_data, 4));
			control_packet.setTimeStamp(PacketUtil.decode(udt_data, 8));
			control_packet.setDestinationSocketID(PacketUtil.decode(udt_data, 12));
			logger.info("Received Handshake packet");
		} else if (controlPacketType == ControlPacket.ControlPacketType.ACK.ordinal()) {
			control_packet = new ACK(PacketUtil.decode(udt_data, 4), Arrays.copyOfRange(udt_data, 16, packet_length));
			control_packet.setTimeStamp(PacketUtil.decode(udt_data, 8));
			control_packet.setDestinationSocketID(PacketUtil.decode(udt_data, 12));
			logger.info("Received Ack packet");
		}
		else if (controlPacketType == ControlPacket.ControlPacketType.NAK.ordinal()) {
			control_packet = new NAK(Arrays.copyOfRange(udt_data, 16, packet_length));
			control_packet.setMessageNumber(PacketUtil.decode(udt_data, 4));
			control_packet.setTimeStamp(PacketUtil.decode(udt_data, 8));
			control_packet.setDestinationSocketID(PacketUtil.decode(udt_data, 12));
			logger.info("Received Nak packet");
		}
		else if(controlPacketType == ControlPacket.ControlPacketType.SHUTDOWN.ordinal()){
			control_packet = new Shutdown();
			control_packet.setMessageNumber(PacketUtil.decode(udt_data, 4));
			control_packet.setTimeStamp(PacketUtil.decode(udt_data, 8));
			control_packet.setDestinationSocketID(PacketUtil.decode(udt_data, 12));
			logger.info("Received Shutdown packet");		
		}

		return control_packet;
	}
}
