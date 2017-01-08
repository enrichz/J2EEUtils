package db;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * This class contains methods for DB access
 * You can pass a logger as parameter when initializing, otherwise system.out will be used
 * @author enrico guariento
 *
 */
public class ToolDB {
	
	private static Logger log = Logger.getLogger(ToolDB.class);
	private static ToolDB instance;
	private Connection connection;
	
	private ToolDB(Logger logger) {
		if(logger!=null) {
			log = logger;
		}
		log.info("[ToolDB] ToolDB initialized");
	}
	
	public static ToolDB getInstance() {
		return getInstance(null);
	}
	
	public static ToolDB getInstance(Logger logger) {
		if(instance==null) {
			instance = new ToolDB(logger);
		}
		return instance;
	}
	
	/**
	 * 
	 * @param datasourceName name of the datasource inside application server. ES: jdbc/myds
	 * @return true if the connection is established or was already present
	 */
	public boolean connectWithDataSource(String datasourceName) {
		try {
			if(connection==null) {
				log.info("[ToolDB] Trying to connect with datasource "+datasourceName);
				InitialContext ic = new InitialContext();
				DataSource ds = (DataSource)ic.lookup(datasourceName);
				connection = ds.getConnection();
			}
			else {
				log.info("[ToolDB] Connection already established");
			}
			return true;
		}
		catch(Exception e) {
			log.error("[ToolDB] *** EXCEPTION ***",e);
			return false;
		}
	}

	/**
	 * Always call this method when finished
	 */
	public void disconnect() {
		try {
			log.info("[ToolDB] Trying to disconnect from DB");
			if(connection!=null) {
				connection.close();
			}
		}
		catch(Exception e) {
			log.error("[ToolDB] *** EXCEPTION ***",e);
		}
	}
	
	/**
	 * Generic insert
	 * @param insertQuery sql insert command
	 */
	public void insert(String insertQuery) {
		log.info("[ToolDB] start insert");
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(insertQuery);
			ps.executeUpdate();
		}
		catch(Exception e) {
			log.error("[ToolDB] *** EXCEPTION ***",e);
		}
		log.info("[ToolDB] end insert");
	}
	
}
