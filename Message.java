import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Message {
    
	public static final Message KEEP_ALIVE = new Message(0, (byte)255);
	public static final Message CHOKE = new Message(1, (byte)0);
	public static final Message UNCHOKE = new Message(1, (byte)1);
	public static final Message INTERESTED = new Message(1, (byte)2);
	public static final Message UNINTERESTED = new Message(1, (byte)3);
	protected final byte id;
	protected final int length;
	
	protected Message(final int length, final byte id) {
		this.id = id;
		this.length = length;
	}

	public void encodePayload(DataOutputStream dos) throws IOException {
	}
	
	public static final class Have extends Message {

		public final int index;		
		
		public Have(final int index) {
			super(5, (byte)4);
			this.index = index;
		}

		public void encodePayload(DataOutputStream dos) throws IOException {
			dos.writeInt(this.index);
		}
	}
	public static final class Bitfield extends Message {
		
		public final byte[] bitfield;

		public Bitfield(final byte[] bitfield) {
			super(bitfield.length + 1, (byte)5);
			this.bitfield = bitfield;
		}

		public void encodePayload(DataOutputStream dos) throws IOException {
			dos.write(this.bitfield);
		}
	}
	
	public static final class Request extends Message {

		final int index;
		
		final int start;
		
		final int mlength;

		public Request(final int index, final int start, final int length) {
			super(13, (byte)6);
			this.index = index;
			this.start = start;
			this.mlength = length;
		}

		public String toString() {
			return new String("Length: " + this.length + " ID: " + this.id
					+ " Index: " + this.index + " Start: " + this.start
					+ " Block: " + this.mlength);
		}
		
		public void encodePayload(DataOutputStream dos) throws IOException {
			dos.writeInt(this.index);
			dos.writeInt(this.start);
			dos.writeInt(this.mlength);
		}
	}
        
	public static final class Piece extends Message {

		final int index;
                
		final int start;
		
		final byte[] block;

		public String toString() {
			return new String("ID: " + this.id + " Length: " + this.length
					+ " index: " + this.index);
		}
		public Piece(final int index, final int start, final byte[] block) {
			super(9 + block.length, (byte)7);
			this.index = index;
			this.start = start;
			this.block = block;
		}

		public void encodePayload(DataOutputStream dos) throws IOException {
			dos.writeInt(this.index);
			dos.writeInt(this.start);
			dos.write(this.block);
		}
	}

	public static final class Cancel extends Message {

		private final int index;
	
		private final int start;
		
		private final int clength;

		public Cancel(final int index, final int start,	final int length) {
			super(13, (byte)8);
			this.index = index;
			this.start = start;
			this.clength = length;
		}

		/* (non-Javadoc)
		 * @see Message#encodePayload(java.io.DataOutputStream)
		 */
		public void encodePayload(DataOutputStream dos) throws IOException {
			dos.writeInt(this.index);
			dos.writeInt(this.start);
			dos.writeInt(this.clength);
		}
	}

	public static Message decode(final InputStream input)
			throws IOException {
		
		DataInputStream dataInput = new DataInputStream(input);

		int length = dataInput.readInt();


		if (length == 0) {
			return KEEP_ALIVE;
		}

		byte id = dataInput.readByte();

		switch (id) {
		case ((byte)0):
			return CHOKE;
		case ((byte)1):
			return UNCHOKE;
		case ((byte)2):
			return INTERESTED;
		case ((byte)3):			
			return UNINTERESTED;
		case ((byte)4):
			int index = dataInput.readInt();
			return new Have(index);
		case ((byte)5):
			byte[] bitfield = new byte[length - 1];
			dataInput.readFully(bitfield);
			return new Bitfield(bitfield);
		case ((byte)6):
			int in = dataInput.readInt();
			int begin = dataInput.readInt();
			length = dataInput.readInt();
			return new Request(in, begin, length);
                case ((byte)7):
			int ind = dataInput.readInt();
			int start = dataInput.readInt();
			byte[] block = new byte[length - 9];
			dataInput.readFully(block);
			return new Piece(ind, start, block);
                }
		return null;
	}
        
	public static void encode(final Message message, final OutputStream output)	throws IOException {
		if (message != null) {
			{
				DataOutputStream dos = new DataOutputStream(output);
				dos.writeInt(message.length);
				if (message.length > 0) {
					dos.write(message.id);
					message.encodePayload(dos);
				}
				dos.flush();

			}
		}
	}

	private static final String[] TYPES = new String[]{"Choke", "Unchoke", "Interested", "Uninterested", "Have", "Bitfield", "Request", "Piece", "Cancel", "Port"};

	@Override
	public String toString() {
		if(this.length == 0){
			return "Keep-Alive";
		}
		return TYPES[this.id]; 
	}
}
