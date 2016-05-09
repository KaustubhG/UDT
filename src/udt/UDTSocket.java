package udt;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import packet.ConnectionHandshake;
import packet.DataPacket;
import packet.PacketFactory;
import packet.Shutdown;
import packet.UDTPacket;
import timers.ACKTimer;
import timers.NAKTimer;
import timers.SNDTimer;

public class UDTSocket {
	private static final Logger logger = Logger.getLogger(UDTSocket.class.getName());

	public final long udtVersion = 4;
	public int DATAGRAM_SIZE;
	private long destinationSocketID = 0l;
	// sequence number of sender's data packet
	private long sequenceNumber = 0l;
	// sequence number of next expected data packet
	private long receivingSequenceNumber;
	// highest sequence number + 1 in ReceiversBuffer
	private long receiverBufferMarker;
	// sequence number of next data packet to be sent
	private long sendersBufferMarker;
	// sequence number of last data packet acked
	private long lastAckedPacket;
	private DatagramSocket udp_socket;
	private volatile SocketState state = SocketState.START;
	private ConnectionHandshake handshake;

	private final long socketID;

	private InetAddress destination_ip;
	private int destination_port;

	private volatile ArrayList<Long> sendersLossList = new ArrayList<>();
	private volatile TreeMap<Long, UDTPacket> sendersBuffer = new TreeMap<>();
	private volatile ArrayList<Long> receiversLossList = new ArrayList<>();
	private volatile TreeMap<Long, UDTPacket> receiversBuffer = new TreeMap<>();

	public ExecutorService handler_pool = Executors.newFixedThreadPool(1);

	// timers
	private ACKTimer ackTimer;
	private NAKTimer nakTimer;
	private SNDTimer sndTimer;

	// UDPEndpointCLient
	public UDTSocket(Role role, InetAddress destination_ip, int destination_port, DatagramSocket udp_socket,
			long socketID) throws Exception {
		if (role == Role.CLIENT) {
			this.destination_ip = destination_ip;
			this.destination_port = destination_port;
			this.socketID = socketID;
			state = SocketState.ACTIVE_OPEN;
			this.udp_socket = udp_socket;
		} else {
			this.udp_socket = udp_socket;
			this.socketID = socketID;
			state = SocketState.LISTEN;
			this.destination_ip = destination_ip;
			this.destination_port = destination_port;
		}
	}

	public void send(UDTPacket packet) throws Exception {
		if (packet == null) {
			throw new RuntimeException("Error; Trying to send null packet");
		}
		byte[] msg = packet.getEncoded();
		DatagramPacket udp_packet = new DatagramPacket(msg, msg.length, destination_ip, destination_port);
		udp_socket.send(udp_packet);
	}

	public ConnectionHandshake createHandshakePacket(long connection_type) {
		handshake = new ConnectionHandshake();
		handshake.setMessageNumber(0);// undefined
		handshake.setDestinationSocketID(getDestinationSocketID());
		handshake.setTimeStamp(1);
		handshake.setMaxFlowWndSize(1024 * 10);
		handshake.setPacketSize(1400);
		handshake.setSocketID(getSocketID());
		handshake.setInitialSeqNo(incrementAndGetSeqNumber());
		handshake.setConnectionType(connection_type);

		return handshake;
	}

	public void connect() throws Exception {
		createHandshakePacket(ConnectionHandshake.CONNECTION_TYPE_CLIENT);
		while (state == SocketState.ACTIVE_OPEN) {
			logger.info("Sending Handshake Packet in ACTIVE_OPEN state");
			send(handshake);
			Thread.sleep(100);
		}
	}

	public UDTPacket receive() throws Exception {
		byte[] buffer = new byte[DATAGRAM_SIZE];
		DatagramPacket udp_packet = new DatagramPacket(buffer, buffer.length);
		udp_socket.receive(udp_packet);

		UDTPacket udt_packet = PacketFactory.createPacket(udp_packet);
		return udt_packet;
	}

	public void goToNextState() {

		switch (state) {
		case START:
			state = SocketState.ACTIVE_OPEN;
			logger.info("State changed to ACTIVE_OPEN ");
			break;
		case LISTEN:
			state = SocketState.ESTABLISHED;
			logger.info("State changed to ESTABLISHED ");
			startTimers();
			break;
		case ACTIVE_OPEN:
			state = SocketState.ESTABLISHED;
			logger.info("State changed to ESTABLISHED ");
			startTimers();
			break;
		case ESTABLISHED:
			state = SocketState.CLOSED;
			logger.info("State changed to CLOSED ");
			break;
		default:
			throw new RuntimeException("Invalid state");
		}
	}

	private void startTimers() {

		ackTimer = new ACKTimer(this);
		ackTimer.start();

		nakTimer = new NAKTimer(this);
		nakTimer.start();

		sndTimer = new SNDTimer(this);
		sndTimer.start();

	}

	public ArrayList<DataPacket> getAndFlushReceiversBuffer() {
		ArrayList<DataPacket> received_packets = new ArrayList<>();

		long first_pos = (receiversBuffer.isEmpty()) ? receiverBufferMarker : receiversBuffer.firstKey();
		long buffer_marker = receiverBufferMarker;
		if (first_pos < buffer_marker) {
			received_packets.add((DataPacket) receiversBuffer.remove(first_pos));
			first_pos += 1;
		}
		return received_packets;
	}

	public void wrapAndStoreData(byte[] data, DataPacket.Position position) {
		DataPacket packet = new DataPacket();
		packet.setPacketSequenceNumber(sequenceNumber);
		packet.setMessageNumber(1, position, 0);
		packet.setTimeStamp(1);
		packet.setDestinationSocketID(destinationSocketID);
		packet.setData(data);
		sendersBuffer.put(sequenceNumber, packet);
		sequenceNumber++;
	}

	public void removeAckedPackets(long ackSeqNumber) {
		if (sendersBuffer.isEmpty())
			return;

		setLastAckedPacket(ackSeqNumber);
		long first = sendersBuffer.firstKey();
		while (first < ackSeqNumber) {
			sendersBuffer.remove(first);
			if (sendersBuffer.isEmpty())
				break;
			first = sendersBuffer.firstKey();
		}
		logger.info("Acked Packets removed before seq number : " + ackSeqNumber);
	}

	public void updateSendersLossList(ArrayList<Long> decodedLossInfo) {
		for (long seqNum : decodedLossInfo) {
			sendersLossList.add(seqNum);
		}
	}

	public void close() throws Exception {
		Shutdown shutdown = new Shutdown();
		shutdown.setDestinationSocketID(destinationSocketID);
		shutdown.setMessageNumber(1);
		shutdown.setTimeStamp(1);
		send(shutdown);
		state = SocketState.CLOSED;
		handler_pool.shutdown();
	}

	public DatagramSocket getSocket() {
		return udp_socket;
	}

	public SocketState getState() {
		return state;
	}

	public long getSocketID() {
		return socketID;
	}

	public void setDestinationSocketID(long destinationSocketID) {
		this.destinationSocketID = (this.destinationSocketID == 0l) ? destinationSocketID : this.destinationSocketID;
	}

	public long getDestinationSocketID() {
		return destinationSocketID;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long incrementAndGetSeqNumber() {
		return ++sequenceNumber;
	}

	public long getReceivingSequenceNumber() {
		return receivingSequenceNumber;
	}

	public void setReceivingSequenceNumber(long receivingSequenceNumber) {
		this.receivingSequenceNumber = receivingSequenceNumber;
	}

	public ConnectionHandshake getHandshakePacket() {
		return handshake;
	}

	public ArrayList<Long> getSendersLossList() {
		return sendersLossList;
	}

	public TreeMap<Long, UDTPacket> getSendersBuffer() {
		return sendersBuffer;
	}

	public ArrayList<Long> getReceiversLossList() {
		return receiversLossList;
	}

	public TreeMap<Long, UDTPacket> getReceiversBuffer() {
		return receiversBuffer;
	}

	public long getSendersBufferMarker() {
		return sendersBufferMarker;
	}

	public void initializeSendersBufferMarker() {
		sendersBufferMarker = this.sequenceNumber;
	}

	public void incrementSendersBufferMarker() {
		sendersBufferMarker++;
	}

	public long getReceiverBufferMarker() {
		return receiverBufferMarker;
	}

	public void setReceiverBufferMarker(long receiverBufferMarker) {
		this.receiverBufferMarker = receiverBufferMarker;
	}

	public long getLastAckedPacket() {
		return lastAckedPacket;
	}

	private void setLastAckedPacket(long lastAckedPacket) {
		this.lastAckedPacket = lastAckedPacket;
	}

	public enum SocketState {
		START, LISTEN, ACTIVE_OPEN, ESTABLISHED, CLOSED
	}

	public enum Role {
		CLIENT, SERVER
	}

}
