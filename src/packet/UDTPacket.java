package packet;

public interface UDTPacket extends Comparable<UDTPacket>{


	public long getMessageNumber();	
	
	/*
	 * @param messageNumber - max 29 bits
	 */
	public void setMessageNumber(long messageNumber) ;
	
	public void setTimeStamp(long timeStamp);	
	public long getTimeStamp();
	
	public boolean isControlPacket();

	public byte[] getEncoded();
	
	public long getPacketSequenceNumber();
	
	public long getDestinationSocketID();
	
}
