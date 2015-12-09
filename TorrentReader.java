import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import GivenTools.*;

public class TorrentReader {
	
	public TorrentReader() {}

	public TorrentInfo parseTorrentFile(File torrentFile) {
		try {
			DataInputStream dataInputStream = new DataInputStream(new FileInputStream(torrentFile));
			long fSize = torrentFile.length();

			if (fSize > Integer.MAX_VALUE || fSize < Integer.MIN_VALUE) {
				dataInputStream.close();
				throw new IllegalArgumentException(fSize + " is too large a torrent filesize for this program to handle");
			}

			byte[] torrentData = new byte[(int)fSize];
			dataInputStream.readFully(torrentData);
			TorrentInfo torrentInfo = new TorrentInfo(torrentData);

			dataInputStream.close();
			
			return torrentInfo;
		} catch (FileNotFoundException e) {
			System.err.println(e);
			return null;
		} catch (IOException e) {
			System.err.println(e);
			return null;
		} catch (IllegalArgumentException e) {
			System.err.println(e);
			return null;
		} catch (BencodingException e) {
			System.err.println(e);
			return null;
		}
	}
}