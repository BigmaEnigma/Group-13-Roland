import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import GivenTools.*;

public class Utils extends ToolKit {
	
	public static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String HextoString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		if (bytes.length == 0) {
			return "";
		}
		StringBuilder string = new StringBuilder(bytes.length * 3);

		for (byte b : bytes) {
			byte hi = (byte) ((b >> 4) & 0x0f);
			byte lo = (byte) (b & 0x0f);

			string.append('%').append(HEX[hi]).append(HEX[lo]);
		}
		return string.toString();
	}
	
	protected static byte[] PeerId() {
		Random rand = new Random(System.currentTimeMillis());
		byte[] peerId = new byte[20];
                
		for (int i = 0; i < 20; ++i) {
			peerId[i] = (byte) ('A' + rand.nextInt(26));
		}
		return peerId;
	}

	public static boolean[] bitfieldToBoolArray(byte[] bitfield, int numPieces) {
		if (bitfield == null)
			return null;
		else {
			boolean[] retArray = new boolean[numPieces];
			for (int i = 0; i < retArray.length; i++) {
				int byteIndex = i / 8;
				int bitIndex = i % 8;

				if (((bitfield[byteIndex] << bitIndex) & 0x80) == 0x80)
					retArray[i] = true;
				else
					retArray[i] = false;

			}
			return retArray;
		}
	}

	public static byte[] boolToBitfieldArray(boolean[] verifiedPieces) {
		int length = verifiedPieces.length / 8;

		if (verifiedPieces.length % 8 != 0) {
			++length;
		}

		int index = 0;
		byte[] bitfield = new byte[length];

		for (int i = 0; i < bitfield.length; ++i) {
			for (int j = 7; j >= 0; --j) {

				if (index >= verifiedPieces.length) {
					return bitfield;
				}

				if (verifiedPieces[index++]) {
					bitfield[i] |= (byte) (1 << j);
				}
			}
		}
		return bitfield;
	}
	
	public static boolean[] checkPieces(TorrentInfo torrentInfo, File outputFile) throws IOException {

		int numPieces = torrentInfo.piece_hashes.length;
		int pieceLength = torrentInfo.piece_length;
		int fileLength = torrentInfo.file_length;
		ByteBuffer[] pieceHashes = torrentInfo.piece_hashes;
		int lastPieceLength = fileLength % pieceLength == 0 ? pieceLength : fileLength % pieceLength;

		byte[] piece = null;
		boolean[] verifiedPieces = new boolean[numPieces];

		for (int i = 0; i < numPieces; i++) {
			if (i != numPieces - 1) {
				piece = new byte[pieceLength];
				piece = readFile(i, 0, pieceLength, torrentInfo, outputFile);
			} else {
				piece = new byte[lastPieceLength];
				piece = readFile(i, 0, lastPieceLength, torrentInfo, outputFile);
			}
			
			
			
			if (Controller.verifySHA1(piece, pieceHashes[i], i)) {
				verifiedPieces[i] = true;
				RUBTClient.log("Verified piece " + i);
			}
		}
		
		for(int i = 0; i < verifiedPieces.length; i++){
			if(verifiedPieces[i] != false){
				
				if(torrentInfo.file_length % torrentInfo.piece_length != 0 && i == torrentInfo.piece_hashes.length -1){
					RUBTClient.addAmountDownloaded(torrentInfo.file_length % torrentInfo.piece_length);
				}
				else {
					RUBTClient.addAmountDownloaded(torrentInfo.piece_length);
				}
			}
			
		}
		
		return verifiedPieces;
	}
	
	public static byte[] readFile(int index, int offset, int length, TorrentInfo torrentInfo, File outputFile) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(outputFile, "r");
		byte[] data = new byte[length];
		
		raf.seek(torrentInfo.piece_length * index + offset);
		raf.read(data);
		raf.close();
		
		return data;
	}
}
