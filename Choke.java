import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;

public class Choke extends TimerTask{
	
	Controller manager;
        
	Choke(Controller manager){
		this.manager = manager;
	}
	
	public void run(){
		
		Peer bad = null;
		long lowest = Long.MAX_VALUE;
		for(Peer peer : manager.peers){
			if(peer.Choking == false){
				if(manager.downloadingStatus == true){
					if(lowest > peer.totalDownload){
						bad = peer;
						lowest = peer.totalDownload;
					}
				} else {
					if(lowest > peer.totalUpload){
						bad = peer;
						lowest = peer.totalUpload;
					}
				}
			}
		}
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for(Peer p : manager.peers){
			if(p.Choking == true){
				indices.add(manager.peers.indexOf(p));
			}
		}
		Random random = new Random();
                int n = 0;
                try {
                    n = random.nextInt(indices.size());
                } catch (Exception e){
                    
                }
                    
		bad.Choking = true;
		RUBTClient.updatePeerChokeStatus(bad, true);
		manager.peers.get(indices.get(n)).Choking = false;
		RUBTClient.updatePeerChokeStatus(manager.peers.get(indices.get(n)),false);
		
		bad.choke();
		manager.peers.get(indices.get(n)).unchoke();
		
		for(Peer p : manager.peers){
			p.totalDownload = 0;
			p.totalDownload = 0;
		}
		
	}
}