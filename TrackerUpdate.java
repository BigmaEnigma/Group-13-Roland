import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;

public class TrackerUpdate extends TimerTask{
	
	Tracker tracker;
	
	Controller manager;

	TrackerUpdate (Tracker tracker, Controller manager){
		this.tracker = tracker;
		this.manager = manager;
	}
	
	public void run(){
		ArrayList<Peer> peers = this.tracker.update("", this.manager);
		boolean isAlreadyAPeer = false;
		
		for(Peer p : peers){
			for(Peer q : manager.peers){
				if(Arrays.equals(p.peerId, q.peerId)){
					isAlreadyAPeer = true;
					break;
				}
			}
			if(isAlreadyAPeer == false){
				this.manager.peers.add(p);
			} else {
				isAlreadyAPeer = false;
			}
		}		
	}
}