package ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Utility class for SSH (utilizes the library JSHC)
 * You can pass a logger as parameter when initializing, otherwise system.out will be used
 * @author enrico guariento
 */
public class ToolSSH {
	
	private static Logger log = Logger.getLogger(ToolSSH.class);
	private static ToolSSH instance;
	
	public static final String STD_OUT = "out";
	public static final String STD_ERR = "err";
	public static final String STATUS = "status"; // OK, KO
	public static final String MESSAGGIO_STATUS = "messaggio";
	private Session session;
	
	private ToolSSH(Logger logger) {
		if(logger!=null) {
			log = logger;
		}
		log.info("[ToolSSH] ToolCryptography initialized");
	}
	
	public static ToolSSH getInstance() {
		return getInstance(null);
	}
	
	public static ToolSSH getInstance(Logger logger) {
		if(instance==null) {
			instance = new ToolSSH(logger);
		}
		return instance;
	}
	
	/**
	 * Makes the login with the specified credentials
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @return true if everything is ok, false otherwise
	 */
	public boolean login(String hostname, int port, String username, String password) {
		// Create a session
		JSch jsch = new JSch();
		Properties prop = new Properties();
		prop.put("StrictHostKeyChecking", "no");
		
		boolean toReturn = false;
		try {
			// If there is already a connection open, close it
			disconnect();
			// Open a new session
			log.debug("[ToolSSH] Trying to connect: "+username+"@"+hostname.trim()+":"+port);
			session = jsch.getSession(username, hostname.trim(), port);
			session.setConfig(prop);
		    session.setPassword(password);
		    session.connect();
		    toReturn = true;
		} catch (Exception e) {
			log.error("[ToolSSH] *** EXCEPTION ***",e);
		}
		return toReturn;
	}
	
	/**
	 * Executes a not interactive command (or multiple commands separated by \n)
	 * @param command command
	 * @return Map with standard out, standard error and status messages
	 * with keys ToolSSH.STD_OUT ToolSSH.STD_ERR ToolSSH.STATUS and ToolSSH.MESSAGGIO_STATUS
	 */
	public Map<String,String> executeCommand(String command) {
		// initialize the map to return
		Map<String,String> toReturn = new HashMap<String, String>();
		toReturn.put(STD_ERR, "");
		toReturn.put(STD_OUT, "");
		toReturn.put(STATUS, "");
		toReturn.put(MESSAGGIO_STATUS, "");
		
		if(session.isConnected()) {
			Channel channel = null;
			try {
				channel = session.openChannel("exec");
				log.debug("[ToolSSH] Connection channel open");
			} catch (Exception e) {
				String messaggio = "[ToolSSH] 2: Error: Opening channel failed";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
			// Command to execute
			((ChannelExec)channel).setCommand(command);
			
			// INPUT STREAM (directly set the command)
		    channel.setInputStream(null);
		    
		    // Prepare the streams for errors and outputs
		    InputStream in = null;
		    InputStream err = null;
			try {
				in = channel.getInputStream();
				err = ((ChannelExec)channel).getErrStream();
			} catch (IOException e) {
				String messaggio = "[ToolSSH] 3: Error: I/O";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
		    StringBuilder outStringBuilder = new StringBuilder();
		    StringBuilder errStringBuilder = new StringBuilder();
		    
		    // Connecting...
		    try {
				channel.connect();
			} catch (JSchException e) {
				String messaggio = "[ToolSSH] 4: Error: Failed connection";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
		    
			try {
			    // leggo i risultati
			    byte[] tmp = new byte[1024];
			    byte[] tmp1 = new byte[1024];
			    while(true) {
			    	boolean finito = false;
			        while(in.available()>0) {
			        	int i = in.read(tmp, 0, 1024);
			        	if(i < 0) break;
			        	outStringBuilder.append(new String(tmp, 0, i));
			        }
			        while(err.available() > 0) {
			        	int i = err.read(tmp1, 0, 1024);
			        	if(i < 0) break;
			        	errStringBuilder.append(new String(tmp1, 0, i));
			        }
			        if(channel.isClosed()){ 
			        	finito = true;
			        }
			        if(finito) break;
			        // This is to give the system the time to generate the output
			        try{ 
			        	Thread.sleep(1000);
			        } catch(Exception ee) {}
			    }
			} catch (IOException e) {
				String messaggio = "[ToolSSH] 3: Error: I/O";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
			
		    // Disconnect the channel
		    channel.disconnect();
		    log.debug("[ToolSSH] Connection to the channel closed");
		    // Fill the map with the data to return
		    toReturn.put(STD_OUT, outStringBuilder.toString());
		    toReturn.put(STD_ERR, errStringBuilder.toString());
		    toReturn.put(STATUS, "OK");
		    String messaggio = "0";
		    log.info(messaggio);
		    toReturn.put(MESSAGGIO_STATUS, messaggio);
		}
		return toReturn;
	}
	
	/**
	 * Execute a not interactive command with sudo
	 * @param command string of the command (do not send sudo with it)
	 * @param sudoPassword root password
	 * @return @return Map with standard out, standard error and status messages
	 * with keys ToolSSH.STD_OUT ToolSSH.STD_ERR ToolSSH.STATUS and ToolSSH.MESSAGGIO_STATUS
	 */
	public Map<String,String> executeSudoCommand(String command, String sudoPassword) {
		// initialize the map to return
		Map<String,String> toReturn = new HashMap<String, String>();
		toReturn.put(STD_ERR, "");
		toReturn.put(STD_OUT, "");
		toReturn.put(STATUS, "");
		toReturn.put(MESSAGGIO_STATUS, "");
		
		if(session.isConnected()) {
			Channel channel = null;
			try {
				channel = session.openChannel("exec");
				log.debug("[ToolSSH] Connection channel open");
			} catch (Exception e) {
				String messaggio = "[ToolSSH] 2: Error: Opening channel failed";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
			// Set the command to execute
			((ChannelExec)channel).setPty(true);
			((ChannelExec)channel).setCommand("sudo -S -p '' "+command);
			
			// INPUT STREAM (set the command directly)
		    channel.setInputStream(null);
		    
		    // Prepare the streams for errors and outputs
		    InputStream in = null;
		    InputStream err = null;
		    OutputStream out = null;
			try {
				in = channel.getInputStream();
				err = ((ChannelExec)channel).getErrStream();
				out = channel.getOutputStream();
			} catch (IOException e) {
				String messaggio = "[ToolSSH] 3: Error: I/O";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
		    StringBuilder outStringBuilder = new StringBuilder();
		    StringBuilder errStringBuilder = new StringBuilder();
		    
		    // Connecting...
		    try {
				channel.connect();
				// send the sudo password
				out.write((sudoPassword+"\n").getBytes());
			    out.flush();
			} catch (Exception e) {
				String messaggio = "[ToolSSH] 4: Error: Connection failed";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
		    
			try {
			    // read the results
			    byte[] tmp = new byte[1024];
			    byte[] tmp1 = new byte[1024];
			    while(true) {
			    	boolean finito = false;
			        while(in.available()>0) {
			        	int i = in.read(tmp, 0, 1024);
			        	if(i < 0) break;
			        	outStringBuilder.append(new String(tmp, 0, i));
			        }
			        while(err.available() > 0) {
			        	int i = err.read(tmp1, 0, 1024);
			        	if(i < 0) break;
			        	errStringBuilder.append(new String(tmp1, 0, i));
			        }
			        if(channel.isClosed()){ 
			        	finito = true;
			        }
			        if(finito) break;
			        // This is to give the system the time to generate the output
			        try{ 
			        	Thread.sleep(1000);
			        } catch(Exception ee) {}
			    }
			} catch (IOException e) {
				String messaggio = "[ToolSSH] 3: Error: I/O";
				log.error(messaggio);
				log.error("[ToolSSH] *** EXCEPTION ***",e);
				toReturn.put(STATUS, "KO");
				toReturn.put(MESSAGGIO_STATUS, messaggio);
				return toReturn;
			}
			
		    // Disconnect the channel
		    channel.disconnect();
		    log.debug("[ToolSSH] Connection to the channel closed");
		    // Fill the map with the data to return
		    toReturn.put(STD_OUT, outStringBuilder.toString());
		    toReturn.put(STD_ERR, errStringBuilder.toString());
		    toReturn.put(STATUS, "OK");
		    String messaggio = "0";
		    log.info(messaggio);
		    toReturn.put(MESSAGGIO_STATUS, messaggio);
		}
		return toReturn;
	}
	
	/**
	 * Disconnect the session. Always call this when finished
	 */
	public void disconnect() {
		if(session.isConnected()) {
			session.disconnect();
			log.info("Session terminated");
		}
	}
}
