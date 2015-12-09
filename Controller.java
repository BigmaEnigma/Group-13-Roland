import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import GivenTools.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller extends Thread {

	Tracker tracker;
	TorrentInfo torrentInfo;
	File outputFile;
	ArrayList<Peer> peers;
	ServerSocket serverSocket = null;
	int listenPort = -1;
	LinkedBlockingQueue<StringOverride> messageQueue = null;
	public static byte[] peerId = Utils.PeerId();
	Choke optUnchokingObj;
	public Timer optUnchoke;
	public int maxUnchoked = 6;
	public int curUnchoked = 0;
	boolean isRunning = false;
	boolean downloadingStatus = true;
	public boolean[] ourBitfield;
	boolean[] curRequestedBitfield;
	int[] piecePrevalence;
	int rateCalulatorTotalDownload =0;
	int rateCalculatorTotalUpload = 0;
	Timer rateCalculatorTimer;
	Rate rtCalc;
	public int numHashFails = 0;
	public static boolean haveFullFile = false;
        
	public int getRarestPiece(Peer peer){
		Arrays.fill(this.piecePrevalence, 0);
		for(Peer peerT : this.peers){
			for(int i = 0; i < this.piecePrevalence.length; i++){
				if(peerT.bitfield[i] == true){
					this.piecePrevalence[i] += 1;
					if(this.curRequestedBitfield[i])
					this.piecePrevalence[i] += 10;
				}
			}
		}		
		int min = Integer.MAX_VALUE;
		
		for(int i = 0; i < this.piecePrevalence.length; i++){
			if(this.ourBitfield[i] == false && peer.bitfield[i] == true){
				if(min > this.piecePrevalence[i]){
					min = piecePrevalence[i];
				}
			}
		}	
		
		if(min == Integer.MAX_VALUE){
			return -1;
		}
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		
		for(int i = 0; i < this.piecePrevalence.length; i++){
			if(this.ourBitfield[i] == false && peer.bitfield[i] == true){
				if(min == this.piecePrevalence[i]){
					indices.add(i);
				}
			}
		}
		
		Random random = new Random();
		int n = random.nextInt(indices.size());
		
		return indices.get(n);
		
	}
	
	public Controller(TorrentInfo torrentInfo, File file) {
		super();

		this.outputFile = file;
		this.torrentInfo = torrentInfo;
		this.peers = new ArrayList<Peer>();
	}
        
	public void close() throws IOException {
		this.isRunning = false;
		if (this.peers != null) {
			for (Peer p : this.peers) {
				try {
					p.disconnect();
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}
	}
	
	public void init() throws IOException {
		this.tracker = new Tracker(this.torrentInfo, Controller.peerId, this.listenPort, this);
		

		this.curRequestedBitfield = new boolean[this.torrentInfo.piece_hashes.length];
		this.piecePrevalence = new int[this.torrentInfo.piece_hashes.length];
			
		Arrays.fill(this.curRequestedBitfield, false);		
		Arrays.fill(this.piecePrevalence, 0);
		
		ArrayList<Peer> allPeers = this.tracker.update("started", this);

		this.tracker.timer = new Timer();
		this.tracker.trackerUpdate = new TrackerUpdate(this.tracker, this);
		this.tracker.timer.schedule(this.tracker.trackerUpdate,  this.tracker.interval * 1000, this.tracker.interval * 1000);
		
		this.messageQueue = new LinkedBlockingQueue<StringOverride>();
		
		if (allPeers != null) {
			int i = 1;
			for (Peer p : allPeers) {
				if (!p.init()) {
					System.err.println("Cannot contact" + p);
				} else {
					this.peers.add(p);
					RUBTClient.addPeer(i, p.ip, p, p.downloadRate, p.uploadRate, p.Choking);
					++i;
				}
				
			}
			RUBTClient.setNumPeers(i - 1);
		}
		
		if(haveFullFile) {
			this.downloadingStatus = false;
			this.tracker.update("completed", this);
			RUBTClient.log("Finished downloading. Now Seeding");
			Tracker.downloaded = torrentInfo.file_length;
			
			byte[] bitfield = Utils.boolToBitfieldArray(this.ourBitfield);
			Message.Bitfield bitMsg = new Message.Bitfield(bitfield);

			for (Peer p : this.peers) {
				try {
					p.sendMessage(bitMsg); 
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}
		this.optUnchoke = new Timer();
		this.optUnchokingObj = new Choke(this);
		this.optUnchoke.schedule(this.optUnchokingObj, 30000, 30000);
		
		rateCalculatorTimer = new Timer();
		rtCalc = new Rate(this);
		rateCalculatorTimer.schedule(rtCalc, 3000,1000);
	}
	
	public synchronized void recieveMessage(StringOverride message) {
		if (message == null) {
			return;
		}
		this.messageQueue.add(message);
	}
	
	public boolean isFileComplete() {
		for(int i = 0; i < this.ourBitfield.length; i++){
			if(this.ourBitfield[i] == false){
				return false;
			}
		}
		return true;
	}

	public void decode() throws Exception {

		StringOverride peerMsg;

		if ((peerMsg = this.messageQueue.take()) != null) {
			switch (peerMsg.message.id) {
				case (byte)0:
					peerMsg.peer.peerChokingMe = true;
					break;
				case (byte)1:
					peerMsg.peer.peerChokingMe = false;
					if(peerMsg.peer.Interested == true){
						peerMsg.peer.sendMessage(peerMsg.peer.getNextRequest());						
					}
					break;
				case (byte)2:
					peerMsg.peer.peerInterested = true;
					peerMsg.peer.sendMessage(new Message(1, (byte)1));
					break;
				case (byte)3:
					peerMsg.peer.peerInterested = false;
					break;
				case (byte)4:
					if (peerMsg.peer.bitfield != null)
						peerMsg.peer.bitfield[((Message.Have) peerMsg.message).index] = true;
					
					if(peerMsg.peer.bitfield!= null)
					{
					for(int j = 0; j < peerMsg.peer.bitfield.length; j++){
						if(peerMsg.peer.bitfield[j] == true && this.ourBitfield[j] == false){
							peerMsg.peer.sendMessage(Message.INTERESTED);
							peerMsg.peer.Interested = true;
							break;
						}
					}
					}	
					
					break;
				case (byte)5:
					boolean[] bitfield = Utils.bitfieldToBoolArray(((Message.Bitfield)peerMsg.message).bitfield, this.torrentInfo.piece_hashes.length);
					boolean isSeed = true;
					for (int i = 0; i < bitfield.length; ++i) {
						peerMsg.peer.bitfield[i] = bitfield[i];
						if(!bitfield[i])
							isSeed = false;
					}
					
					for(int j = 0; j < peerMsg.peer.bitfield.length; j++){
						if(peerMsg.peer.bitfield[j] == true && this.ourBitfield[j] == false){
							peerMsg.peer.sendMessage(Message.INTERESTED);
							peerMsg.peer.Interested = true;
							break;
						}
					}
					if(isSeed)
					{
						RUBTClient.addSeed();
					}
					break;
			case (byte)7:
				Message.Have haveMsg = new Message.Have(((Message.Piece)peerMsg.message).index);
                                
				if (!this.ourBitfield[((Message.Piece)peerMsg.message).index]) {
						if (peerMsg.peer.appendToPieceAndVerifyIfComplete((Message.Piece)peerMsg.message, this.torrentInfo.piece_hashes, this) == true) {
							this.ourBitfield[((Message.Piece) (peerMsg.message)).index] = true;
							RUBTClient.addProgressBar(1);
							
							for (Peer p : this.peers) {
								try {
								 p.sendMessage(haveMsg); 
								} catch (Exception e) {
									System.err.println(e);
								}
							}
						}
				}			
				if (this.isFileComplete()) {
						this.downloadingStatus = false;
						this.tracker.update("completed", this);
						RUBTClient.log("Finished downloading. Will now seed.");
						RUBTClient.toggleProgressBarLoading();
						return;
				}
				rateCalulatorTotalDownload += ((Message.Piece)peerMsg.message).length;
				if(!peerMsg.peer.peerChokingMe)
					peerMsg.peer.sendMessage(peerMsg.peer.getNextRequest());
				break;
			default:
				break;
			}
		}
	}

	public boolean UpdateFile(Message.Piece piece, ByteBuffer SHA1Hash, byte[] data, String ip) throws Exception {
		if (verifySHA1(data, SHA1Hash, piece.index)) {
			RandomAccessFile raf = new RandomAccessFile(this.outputFile, "rws");

			raf.seek((this.torrentInfo.piece_length * piece.index));
			raf.write(data);
			raf.close();
			
			Tracker.downloaded += data.length;
			RUBTClient.addAmountDownloaded(data.length);
			RUBTClient.log("Got piece " + piece.index + " from " + ip);
			
			return true;
		} else{
			return false;
		}
	}

	public byte[] readFile(int index, int offset, int length) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(this.outputFile, "r");
		byte[] data = new byte[length];
		
		raf.seek(this.torrentInfo.piece_length * index + offset);
		raf.readFully(data);
		raf.close();
		
		return data;
	}

	@Override
	public void run() {
		while (this.isRunning == true) {
			try {
				decode();
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
	
	public static boolean verifySHA1(byte[] piece, ByteBuffer SHA1Hash, int index) {
			MessageDigest SHA1;

			try {
				SHA1 = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				return false;
			}
			
			SHA1.update(piece);
			byte[] pieceHash = SHA1.digest();

			if (Arrays.equals(pieceHash, SHA1Hash.array())) {
				return true;
			} else {
				return false;							
			}
	}

	public boolean[] checkPieces() throws IOException {

		int numPieces = this.torrentInfo.piece_hashes.length;
		int pieceLength = this.torrentInfo.piece_length;
		int fileLength = this.torrentInfo.file_length;
		ByteBuffer[] pieceHashes = this.torrentInfo.piece_hashes;
		int lastPieceLength = fileLength % pieceLength == 0 ? pieceLength : fileLength % pieceLength;

		byte[] piece = null;
		boolean[] verifiedPieces = new boolean[numPieces];

		for (int i = 0; i < numPieces; i++) {
			if (i != numPieces - 1) {
				piece = new byte[pieceLength];
				piece = readFile(i, 0, pieceLength);
			} else {
				piece = new byte[lastPieceLength];
				piece = readFile(i, 0, lastPieceLength);
			}
			
			if (verifySHA1(piece, pieceHashes[i], i)) {
				verifiedPieces[i] = true;
				RUBTClient.log("Verified piece " + i);
			}
		}
		
		for(int i = 0; i < verifiedPieces.length; i++){
			if(i == verifiedPieces.length - 1){
				this.downloadingStatus = false;
			}
		}
		
		return verifiedPieces;
	}
}
