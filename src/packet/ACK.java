package packet;

import java.io.ByteArrayOutputStream;

public class ACK extends ControlPacket {

	// the ack sequence number
	// to be handled
	private long ackSequenceNumber;

	// the packet sequence number to which all the previous packets have been
	// received (excluding)
	private long ackNumber;

	public ACK() {
		this.controlPacketType = ControlPacketType.ACK.ordinal();
	}

	public ACK(long ackSeqNo, byte[] controlInformation) {
		this();
		this.ackSequenceNumber = ackSeqNo;
		decodeControlInformation(controlInformation);
	}

	void decodeControlInformation(byte[] data) {
		ackNumber = PacketUtil.decode(data, 0);
	}

	@Override
	protected long getAdditionalInfo() {
		return ackSequenceNumber;
	}

	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}

	public void setAckSequenceNumber(long ackSequenceNumber) {
		this.ackSequenceNumber = ackSequenceNumber;
	}

	/**
	 * get the ack number (the number up to which all packets have been received (excluding))
	 * 
	 * @return
	 */
	public long getAckNumber() {
		return ackNumber;
	}

	/**
	 * set the ack number (the number up to which all packets have been received (excluding))
	 * 
	 * @param ackNumber
	 */
	public void setAckNumber(long ackNumber) {
		this.ackNumber = ackNumber;
	}

	@Override
	public byte[] encodeControlInformation() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(PacketUtil.encode(ackNumber));

			return bos.toByteArray();
		} catch (Exception e) {
			// can't happen
			return null;
		}

	}
}
