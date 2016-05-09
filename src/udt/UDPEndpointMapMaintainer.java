package udt;

public class UDPEndpointMapMaintainer extends Thread {

	
	public UDPEndpoint udp_endpoint;
	public UDPEndpointMapMaintainer(UDPEndpoint udp_endpoint){
		this.udp_endpoint = udp_endpoint;
	}
	
	public void run(){
		
		while(true){
			udp_endpoint.removeClosedSockets();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {

			}
		}
	}
}
