package org.minima;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.minima.objects.base.MiniString;
import org.minima.system.Main;
import org.minima.system.commands.Command;
import org.minima.system.params.GeneralParams;
import org.minima.system.params.GlobalParams;
import org.minima.system.params.TestParams;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class Minima {

	public static void main(String[] zArgs) {
		
		MinimaLogger.log("**********************************************");
		MinimaLogger.log("*  __  __  ____  _  _  ____  __  __    __    *");
		MinimaLogger.log("* (  \\/  )(_  _)( \\( )(_  _)(  \\/  )  /__\\   *");
		MinimaLogger.log("*  )    (  _)(_  )  (  _)(_  )    (  /(__)\\  *");
		MinimaLogger.log("* (_/\\/\\_)(____)(_)\\_)(____)(_/\\/\\_)(__)(__) *");
		MinimaLogger.log("*                                            *");
		MinimaLogger.log("**********************************************");
		MinimaLogger.log("Welcome to Minima "+GlobalParams.MINIMA_VERSION+" - for assistance type help. Then press enter.");
		
		//Set the main configuration folder
		File conf = new File(System.getProperty("user.home"),".minima");
		GeneralParams.CONFIGURATION_FOLDER = conf.getAbsolutePath(); 
		
		//Daemon mode
		boolean daemon = false;
		
		int arglen 	= zArgs.length;
		if(arglen > 0) {
			int counter	=	0;
			while(counter<arglen) {
				String arg 	= zArgs[counter];
				counter++;
				
				if(arg.equals("-port")) {
					GeneralParams.MINIMA_PORT = Integer.parseInt(zArgs[counter++]);
				
				}else if(arg.equals("-conf")) {
					GeneralParams.CONFIGURATION_FOLDER = zArgs[counter++];
				
				}else if(arg.equals("-daemon")) {
					daemon = true;
					
				}else if(arg.equals("-private")) {
					GeneralParams.PRIVATE_NETWORK 	= true;
				
				}else if(arg.equals("-automine")) {
					GeneralParams.AUTOMINE 			= true;
				
				}else if(arg.equals("-noautomine")) {
					GeneralParams.AUTOMINE 			= false;
				
				}else if(arg.equals("-clean")) {
					GeneralParams.CLEAN 			= true;
					
				}else if(arg.equals("-genesis")) {
					GeneralParams.CLEAN 			= true;
					GeneralParams.PRIVATE_NETWORK 	= true;
					GeneralParams.GENESIS 			= true;
					GeneralParams.AUTOMINE 			= true;
					
				}else if(arg.equals("-test")) {
					GeneralParams.PRIVATE_NETWORK 	= true;
					TestParams.setTestParams();
				
				}else {
					MinimaLogger.log("Unknown parameter : "+arg);
					System.exit(1);
				}
			}
		}
		
		//Main handler..
		Main main = new Main();

		//Daemon mode has no stdin input
		if(daemon) {
	    	MinimaLogger.log("Daemon Started..");
			
			//Loop while running..
			while (!main.isShutdownComplete()) {
                try {Thread.sleep(1000);} catch (InterruptedException e) {}
            }
			
			//All done..
			System.exit(0);
	    }
		
		//Listen for input
		InputStreamReader is    = new InputStreamReader(System.in, MiniString.MINIMA_CHARSET);
	    BufferedReader bis      = new BufferedReader(is);
	    
	    //Loop until finished..
	    while(main.isRunning()){
	        try {
	            //Get a line of input
	            String input = bis.readLine();
	            
	            //Check valid..
	            if(input!=null && !input.equals("")) {
	            	//trim it..
	            	input = input.trim();
	            	
	            	//Run it..
	            	JSONArray res = Command.runMultiCommand(input);
	            	
	            	//Print it out 
	            	System.out.println(MiniFormat.JSONPretty(res));
	            	
	                //Is it quit..
	            	boolean quit = false;
	            	Iterator<JSONObject> it = res.iterator();
	            	while(it.hasNext()) {
	            		JSONObject json = it.next();
	            		if(json.get("command").equals("quit")) {
	            			quit = true;
	            			break;
	            		}
	            	}
	            	
	            	if(quit) {
	            		break;
	            	}
	            }
	            
	        } catch (Exception ex) {
	            MinimaLogger.log(ex);
	        }
	    }
	    
	    //Cross the streams..
	    try {
	        bis.close();
	        is.close();
	    } catch (IOException ex) {
	    	MinimaLogger.log(""+ex);
	    }
	    
	    MinimaLogger.log("Bye bye..");
	}
	
}