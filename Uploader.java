import java.util.concurrent.LinkedBlockingQueue;

public class Uploader extends Thread{
	
	LinkedBlockingQueue<Message.Request> uploadQueue = null;
        
	public Peer peer;
	
	public boolean isRunning = false;
        
	Uploader(Peer peer){
		this.peer = peer;
		this.uploadQueue = new LinkedBlockingQueue<Message.Request>();
	}
	
	public void recieveRequest(Message.Request message) {
		if (message == null) {
			return;
		}
		
		this.uploadQueue.add(message);
	}
	
	public void run(){

		Message.Request requestMessage = null;
		while(this.isRunning == true){
			if(this.peer.Choking == false){
				try {
					if (( requestMessage = this.uploadQueue.take()) != null) {
						try {
							byte[] dataToUpload;
							dataToUpload = this.peer.manager.readFile(requestMessage.index, requestMessage.start, requestMessage.mlength);
							Tracker.uploaded += dataToUpload.length;
							this.peer.uploadRate += dataToUpload.length;
							this.peer.sendMessage(new Message.Piece(requestMessage.index, requestMessage.start, dataToUpload));
                                                           
                                                        RUBTClient.log("Sent piece " + requestMessage.index + " to " + peer.ip);
							peer.totalUpload += dataToUpload.length;
							peer.rateCalculatorTotalUpload += dataToUpload.length;
							peer.manager.rateCalculatorTotalUpload+= dataToUpload.length;
							RUBTClient.addAmountUploaded(dataToUpload.length);
						} catch(Exception e){
							System.err.println(e);
						}
					}
				} catch (InterruptedException e) {
					break;
				}
				
			}
			else{
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}