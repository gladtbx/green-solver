package za.ac.sun.cs.green.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Variable;
//import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.parser.klee.KleeOutputParser;
import za.ac.sun.cs.green.store.Store;
import za.ac.sun.cs.green.store.memory.MemoryStore;
import za.ac.sun.cs.green.util.Configuration;

public class GreenServer {

	private static Green green = null;

	private static Logger log = null;
	
	private static Boolean cacheMissed = false;

	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("There should be an argument: A path to properties file\n");
			System.exit(1);
		}
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(args[0]));
		} catch (Exception e) {
			System.err.println("Problem loading .properties file");
			System.exit(1);
		}

		green = new Green(p);
		log = green.getLog();
		new Configuration(green, p).configure();
		green.setStore(new MemoryStore(green, p));
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		BufferedReader input = null;
		PrintStream output = null;
		while(true){
			try {
				serverSocket = new ServerSocket(9408);
				boolean isRunning = true;
				while (isRunning) {
					log.info("Waiting for a client to connect...");
					clientSocket = serverSocket.accept();
					log.info("Connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getLocalPort());
					input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					output = new PrintStream(clientSocket.getOutputStream());
					while (clientSocket.isConnected()) {
						String query = "";
						//We collect variable names and sized from Klee side.
						Map<String,Integer> vss = new Hashtable<String,Integer>();//Variable Sizes.
						Boolean close = false;
						while(true){
							String line = input.readLine();
							if(line == null){
								close = true;
								break;
							}
							if(line.startsWith("ASV")){//ask for variables
								String[] data = line.split("\\s+");
								int ctr = 1;
								while(ctr+1<data.length){
									vss.put(data[ctr], Integer.parseInt(data[ctr+1]));
									ctr+=2;
								}
								continue;
							}

							query += line;
							if(line.equals("(exit)")){
								break;
							}
							if ((query == null) || query.equals("QUIT")) {
								isRunning = false;
								log.info("Closing the client connection and shutting down");
								output.print("OK");
								output.close();
								try {
									input.close();
								} catch (IOException x) {
									log.log(Level.SEVERE, "input.close() failed", x);
								}
								try {
									clientSocket.close();
								} catch (IOException x) {
									log.log(Level.SEVERE, "clientSocket.close() failed", x);
								}
								close = true;
								break;
							}
							if (query.equals("CLOSE")) {
								log.info("Closing the client connection");
								output.print("OK");
								output.close();
								try {
									input.close();
								} catch (IOException x) {
									log.log(Level.SEVERE, "input.close() failed", x);
								}
								try {
									clientSocket.close();
								} catch (IOException x) {
									log.log(Level.SEVERE, "clientSocket.close() failed", x);
								}
								close = true;
								break;
							}
						}
						if(!close){
							output.print(process(query,vss));
						}
						else{
							output.close();
							break;
						}
					}
					green.report();
				}
			} catch (IOException x) {
				log.log(Level.SEVERE, x.getMessage(), x);
//			} catch (KleeParseException x) {
//				log.log(Level.SEVERE, x.getMessage(), x);
			} finally {
				output.close();
				try {
					input.close();
				} catch (IOException x) {
					log.log(Level.SEVERE, "input.close() failed", x);
				}
				try {
					clientSocket.close();
				} catch (IOException x) {
					log.log(Level.SEVERE, "clientSocket.close() failed", x);
				}
				try {
					serverSocket.close();
				} catch (IOException x) {
					log.log(Level.SEVERE, "serverSocket.close() failed", x);
				}
			}			
		}
	}

	private static char[] process(String query, Map<String, Integer> vss) {
		String ret="";
		log.info("QUERY: " + query);
		//Gladtbx: added the parser for the query.
		try{
			Instance i = new Instance(green, null, KleeOutputParser.createExpressionKlee(query));
			//VSS is the mapping of variable names to the size of the variable of request
			i.setData("VSS", vss);
			int missedCount = green.getZ3JavaModelMissCount();
			System.out.println(missedCount);
	        Object requestRet = i.request("sat");
	        if(requestRet != null){
	        	if(requestRet instanceof Map<?, ?>){
	        		//vm is the request result
	            	@SuppressWarnings("unchecked")
					Map<String, Object> vm = (Map<String, Object>) requestRet;
	            	//System.out.println("Get Map: " + vm.toString());
	            	//Gladtbx:
	            	//We use the insertion count to detect if the query had a cache hit or miss
	            	//If the insertion count does not change, the query had a hit, thus ret = 1
	            	//Else we had a miss
	            	if(vm.size() != 0){
	            		if(green.getZ3JavaModelMissCount() == missedCount ){
	            			ret = "1";
	            		}else if(green.getZ3JavaModelMissCount() > missedCount){
	            			ret = "2";
	            			missedCount = green.getZ3JavaModelMissCount();
	            		}else{
	            			//Error here, missedCount should only increase
	            			ret = "2";
	            			log.log(Level.SEVERE, "MissedCount form Model Service Decreased! This should not happen!");
	            		}
		            	//The mapping of the variables are attached here.
		            	for(Map.Entry<String,Object> entry : vm.entrySet()){
		            		ret+=" ";
		            		ret+=entry.getKey();
		            		ret+=" ";
		            		int[] vals = (int[]) entry.getValue();
		            		for(int val: vals){
		            			ret+= Integer.toString(val,10);//Transfer each byte in base 10
		            			ret+= "|";
		            		}
		            	}
	            	}
	            	else{
	            		ret = "0";
	            	}
	            	//append the mapping of var and obj 
	        	}else{
	        		if((Boolean)requestRet){
	        			ret = "1";
	        		}
	        		else{
	        			ret = "0";
	        		}
	        	}
	        }
    		System.out.println(ret);
	        return ret.toCharArray();
		}catch(Exception x){
			log.log(Level.SEVERE, "Process String Failed",x);
			System.out.println("Process String Failed");
		}
		return ret.toCharArray();
	}
}
