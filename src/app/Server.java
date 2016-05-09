package app;

import java.util.logging.Logger;

import udt.UDPEndpoint;

public class Server {

	private static final Logger logger = Logger.getLogger(Server.class.getName());

	public static void main(String[] args) throws InterruptedException {
		
		UDPEndpoint udp_endpoint = new UDPEndpoint(4000);
	}

}
