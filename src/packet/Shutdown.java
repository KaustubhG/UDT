package packet;

public class Shutdown extends ControlPacket{
	
	public Shutdown(){
		this.controlPacketType=ControlPacketType.SHUTDOWN.ordinal();	
	}
	
	@Override
	public byte[] encodeControlInformation(){
		return null;
	}
}

