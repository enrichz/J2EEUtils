package ldap;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * This class is the datasource for LDAP connection
 * @author enrico guariento
 *
 */
public class LdapDataSource {
	
	private String host;
	private Integer port;
	private String userDn;
	private String userPassword;
	
	public LdapDataSource(String host, int port, String userDn, String userPassword) throws LDAPException {
		this.host = host;
		this.port = port;
		this.userDn = userDn;
		this.userPassword = userPassword;
	}
	
	/**
	 * @return single connection to ldap. This does not use the connection pool so you need to close it
	 * @throws LDAPException
	 */
	public LDAPConnection getConnection() throws LDAPException {
		return new LDAPConnection(host, port, userDn, userPassword);
	}
	
	/**
	 * Create a connection with a specified dn. It's the same as getConnection but it uses passed parameters
	 * You can check if the user has LDAP access. Always close the returned  connection
	 * @param userDn dn of the user who needs to be verified
	 * @param password password of the user
	 * @return LDAPConnection
	 * @throws LDAPException
	 */
	public LDAPConnection bindUser(String userDn, String password) throws LDAPException {
		return new LDAPConnection(host, port, userDn, password);
	}
	
	/**
	 * This method closes a connection
	 * @param connection connection to close
	 */
	public void destroyConnection(LDAPConnection connection) {
		if(connection!=null) {
			connection.close();
		}
	}

}
