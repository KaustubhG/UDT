package udt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import app.ServerApp;

public class UDPEndpoint {

	public final int DATAGRAM_SIZE = 1400;
	private volatile HashMap<Long, UDTSocket> socketIDSocketMap = new HashMap<>();
	private DatagramSocket udp_socket;
	private final static AtomicLong nextSocketID = new AtomicLong(20 + new Random().nextInt(5000));
	public ExecutorService receiver_pool = Executors.newFixedThreadPool(1);
	public ExecutorService handler_pool = Executors.newFixedThreadPool(1);

	private volatile HashMap<Long, Long> destSrcSocketIDMap = new HashMap<>();

	public UDPEndpoint(int port) {
		try {
			udp_socket = new DatagramSocket(port);
			receiver_pool.execute(new UDPEndpointReceiver(this));
			//start MapMaintainer
			UDPEndpointMapMaintainer maintainer = new UDPEndpointMapMaintainer(this);
			maintainer.start();
		} catch (SocketException e) {
			throw new RuntimeException("Error in creating udp socket : " + e.getMessage());
		}
	}

	public UDTSocket createUDTSocket(UDTSocket.Role role, InetAddress destination_ip, int destination_port)
			throws Exception {

		long socketId = nextSocketID.incrementAndGet();
		UDTSocket socket = new UDTSocket(role, destination_ip, destination_port, udp_socket, socketId);
		socket.DATAGRAM_SIZE = DATAGRAM_SIZE;
		socketIDSocketMap.put(socketId, socket);

		if (role == UDTSocket.Role.SERVER) {
			ServerApp server_app = new ServerApp(socket);
			server_app.start();
		} else {
			socket.connect();
		}
		return socket;
	}

	public DatagramSocket getUDPSocket() {
		return this.udp_socket;
	}

	public UDTSocket getUDTSocket(long socketID) {
		if (this.socketIDSocketMap.containsKey(socketID))
			return this.socketIDSocketMap.get(socketID);
		return null;
	}

	public void removeClosedSockets() {
		for (long socketID : this.socketIDSocketMap.keySet()) {
			if (this.socketIDSocketMap.get(socketID).getState() == UDTSocket.SocketState.CLOSED) {
				this.socketIDSocketMap.remove(socketID);
			}
		}
	}

	public HashMap<Long, Long> getDestSrcSocketIDMap() {
		return this.destSrcSocketIDMap;
	}



}
