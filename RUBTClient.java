
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;
import java.awt.EventQueue;

import GivenTools.*;

public class RUBTClient {

	private static JFrame clientFrame;
	
	public static File file;

	private static JTable infoTable;

	private static JTable peerTable;
	
	private static JTextPane guiLogger;
	
	private static JProgressBar progressBar;
	
	public static TorrentReader torrentReader = new TorrentReader();
	
	public static TorrentInfo torrentInfo = null;
	
	public static Controller manager = null;

	public static File outputFile;
	
	public static File torrentFile;
        
	public int amountUploaded =0;
	
	public int amountDownloaded = 0;

	public static void main(String[] args) {	  
	
		if(args.length != 2){
			System.out.println("Invalid number of command line arguments");
			return;
		}
		String torrentName = args[0];
		String outputName = args[1];

		torrentFile = new File(torrentName);
                outputFile = new File(outputName);

		if(!torrentFile.exists()){
			System.out.println("Cannot find torrent file");
			return;
		}
		
		TorrentReader torrentReader = new TorrentReader();
		

		if((torrentInfo = torrentReader.parseTorrentFile(torrentFile)) == null){
			return;
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RUBTClient window = new RUBTClient();
					window.clientFrame.setVisible(true);
	
					Controller manager = new Controller(torrentInfo, outputFile);
					
					boolean[] checkPieces;
					if(outputFile.exists()){
						
						checkPieces = Utils.checkPieces(torrentInfo, outputFile);
						manager.ourBitfield = checkPieces;
                                                
						boolean haveFullFile = true;
						for(int i = 0; i < manager.ourBitfield.length; i++){
							if(manager.ourBitfield[i] == false){
								haveFullFile = false;
							}
						}
						
						if(haveFullFile) {
							manager.downloadingStatus = false;
							addProgressBar(torrentInfo.piece_hashes.length);
							manager.haveFullFile = true;
						}
						else {
							manager.downloadingStatus = true;
							for(int i=0;i<checkPieces.length;++i){
								if(checkPieces[i]){
									Tracker.downloaded+=torrentInfo.piece_length;
									addProgressBar(1);
								}
							}
						}
					}
					else {
						outputFile.createNewFile();
						manager.ourBitfield = new boolean[torrentInfo.piece_hashes.length];
						Arrays.fill(manager.ourBitfield, false);
					}
					
					manager.init();
					manager.isRunning = true;
					manager.start();
						
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	
	public RUBTClient() {
		initialize();
		setNumSeeds(0);
	}
	
	private static void Start() throws IOException
	{
		File output = new File(torrentInfo.file_name);
		manager = new Controller(torrentInfo, output);
				
		manager.init();
		manager.isRunning = true;
		manager.start();
	}
	
	public static synchronized void addAmountDownloaded(int downloadCompletion)
	{
		infoTable.setValueAt(((Integer)infoTable.getValueAt(2, 1)) + downloadCompletion, 2, 1);
	}
	
	public static synchronized void setAmountDownloaded(int downloadCompletion)
	{
		infoTable.setValueAt(downloadCompletion, 2, 1);
	}
	
	public static synchronized void addAmountUploaded(int uploadCompletion)
	{
		infoTable.setValueAt(((Integer)infoTable.getValueAt(3, 1)) + uploadCompletion, 3, 1);
	}

	public static synchronized void setAmountUploaded(int uploadCompletion)
	{
		infoTable.setValueAt(uploadCompletion, 3, 1);
	}
        
	public static synchronized void setDownloadRate(int downloadRate)
	{
		infoTable.setValueAt(downloadRate+ " Kb/s", 0, 1);
	}
	
	public static synchronized void setUploadRate(int uploadRate)
	{
		infoTable.setValueAt(uploadRate + " Kb/s", 1, 1);
	}
	
	public static synchronized void setNumPeers(int numPeers)
	{
		infoTable.setValueAt(numPeers, 4, 1);
	}
	
        public static synchronized void addPeer(int i, String ip, Peer p, int downloadRate, int uploadRate, boolean chokeStatus)
	{
		if(i>peerTable.getRowCount()-1) {
			( (DefaultTableModel) peerTable.getModel() ).addRow(new Object[]{p, ip, downloadRate, uploadRate, chokeStatus});
		}
		else {
			peerTable.setValueAt(p, i, 0);
			peerTable.setValueAt(ip, i, 1);
			peerTable.setValueAt(downloadRate, i, 2);
			peerTable.setValueAt(uploadRate, i, 3);
			peerTable.setValueAt(chokeStatus, i, 4);
		}
	}
        
	public static synchronized void setNumSeeds(int numSeeds)
	{
		infoTable.setValueAt(numSeeds, 5, 1);
	}
	
	public static synchronized void addSeed()
	{
		infoTable.setValueAt(((Integer)infoTable.getValueAt(5, 1)) + 1, 5, 1);
	}
	
	public static synchronized void log(String text)
	{
		guiLogger.setText(guiLogger.getText() +text + "\n");
	}
	
	public static synchronized void addProgressBar(int progress)
	{
		progressBar.setValue(progressBar.getValue() + progress);
	}

	public static synchronized void toggleProgressBarLoading()
	{
		progressBar.setIndeterminate(!progressBar.isIndeterminate());
	}
	
	public static synchronized void DownRate(Peer p, int downRate) {
		for(int j = 0; j < peerTable.getRowCount(); ++j) {
			Object obj1 = getData(peerTable, j, 0);
			if(obj1 == p) {
				peerTable.setValueAt(downRate, j, 2);
			}
		}
	}
	
	public static synchronized void updatePeerChokeStatus(Peer p, boolean choke) {
		for(int j = 0; j < peerTable.getRowCount(); ++j) {
			Object obj1 = getData(peerTable, j, 0);
			
			if(obj1!= null)
			{
			if(p.equals(obj1)) {
				if(choke)
				{
					peerTable.setValueAt(true, j, 4);
				}
				else
				{
					peerTable.setValueAt(false, j, 4);
				}
			}
			
			}
		}
	}
	
        public static synchronized void UpRate(Peer p, int d) {
		for(int j = 0; j < peerTable.getRowCount(); ++j) {
			Object obj1 = getData(peerTable, j, 0);
			if(obj1 == p) {
				peerTable.setValueAt(d, j, 3);
			}
		}
	}
        
	@SuppressWarnings("serial")
	private void initialize() {
		clientFrame = new JFrame();
		clientFrame.setTitle("Roland's Tracker Thing");
		clientFrame.setBounds(100, 100, 744, 559);
		clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		clientFrame.getContentPane().setLayout(null);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(10, 11, 461, 445);
		clientFrame.getContentPane().add(tabbedPane);
		
		infoTable = new JTable();
		infoTable.setFillsViewportHeight(true);
		infoTable.setBorder(null);
		infoTable.setFont(new Font("Tahoma", Font.PLAIN, 15));
		infoTable.setShowVerticalLines(false);
		infoTable.setModel(new DefaultTableModel(
			new Object[][] {
				
				{"Download Rate:", null},
				{"Upload Rate:", null},
				{"Downloaded:", new Integer(0)},
				{"Uploaded:", new Integer(0)},
				{"Peers:", null},
				{"Seeds:", "0"},
			},
			new String[] {
				"Description", "New column"
			}
		) {
			boolean[] columnEditables = new boolean[] {
				false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		infoTable.getColumnModel().getColumn(0).setPreferredWidth(96);
		tabbedPane.addTab("Information", null, infoTable, null);
		peerTable = new JTable();
		peerTable.setFont(new Font("Tahoma", Font.PLAIN, 14));
		peerTable.setBorder(null);
		peerTable.setModel(new DefaultTableModel(
			new Object[][] {
				{"Peer ID", "IP", "Download Rate", "Upload Rate", "Choke Status"},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
			},
			new String[] {
				"Peer ID", "IP", "Download Rate", "Upload Rate", "Choke Status"
			}
		) {
			boolean[] columnEditables = new boolean[] {
				true, true, false, true, true
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		peerTable.getColumnModel().getColumn(0).setPreferredWidth(140);
		peerTable.getColumnModel().getColumn(1).setPreferredWidth(81);
		peerTable.getColumnModel().getColumn(2).setPreferredWidth(94);
		tabbedPane.addTab("Peer Information", null, peerTable, null);
		
		progressBar = new JProgressBar();
		progressBar.setMaximum(55);
		progressBar.setBounds(10, 486, 398, 23);
		clientFrame.getContentPane().add(progressBar);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(481, 50, 237, 406);
		clientFrame.getContentPane().add(scrollPane);
		
		guiLogger = new JTextPane();
		guiLogger.setEditable(false);
		scrollPane.setViewportView(guiLogger);
		
		JLabel lblLogger = new JLabel("Logger:");
		lblLogger.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblLogger.setBounds(498, 15, 46, 14);
		clientFrame.getContentPane().add(lblLogger);
		
		JLabel lblProgress = new JLabel("Progress:");
		lblProgress.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblProgress.setBounds(10, 467, 61, 23);
		clientFrame.getContentPane().add(lblProgress);
		
		JLabel lblProgressComplete = new JLabel("Complete!");
		lblProgress.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblProgress.setBounds(10, 467, 61, 23);
		
	}

	public void parseTorrent(File torrentFile) {
		
		if((torrentInfo = torrentReader.parseTorrentFile(torrentFile)) == null){
			return;
                }
	}
        
	public static Object getData(JTable table, int row_index, int col_index){
		  return table.getModel().getValueAt(row_index, col_index);
	}

}