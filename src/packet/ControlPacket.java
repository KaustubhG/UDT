package packet;

public abstract class ControlPacket implements UDTPacket {

	protected long controlPacketType; // This is 31 bits [Type:ExtendedType]
	protected long messageNumber; // Additional Info field
	protected long timeStamp;
	protected long destinationSocketID;
	protected byte[] controlInformation;


	public abstract byte[] encodeControlInformation();

	public long getDestinationSocketID() {
		return destinationSocketID;
	}

	public void setDestinationSocketID(long destinationSocketID) {
		this.destinationSocketID = destinationSocketID;
	}

	public long getControlPacketType() {
		return controlPacketType;
	}

	@Override
	public long getMessageNumber() {
		return messageNumber;
	}

	@Override
	public void setMessageNumber(long ackSequenceNumber) {
		this.messageNumber = ackSequenceNumber;
	}

	@Override
	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	// Header + ControlInformation for packet Transmission
	@Override
	public byte[] getEncoded() {

		// Header is 16 bytes
		byte[] header = new byte[16];
		System.arraycopy(PacketUtil.encodeSetHighest(true, controlPacketType), 0, header, 0, 4);
		System.arraycopy(PacketUtil.encode(messageNumber), 0, header, 4, 4);
		System.arraycopy(PacketUtil.encode(timeStamp), 0, header, 8, 4);
		System.arraycopy(PacketUtil.encode(destinationSocketID), 0, header, 12, 4);

		byte[] controlInfo = encodeControlInformation();

		byte[] result = controlInfo != null ? new byte[header.length + controlInfo.length] : new byte[header.length];
		System.arraycopy(header, 0, result, 0, header.length);
		if (controlInfo != null) {
			System.arraycopy(controlInfo, 0, result, header.length, controlInfo.length);
		}

		return result;
	};

	// Returns Additional Information for the control packet
	protected long getAdditionalInfo() {
		return 0L;
	}

	@Override
	public int compareTo(UDTPacket other) {
		return (int) (getPacketSequenceNumber() - other.getPacketSequenceNumber());
	}

	@Override
	public boolean isControlPacket() {
		return true;
	}

	@Override
	public long getPacketSequenceNumber() {
		return -1;
	}

	public static enum ControlPacketType {

		CONNECTION_HANDSHAKE, KEEP_ALIVE, ACK, NAK, UNUNSED_1, SHUTDOWN, ACK2, MESSAGE_DROP_REQUEST, UNUNSED_2, UNUNSED_3, UNUNSED_4, UNUNSED_5, UNUNSED_6, UNUNSED_7, UNUNSED_8, USER_DEFINED,

	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("Control Packet type : ").append(getControlPacketType()).append("\n");
		sb.append("Message Number : ").append(getMessageNumber()).append("\n");
		sb.append("Time Stamp : ").append(getTimeStamp()).append("\n");
		sb.append("Destination ID : ").append(getDestinationSocketID());
		return sb.toString();
	}

}
