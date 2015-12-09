import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;



public class Connect extends Thread{
	
        DataOutputStream output;
	
	DataInputStream input;
    
	public Peer peer;
	
	public ServerSocket peerSocket;
	
	public Socket socket;
	
	public int port;
	
	Controller manager;
	
	byte[] peerId;
	
	String ip;
	
	public void run(){
		try {
			socket = peerSocket.accept();
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
			
			output.write(Peer.generateHandshake(manager.peerId,	manager.torrentInfo.info_hash.array()));
			output.flush();

			byte[] response = new byte[68];
			
			this.socket.setSoTimeout(1000);
			input.readFully(response);		
			this.socket.setSoTimeout(1000);
	
			RUBTClient.log("Handshake Response: " + Arrays.toString(response));
			InetAddress peerIP = socket.getInetAddress();
			ip = peerIP.toString();
			port = socket.getPort();
                        peerId = new byte[20];
                        System.arraycopy(response, 48, peerId, 0, 20);
			
			peer = new Peer(peerId, port, ip, manager);
			peer.init();
			manager.peers.add(peer);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}