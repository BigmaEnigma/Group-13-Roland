import java.util.TimerTask;

public class Rate extends TimerTask{
	
	Controller manager;
	int updateInterval = 3000;
	
	Rate(Controller manager){
		this.manager = manager;

	}
	
	public void run(){
			
		RUBTClient.setDownloadRate(manager.rateCalulatorTotalDownload / updateInterval);
		manager.rateCalulatorTotalDownload=0;
		
		RUBTClient.setUploadRate(manager.rateCalculatorTotalUpload / updateInterval);
		manager.rateCalculatorTotalUpload=0;
		
		for (Peer peer : manager.peers)
		{
                        RUBTClient.UpRate(peer, peer.rateCalculatorTotalUpload/updateInterval);
			peer.rateCalculatorTotalUpload=0;
                    
			RUBTClient.DownRate(peer, peer.rateCalulatorTotalDownload/updateInterval);
			peer.rateCalulatorTotalDownload=0;
			
		}
	}
}