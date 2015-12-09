import java.io.IOException;

public class KeepAlive extends Thread{
	
	public Peer peer;
	
	public int interval = 120000;
	
	public boolean isRunning = false;
	
	KeepAlive(Peer peer){
		this.peer = peer;
	}
	
	public void run(){
		while(this.isRunning){
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				continue;
			}
			try {
				peer.sendMessage(Message.KEEP_ALIVE);
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}	
}