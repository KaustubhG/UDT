package packet;


//Send as immediate reply to ACK
public class ACK2 extends ControlPacket{


	//the ack sequence number
	private long ackSequenceNumber ;

	public ACK2(){
		this.controlPacketType=ControlPacketType.ACK2.ordinal();	
	}

	public ACK2(long ackSeqNo,byte[]controlInformation){
		this();
		this.ackSequenceNumber=ackSeqNo;
		decode(controlInformation );
	}

	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}
	 public void setAckSequenceNumber(long ackSequenceNumber) {
		this.ackSequenceNumber = ackSequenceNumber;
	}
	 
	void decode(byte[]data){
	}

	public boolean forSender(){
		return false;
	}

	private static final byte[]empty=new byte[0];
	@Override
	public byte[] encodeControlInformation(){
		return empty;
	}
}



