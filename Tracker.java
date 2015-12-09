import GivenTools.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class Tracker {

	public static final ByteBuffer KEY_FAILURE = ByteBuffer.wrap(new byte[] {
			'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o',
			'n' });
	public static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' });

	public static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] { 'i',
			'p' });

	public static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] { 'p',
			'o', 'r', 't' });

	public static final ByteBuffer KEY_PEERID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' });

	public static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });

	public static final int requestSize = 16000;
	
	public byte[] infohash;
	
	public byte[] peerid;
	
	public static int uploaded;

	public static int downloaded;
	
	public int left;
        
	private int port;
	
	private URL announce;
	
	private String event;
	
	private URL requestString;
	
	public boolean isRunning = true;
	
	public int interval = -1;
	
	public Controller manager;
	
	public Timer timer;
	
	public TrackerUpdate trackerUpdate;

	Tracker(TorrentInfo torrentData, final byte[] peerId, final int port, Controller manager) {
		this.infohash = torrentData.info_hash.array();
		this.peerid = peerId;
		this.left = torrentData.file_length;
		this.port = port;
		this.event = null;
		this.announce = torrentData.announce_url;
		this.requestString = createURL(torrentData.announce_url);
		this.manager = manager;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Peer> update(String event, Controller manager) {
		this.requestString = createURL(this.announce);
		
		HashMap<ByteBuffer, Object> response = null;

		try {
			byte[] trackerResponse = sendGETRecieveResponse();

			if (trackerResponse == null) {
				return null;
			}
			response = (HashMap<ByteBuffer, Object>) Bencoder2.decode(trackerResponse);
		} catch (BencodingException e) {
			System.err.println(e);
			return null;
		}

		if (response.containsKey(KEY_FAILURE)) {
			return null;
		}

		ArrayList<Peer> peers = new ArrayList<Peer>();

		this.interval = (Integer)response.get(KEY_INTERVAL);

		List<Map<ByteBuffer, Object>> peersList = (List<Map<ByteBuffer, Object>>) response
				.get(KEY_PEERS);

		if (peersList == null) {
			return null;
		}

		for (Map<ByteBuffer, Object> rawPeer : peersList) {
			int peerPort = ((Integer) rawPeer.get(KEY_PORT)).intValue();
			byte[] peerId = ((ByteBuffer) rawPeer.get(KEY_PEERID)).array();
			String ip = null;
			try {
				ip = new String(((ByteBuffer) rawPeer.get(KEY_IP)).array(),
						"ASCII");
			} catch (UnsupportedEncodingException e) {
				System.err.println(e);
				continue;
			}

			peers.add(new Peer(peerId, peerPort, ip, manager));
		}

		if(this.interval < 0){
			this.interval = 120000;
		}
		
		return peers;
	}

	public byte[] sendGETRecieveResponse() {
		try {
			HttpURLConnection httpConnection = (HttpURLConnection)this.requestString.openConnection();
			DataInputStream dataInputStream = new DataInputStream(httpConnection.getInputStream());

			int dataSize = httpConnection.getContentLength();
			byte[] retArray = new byte[dataSize];

			dataInputStream.readFully(retArray);
			dataInputStream.close();

			return retArray;
		} catch (IOException e) {
			System.err.println(e);
			return null;
		}
	}

	public URL createURL(URL announceURL) {
		String newURL = announceURL.toString();
		newURL += "?info_hash=" + Utils.HextoString(this.infohash)
				+ "&peer_id=" + Utils.HextoString(this.peerid) + "&port="
				+ this.port + "&uploaded=" + this.uploaded + "&downloaded="
				+ this.downloaded + "&left=" + this.left;
		if (this.event != null)
			newURL += "&event=" + this.event;

		try {
			return new URL(newURL);
		} catch (MalformedURLException e) {
			System.err.println(e);
			return null;
		}
	}
}