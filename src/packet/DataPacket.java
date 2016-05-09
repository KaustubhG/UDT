package packet;

public class DataPacket implements UDTPacket {

	public static final long DELIVERY_TYPE_IN_ORDER = 1;
	public static final long DELIVERY_TYPE_NO_ORDER = 0;

	private long packetSequenceNumber; // should not exceed 31 bits
	private long messageNumber; // should not exceed 29 bits
	private long timeStamp;
	private long destinationSocketID;
	private Position position;
	private long deliveryType;

	public long getDestinationSocketID() {
		return destinationSocketID;
	}

	public void setDestinationSocketID(long destinationSocketID) {
		this.destinationSocketID = destinationSocketID;
	}

	private byte[] data;

	public DataPacket(long packetSequenceNumber, long messageNumber, Position position, long deliveryType, long timeStamp, long destinationSocketID,
			byte[] data) {
		this.packetSequenceNumber = packetSequenceNumber;
		this.position = position;
		this.deliveryType = deliveryType;
		encodeMessageNumber(messageNumber, position, deliveryType);
		this.timeStamp = timeStamp;
		this.destinationSocketID = destinationSocketID;
		this.data = data;
	}

	public DataPacket() {
	}
	
	public void encodeMessageNumber(long messageNumber, Position position, long deliveryType){
		if (deliveryType == 1) {
			// setting 29th bit
			messageNumber |= 0b100000000000000000000000000000;
		}
		switch (position) {
		case FIRST:
			// setting 31st bit
			messageNumber |= 0b10000000000000000000000000000000;
			break;
		case LAST:
			// setting 30th bit
			messageNumber |= 0b1000000000000000000000000000000;
			break;
		case ONLY:
			// setting 30th, 31th bit
			messageNumber |= 0b11000000000000000000000000000000;
			break;
		case MIDDLE:
			break;
		}
		this.messageNumber = messageNumber;
	}
	
	public void decodeMessageNumber(long messageNumber) {
		// last 29 bits
		this.messageNumber = messageNumber & 0b11111111111111111111111111111;
		
		// 30th bit
		this.deliveryType = (messageNumber & 0b1000000000000000000000000000000) >> 29;
		
		// 31st, 32nd bit
		int pos = (int) (messageNumber >> 30);
		switch (pos){
		case 0:
			this.position = Position.MIDDLE;
			break;
		case 1:
			this.position = Position.LAST;
			break;
		case 2:
			this.position = Position.FIRST;
			break;
		case 3:
			this.position = Position.ONLY;
			break;
		}
	}

	@Override
	public long getPacketSequenceNumber() {
		return packetSequenceNumber;
	}

	public void setPacketSequenceNumber(long sequenceNumber) {
		this.packetSequenceNumber = sequenceNumber;
	}

	@Override
	public long getMessageNumber() {
		return messageNumber;
	}

	@Override
	public void setMessageNumber(long messageNumber) {
		encodeMessageNumber(messageNumber, Position.ONLY, DELIVERY_TYPE_NO_ORDER);
	}

	public void setMessageNumber(long messageNumber, Position position, long deliveryType) {
		encodeMessageNumber(messageNumber, position, deliveryType);
	}

	@Override
	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public long getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(long deliveryType) {
		this.deliveryType = deliveryType;
	}

	// Header + Data for Transmission
	@Override
	public byte[] getEncoded() {
		// header.length is 16
		byte[] result = new byte[16 + data.length];
		System.arraycopy(PacketUtil.encode(packetSequenceNumber), 0, result, 0, 4);
		System.arraycopy(PacketUtil.encode(messageNumber), 0, result, 4, 4);
		System.arraycopy(PacketUtil.encode(timeStamp), 0, result, 8, 4);
		System.arraycopy(PacketUtil.encode(destinationSocketID), 0, result, 12, 4);
		System.arraycopy(data, 0, result, 16, data.length);
		return result;
	}

	@Override
	public int compareTo(UDTPacket other) {
		return (int) (getPacketSequenceNumber() - other.getPacketSequenceNumber());
	}

	@Override
	public boolean isControlPacket() {
		return false;
	}

	public enum Position {
		MIDDLE, LAST, FIRST, ONLY
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("Data Packet Sequence Number : ").append(getPacketSequenceNumber()).append("\n");
		sb.append("Position : ").append(getPosition()).append("\n");
		sb.append("Message Number : ").append(getMessageNumber()).append("\n");
		sb.append("Time Stamp : ").append(getTimeStamp()).append("\n");
		sb.append("Destination ID : ").append(getDestinationSocketID()).append("\n");
		sb.append("Data = ").append(new String(data));
		return sb.toString();
	}
}
