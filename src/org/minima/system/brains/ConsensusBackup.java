package org.minima.system.brains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.minima.database.MinimaDB;
import org.minima.database.coindb.CoinDBRow;
import org.minima.database.mmr.MMREntry;
import org.minima.database.mmr.MMRSet;
import org.minima.database.txpowdb.TxPOWDBRow;
import org.minima.database.txpowtree.BlockTreeNode;
import org.minima.database.userdb.UserDB;
import org.minima.database.userdb.java.JavaUserDB;
import org.minima.objects.Address;
import org.minima.objects.Coin;
import org.minima.objects.TxPOW;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.system.Main;
import org.minima.system.backup.BackupManager;
import org.minima.system.backup.SyncPackage;
import org.minima.system.backup.SyncPacket;
import org.minima.utils.MinimaLogger;
import org.minima.utils.Streamable;
import org.minima.utils.messages.Message;

public class ConsensusBackup {

	public static final String CONSENSUS_PREFIX  = "CONSENSUSBACKUP_";
	
	public static String CONSENSUSBACKUP_BACKUP  = CONSENSUS_PREFIX+"BACKUP"; 
	
	public static String CONSENSUSBACKUP_RESTORE        = CONSENSUS_PREFIX+"RESTORE"; 
	public static String CONSENSUSBACKUP_RESTOREUSERDB  = CONSENSUS_PREFIX+"RESTOREUSERDB"; 
	public static String CONSENSUSBACKUP_RESTORETXPOW   = CONSENSUS_PREFIX+"RESTORETXPOW"; 
	public static String CONSENSUSBACKUP_RESTORETREEDB  = CONSENSUS_PREFIX+"RESTORETREEDB"; 
	
	public static final String USERDB_BACKUP = "user.minima";
	public static final String SYNC_BACKUP   = "sync.package";
	
	MinimaDB mDB;
	ConsensusHandler mHandler;
	
	public ConsensusBackup(MinimaDB zDB, ConsensusHandler zHandler) {
		mDB = zDB;
		mHandler = zHandler;
	}
	
	private MinimaDB getMainDB() {
		return mDB;
	}
	
	private BackupManager getBackup() {
		return mHandler.getMainHandler().getBackupManager();
	}
	
	public void processMessage(Message zMessage) throws Exception {
		
		if(zMessage.isMessageType(CONSENSUSBACKUP_BACKUP)) {
			//Is this for shut down or just a regular backup..
			boolean shutdown = false;
			if(zMessage.exists("shutdown")) {
				shutdown = zMessage.getBoolean("shutdown");
			}
			
			//Get this as will need it a few times..
			BackupManager backup = mHandler.getMainHandler().getBackupManager();
			
			//First backup the UserDB..
			JavaUserDB userdb = (JavaUserDB) getMainDB().getUserDB();
			File backuser     = backup.getBackUpFile(USERDB_BACKUP);
			BackupManager.writeObjectToFile(backuser, userdb);
			MinimaLogger.log("User backup @ "+backuser.getAbsolutePath());
			
			
			//Now the complete SyncPackage..
			SyncPackage sp = getMainDB().getSyncPackage();
			File backsync  = backup.getBackUpFile(SYNC_BACKUP);
			BackupManager.writeObjectToFile(backsync, sp);
			MinimaLogger.log("Syncpackage backup @ "+backsync.getAbsolutePath());
			
			//Do we shut down..
			if(shutdown) {
				mHandler.getMainHandler().PostMessage(Main.SYSTEM_FULLSHUTDOWN);
			}
			
		}else if(zMessage.isMessageType(CONSENSUSBACKUP_RESTORE)) {
			
			//Get this as will need it a few times..
			BackupManager backup = mHandler.getMainHandler().getBackupManager();
			
			//Check the backups exist..
			File backuser  = backup.getBackUpFile(USERDB_BACKUP);
			File backsync  = backup.getBackUpFile(SYNC_BACKUP);
			
			//Are we ok ?
			if(!backuser.exists()) {
				//Not OK.. start fresh.. 
				MinimaLogger.log("No User backups found.. @ "+backuser.getAbsolutePath());
				mHandler.getMainHandler().PostMessage(Main.SYSTEM_INIT);
				return;
			}
			
			if(!backsync.exists()) {
				//Not OK.. start fresh.. 
				MinimaLogger.log("No SyncPackage found.. @ "+backsync.getAbsolutePath());
				mHandler.getMainHandler().PostMessage(Main.SYSTEM_INIT);
				return;
			}
			
			//Load the user..
			FileInputStream fis = new FileInputStream(backuser);
			DataInputStream dis = new DataInputStream(fis);
			JavaUserDB jdb = new JavaUserDB();
			try {
				jdb.readDataStream(dis);
				dis.close();
				fis.close();
			}catch (Exception exc) {
				exc.printStackTrace();
				//HMM.. not good.. file corrupted.. bug out
				MinimaLogger.log("USER BACKUP FILE CORRUPTED.. not starting up.. :(");
				return;
			}
			
			//Set it..
			getMainDB().setUserDB(jdb);
			
			//Load all the TXPOW
			getMainDB().getTxPowDB().ClearDB();
			File[] txpows = getBackup().getTxPOWFolder().listFiles();
			for(File txf : txpows) {
				TxPOW txpow = loadTxPOW(txf);
				if(txpow!=null) {
					//Add it.. will sort out the txpowdbrow in the next step..
					getMainDB().addNewTxPow(txpow);	
				}
			}
			
			//Load the SyncPackage
			fis = new FileInputStream(backsync);
			dis = new DataInputStream(fis);
			SyncPackage sp = new SyncPackage();
			try {
				sp.readDataStream(dis);
				dis.close();
				fis.close();
			}catch(Exception exc) {
				exc.printStackTrace();
				//HMM.. not good.. file corrupted.. bug out
				MinimaLogger.log("SYNCPACKAGE MMR BACKUP FILE CORRUPTED.. not starting up.. :(");
				return;
			}
			
			//Get the SyncPackage
			MiniNumber casc = sp.getCascadeNode();
			
			//Drill down 
			ArrayList<SyncPacket> packets = sp.getAllNodes();
			for(SyncPacket spack : packets) {
				TxPOW txpow     = spack.getTxPOW();
				MMRSet mmrset   = spack.getMMRSet();
				boolean cascade = spack.isCascade();
				
				//Check all MMR in the unbroken chain.. no point in cascade as may have changed..
				if(mmrset!=null) {
					if(mmrset.getBlockTime().isMoreEqual(casc)) {
						getMainDB().scanMMRSetForCoins(mmrset);
					}
				}
				
				//Add it to the DB..
				BlockTreeNode node = getMainDB().hardAddTxPOWBlock(txpow, mmrset, cascade);
			
				//Is this the cascade block
				if(txpow.getBlockNumber().isEqual(sp.getCascadeNode())) {
					getMainDB().hardSetCascadeNode(node);
				}
			}
			
			//Reset weights
			getMainDB().hardResetChain();
			
			//And Now sort the TXPOWDB
			ArrayList<BlockTreeNode> list = getMainDB().getMainTree().getAsList();
			getMainDB().getTxPowDB().resetAllInBlocks();
			
			//Now sort
			for(BlockTreeNode treenode : list) {
				//Get the Block
				TxPOW txpow = treenode.getTxPow();
				
				//Store it..
				mHandler.getMainHandler().getBackupManager().backupTxpow(txpow);
				
				//get the row..will already be added..
				TxPOWDBRow trow = getMainDB().getTxPowDB().addTxPOWDBRow(txpow);
				
				//What Block
				MiniNumber block = txpow.getBlockNumber();
				
				//Set the details
				trow.setOnChainBlock(true);
				trow.setIsInBlock(true);
				trow.setInBlockNumber(block);
				
				//Now the Txns..
				ArrayList<MiniData> txpowlist = txpow.getBlockTxns();
				for(MiniData txid : txpowlist) {
					trow = getMainDB().getTxPowDB().findTxPOWDBRow(txid);
					if(trow!=null) {
						//Store it..
						mHandler.getMainHandler().getBackupManager().backupTxpow(trow.getTxPOW());
						
						//Set that it is in this block
						trow.setOnChainBlock(false);
						trow.setIsInBlock(true);
						trow.setInBlockNumber(block);
					}
				}
			}
			
			//Get on with it..
			mHandler.getMainHandler().PostMessage(Main.SYSTEM_INIT);
		}
	}
	
	public static TxPOW loadTxPOW(File zTxpowFile) {
		TxPOW txpow    = new TxPOW();
		
		try {
			FileInputStream fis = new FileInputStream(zTxpowFile);
			DataInputStream dis = new DataInputStream(fis);
			txpow.readDataStream(dis);
			dis.close();
			fis.close();
		} catch (Exception e) {
			MinimaLogger.log("ERROR loading TxPOW "+zTxpowFile.getName());
			return null;
		}
		
		return txpow;
	}
	
}
