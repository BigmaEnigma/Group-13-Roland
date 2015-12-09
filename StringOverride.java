
public class StringOverride{
	
        public Peer peer;
        
	public Message message;
	
	StringOverride(Peer peer, Message message){
		this.message = message;
		this.peer = peer;
	}
        
	public String toString(){
		return this.peer.toString() + " " + this.message.toString();
	}
}