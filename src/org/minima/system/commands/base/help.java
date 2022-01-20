package org.minima.system.commands.base;

import org.minima.system.commands.Command;
import org.minima.system.commands.network.connect;
import org.minima.system.commands.network.disconnect;
import org.minima.system.commands.network.maxima;
import org.minima.system.commands.network.message;
import org.minima.system.commands.network.network;
import org.minima.system.commands.network.rpc;
import org.minima.system.commands.network.sshtunnel;
import org.minima.system.commands.network.webhooks;
import org.minima.system.commands.search.coins;
import org.minima.system.commands.search.keys;
import org.minima.system.commands.search.scripts;
import org.minima.system.commands.search.txpow;
import org.minima.system.commands.txn.txncheck;
import org.minima.system.commands.txn.txnclear;
import org.minima.system.commands.txn.txncreate;
import org.minima.system.commands.txn.txndelete;
import org.minima.system.commands.txn.txnexport;
import org.minima.system.commands.txn.txnimport;
import org.minima.system.commands.txn.txninput;
import org.minima.system.commands.txn.txnlist;
import org.minima.system.commands.txn.txnoutput;
import org.minima.system.commands.txn.txnpost;
import org.minima.system.commands.txn.txnsign;
import org.minima.system.commands.txn.txnstate;
import org.minima.utils.json.JSONObject;

public class help extends Command {

	public help() {
		super("help","Show Help. [] are required. () are optional. Chain multiple commands with ;");
	}
	
	@Override
	public JSONObject runCommand() throws Exception{
		JSONObject ret = getJSONReply();
		
		JSONObject details = new JSONObject();
		
		addCommand(details, new help());
		
		addCommand(details, new status());
		addCommand(details, new printtree());
		addCommand(details, new trace());
		addCommand(details, new automine());
		addCommand(details, new hashtest());
//		addCommand(details, new debugflag());
		
		addCommand(details, new txpow());
		addCommand(details, new coins());
		addCommand(details, new tokens());
		addCommand(details, new keys());
		
		addCommand(details, new newaddress());
		addCommand(details, new send());
		addCommand(details, new balance());
		addCommand(details, new tokencreate());
		
		addCommand(details, new scripts());
		addCommand(details, new runscript());
		addCommand(details, new tutorial());
		
		addCommand(details, new mmrcreate());
		addCommand(details, new mmrproof());
		
		addCommand(details, new txnlist());
		addCommand(details, new txncreate());
		addCommand(details, new txndelete());
		addCommand(details, new txncheck());
		addCommand(details, new txninput());
		addCommand(details, new txnoutput());
		addCommand(details, new txnstate());
		addCommand(details, new txnsign());
		addCommand(details, new txnclear());
		addCommand(details, new txnpost());
		addCommand(details, new txnimport());
		addCommand(details, new txnexport());
		
		addCommand(details, new network());
		addCommand(details, new maxima());
		addCommand(details, new message());
		addCommand(details, new connect());
		addCommand(details, new disconnect());
		addCommand(details, new rpc());
		addCommand(details, new webhooks());
		addCommand(details, new sshtunnel());
		
		addCommand(details, new backup());
		addCommand(details, new restore());
		addCommand(details, new incentivecash());
		
		addCommand(details, new quit());
		
		ret.put("response", details);
		
		return ret;
	}

	
	private void addCommand(JSONObject zDetails, Command zCommand) {
		zDetails.put(getStrOfLength(15,zCommand.getname()), zCommand.getHelp());
	}
	
	public String getStrOfLength(int zDesiredLen, String zString) {
		String ret = new String(zString);
		int len    = ret.length();
		
		//The same or longer
		if(len >= zDesiredLen) {
			return ret.substring(0, zDesiredLen);
		}
		
		//If Shorter add zeros
		for(int i=0;i< zDesiredLen-len;i++) {
			ret = ret.concat(" ");
		}
		
		return ret;
	}
	
	@Override
	public Command getFunction() {
		return new help();
	}

}