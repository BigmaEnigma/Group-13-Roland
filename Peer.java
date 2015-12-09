import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;

public class Peer extends Thread {
    
	protected byte[] peerId;

	protected int port;

	protected String ip;

	boolean Choking = true;

	boolean Interested = false;
	
	boolean peerChokingMe = true;

	boolean peerInterested = false;

	public boolean[] bitfield = null;
        
	public int previousIndex = -1;
	
	Controller manager;
        
	Uploader peerUploader;

	KeepAlive peerKeepAlive;

	private Socket socket = null;

	protected InputStream in;
	
	protected OutputStream out;
	
	public int uploadRate = 0;

	public int downloadRate = 0;
	
	private int currentPieceIndex = -1;
	
	private int currentByteOffset = 0;
	
	private ByteArrayOutputStream piece = null;

	private boolean isRunning = true;

	long totalDownload =0L;
	
	int downloadCount =0;

	public long totalUpload=0L;
	
	int rateCalulatorTotalDownload =0;
	
	int rateCalculatorTotalUpload = 0;

	public Peer(byte[] peerId, int port, String ip, Controller manager) {
		super("Peer@" + ip + ":" + port);
		this.peerId = peerId;
		this.port = port;
		this.ip = ip;
		this.manager = manager;
		this.bitfield = new boolean[this.manager.torrentInfo.piece_hashes.length];
		this.peerUploader = new Uploader(this);
		this.peerUploader.isRunning = true;
		this.peerUploader.start();
		this.peerKeepAlive = new KeepAlive(this);
		Arrays.fill(this.bitfield, false);
	}

	public boolean init() {
		byte[] id = new byte[6];
                try {
                    System.arraycopy(this.peerId, 0, id, 0, 6);
                } catch (Exception e){
                    return false;
                }
                if (!Arrays.equals(id, new byte[] {'-', 'R', 'U', '1', '1', '0'}) && !Arrays.equals(id, new byte[] { '-', 'A', 'Z', '5', '7', '0'})){ 
			return false;
		}
		
                isRunning = true;
		try {
			this.connect();

			DataOutputStream os = new DataOutputStream(this.out);
			DataInputStream is = new DataInputStream(this.in);

			if (is == null || os == null) {
				this.disconnect();
				return false;
			}

			os.write(Peer.generateHandshake(Controller.peerId, manager.torrentInfo.info_hash.array()));
			os.flush();

			byte[] response = new byte[68];
			
			
			this.socket.setSoTimeout(10000);
			is.readFully(response);		
			this.socket.setSoTimeout(130000);
			
			if(!checkHandshake(manager.torrentInfo.info_hash.array(), response)){
				return false;
			}

			if(this.manager.curUnchoked < this.manager.maxUnchoked){
				this.Choking = false;
				this.manager.curUnchoked++;
			} else {
				this.Choking = true;
			}
			
			this.start();
			
			RUBTClient.log("Connected to " + this);
			
			return true;
		} catch (Exception e) {
			System.err.println(e);
			return false;
		}
	}
        
	public Message.Request getNextRequest() {
		int piece_length = this.manager.torrentInfo.piece_length;
		int file_length = this.manager.torrentInfo.file_length;
		int requestSize = Tracker.requestSize;
		int numPieces = this.manager.torrentInfo.piece_hashes.length;
		
		if(this.currentPieceIndex == -1){
			if((this.currentPieceIndex = this.manager.getRarestPiece(this)) == -1){
				RUBTClient.log("Failed to get next piece");
				return null;
			}			
		} 
		if(this.currentPieceIndex == (numPieces - 1)){
			piece_length = file_length % this.manager.torrentInfo.piece_length;
		}
		
		if((this.currentByteOffset + requestSize) > piece_length){
			requestSize = piece_length % requestSize;
		}
		
		Message.Request request = new Message.Request(this.currentPieceIndex, this.currentByteOffset, requestSize);
		
		if((this.currentByteOffset + requestSize) >= piece_length){
			this.currentPieceIndex = -1;
			this.currentByteOffset = 0;
		} else {
			this.currentByteOffset += requestSize;
		}
		
		return request;		
	}
	
	public boolean appendToPieceAndVerifyIfComplete(Message.Piece pieceMsg, ByteBuffer[] hashes,
			Controller manager) {

		int currentPieceLength = (pieceMsg.index == (this.manager.torrentInfo.piece_hashes.length - 1)) ? this.manager.torrentInfo.file_length % this.manager.torrentInfo.piece_length : this.manager.torrentInfo.piece_length;
		
		if (this.piece == null) {
			this.piece = new ByteArrayOutputStream();
		}

		try {
			piece.write(pieceMsg.block, 0, pieceMsg.block.length);
		} catch (Exception e) {
			System.err.println(e);
		}
		
		if(this.currentPieceIndex == -1){	
			this.previousIndex = pieceMsg.index;
			this.manager.curRequestedBitfield[this.previousIndex] = false;
			
			try {
				if (manager.UpdateFile(pieceMsg, hashes[pieceMsg.index], piece.toByteArray(), this.ip)) {
					totalDownload+= currentPieceLength;
					this.manager.ourBitfield[pieceMsg.index] = true;
					rateCalulatorTotalDownload+= totalDownload;	
					piece = null;
					return true;
				} else {
						piece = null;
				}
			} catch (Exception e) {
				System.err.println(e);
				piece = null;
			}
		}
	
		return false;
	}

	public synchronized void connect() throws IOException {
		this.socket = new Socket(this.ip, this.port);
		this.in = this.socket.getInputStream();
		this.out = this.socket.getOutputStream();
	}

	public synchronized void disconnect() {
		if(this.Choking == false){
			this.manager.curUnchoked -= 1;
		}
		if (this.socket != null) {
			this.peerKeepAlive.isRunning = false;
			this.peerUploader.isRunning = false;
			this.peerKeepAlive.interrupt();
			this.peerUploader.interrupt();
		}
			
			try {
				if(this.socket!= null)
				this.socket.close();
				
			} catch (IOException e) {
				System.err.println(e);
				
			} finally {
				this.socket = null;
				this.in = null;
				this.out = null;
				//this.manager.peers.remove(this);
				isRunning = false;
			
			}
		
	}

	public synchronized void sendMessage(Message m) throws IOException {
		if (this.out == null) {
			throw new IOException(this
					+ " cannot send a message on an empty socket.");
		}
		Message.encode(m, this.out);
		this.peerKeepAlive.interrupt();
	}

	public String toString() {
		return new String(peerId) + " " + port + " " + ip;
	}
        
	public static byte[] generateHandshake(byte[] peer, byte[] infohash) {
		int index = 0;
		byte[] handshake = new byte[68];
		
		handshake[index] = 0x13;
		index++;
		
		byte[] BTChars = { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		System.arraycopy(BTChars, 0, handshake, index, BTChars.length);
		index += BTChars.length;
		
		byte[] zero = new byte[8];
		System.arraycopy(zero, 0, handshake, index, zero.length);
		index += zero.length;
		
		System.arraycopy(infohash, 0, handshake, index, infohash.length);
		index += infohash.length;
		
		System.arraycopy(peer, 0, handshake, index, peer.length);
		
		return handshake;
	}

	public void run() {
		
		while (this.socket != null && !this.socket.isClosed() && isRunning) {
				Message myMessage = null;
				try {
					myMessage = Message.decode(this.in);
				} catch (IOException e) {
					System.err.println(e);
					break;
				}
				if (myMessage != null) {
					if(myMessage.id == (byte)6){
						this.peerUploader.recieveRequest((Message.Request)myMessage);
					} else {
						this.manager.recieveMessage(new StringOverride(this, myMessage));
					}
				}
		} 
	}
	
	public boolean checkHandshake(byte[] infoHash, byte[] response) {
		byte[] peerHash = new byte[20];
		System.arraycopy(response, 28, peerHash, 0, 20);

		if(!Arrays.equals(peerHash, infoHash))
		{
			return false;
		}
		return true;
	}
	
	public void choke(){
		try {
			this.sendMessage(Message.CHOKE);
		} catch (IOException e) {
			System.err.println(e);
		}
		this.Choking = true;
		RUBTClient.updatePeerChokeStatus(this, true);
	}
	
	public void unchoke(){
		try {
			this.sendMessage(Message.UNCHOKE);
		} catch (IOException e) {
			System.err.println(e);
		}
		this.Choking = false;
		RUBTClient.updatePeerChokeStatus(this, false);
	}
	
	
	
}
