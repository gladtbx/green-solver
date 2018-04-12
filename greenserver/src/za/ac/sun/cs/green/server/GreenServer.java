package za.ac.sun.cs.green.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Variable;
//import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.parser.klee.KleeOutputParser;
import za.ac.sun.cs.green.store.memory.MemoryStore;
import za.ac.sun.cs.green.util.Configuration;

public class GreenServer {

	private static Green green = null;

	private static Logger log = null;

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
						Boolean close = false;
						while(true){
							String line = input.readLine();
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
							output.print(process(query));
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

	private static char[] process(String query) {
		String ret="";
		log.info("QUERY: " + query);
		//Gladtbx: added the parser for the query.
		try{
			Instance i = new Instance(green, null, KleeOutputParser.createExpressionKlee(query));
	        Object requestRet = i.request("sat");
	        if(requestRet != null){
	        	if(requestRet instanceof Map<?, ?>){
	            	@SuppressWarnings("unchecked")
					Map<String, Object> vm = (Map<String, Object>) requestRet;
	            	//System.out.println("Get Map: " + vm.toString());
	            	if(vm.size() != 0){
		            	ret = "1";
		            	for(Map.Entry<String,Object> entry : vm.entrySet()){
		            		ret+=" ";
		            		ret+=entry.getKey();
		            		ret+=" ";
		            		int[] vals = (int[]) entry.getValue();
		            		for(int val: vals){
		            			ret+= Integer.toString(val,10);//Transfer each byte in base 16
		            			ret+= "|";
		            		}
//		            		System.out.println(ret);
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
	        return ret.toCharArray();
		}catch(Exception x){
			log.log(Level.SEVERE, "Process String Failed",x);
			System.out.println("Process String Failed");
		}
		return ret.toCharArray();
	}
}
