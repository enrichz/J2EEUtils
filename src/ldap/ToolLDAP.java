package ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * This class contains methods for LDAP access
 * You can pass a logger as parameter when initializing, otherwise system.out will be used
 * @author enrico guariento
 *
 */
public class ToolLDAP {

	private static Logger log = Logger.getLogger(ToolLDAP.class);
	private static ToolLDAP instance;
	private LdapDataSource dataSource;
	
	private ToolLDAP(String host, int port, String userDn, String userPassword, Logger logger) {
		if(logger!=null) {
			log = logger;
		}
		try {
			dataSource = new LdapDataSource(host, port, userDn, userPassword);
			log.info("[ToolLDAP] ToolLDAP initialized");
		} catch (Exception e) {
			log.error("[ToolLDAP] *** EXCEPTION ***",e);
		}
	}
	
	public static ToolLDAP getInstance(String host, int port, String userDn, String userPassword) {
		return getInstance(host, port, userDn, userPassword, null);
	}
	
	public static ToolLDAP getInstance(String host, int port, String userDn, String userPassword, Logger logger) {
		if(instance==null) {
			instance = new ToolLDAP(host, port, userDn, userPassword, logger);
		}
		return instance;
	}

	/**
	 * Generic method for search
	 * @param searchDn root to start for the search
	 * @param scope scope of the search
	 * @param filter ie: (uid=xxxx)
	 * @return Map: key=attribute name, value=array with attribute values
	 */
	public Map<String, String[]> genericSearch(String searchDn, SearchScope scope, String filter) {
		Map<String, String[]> toReturn = new HashMap<String, String[]>();
		LDAPConnection connection = null;
		log.info("[ToolLDAP] Search from "+searchDn+" with scope "+scope.getName()+" and filter "+filter);
		try {
			// Get the connection from the datasource
			connection = dataSource.getConnection();
			SearchResult searchResult = connection.search(searchDn, scope, filter);
			if(searchResult!=null && searchResult.getSearchEntries()!=null && searchResult.getSearchEntries().size()>0) {
				// There is always one result
				SearchResultEntry sre = searchResult.getSearchEntries().get(0);
				log.debug("[ToolLDAP] Found attribute "+sre.toString());
				for(Attribute a : sre.getAttributes()) {
					String[] values = a.getValues();
					toReturn.put(a.getName(), values);
					// print the values for debug
					for (int i = 0; i < values.length; i++) {
						log.debug("[ToolLDAP] Value: "+values[i]);
					}
				}
			}
		} catch (Exception e) {
			log.error("[ToolLDAP] *** EXCEPTION ***",e);
		}
		finally {
			dataSource.destroyConnection(connection);
		}
		return toReturn;
	}
	
	/**
	 * This modifies a single value attribute, or adds it if not present
	 * @param entryDN dn of the entry to modify
	 * @param attributeName name of the attribute to modify
	 * @param newValue new value of the attribute
	 * @return restituisce true if everything is ok, false otherwise
	 */
	public boolean replaceOrAddAttributeSingleValue(String entryDN, String attributeName, String newValue) {
		boolean toReturn = false;
		LDAPConnection connection = null;
		try {
			// get the connection from the datasource
			connection = dataSource.getConnection();
			Modification modifica = new Modification(ModificationType.REPLACE, attributeName, newValue);
			LDAPResult result = connection.modify(entryDN, modifica);
			if(result!=null && result.getResultCode().equals(ResultCode.SUCCESS)) {
				toReturn = true;
			}
		} catch (Exception e) {
			log.error("[ToolLDAP] *** EXCEPTION ***",e);
		}
		finally {
			dataSource.destroyConnection(connection);
		}
		return toReturn;
	}
	
	/**
	 * Binds a user
	 * @param username dn of the user to bind
	 * @param password password of the user
	 * @return OK or message exception
	 */
	public String bindUser(String userDn, String password) {
		String toReturn = "OK";
		LDAPConnection connection = null;
		try {
			connection = dataSource.bindUser(userDn, password);
		} catch (Exception e) {
			log.error("[ToolLDAP] *** EXCEPTION ***",e);
			toReturn = e.toString();
		}
		finally {
			dataSource.destroyConnection(connection);
		}
		return toReturn;
	}

	/**
	 * This method modifies (REPLACE) the attributes sent in the map
	 * @param entryDN dn of the entry to modify
	 * @param modifications Map: key=attribute name, value=array with attribute new values
	 * @return true if everything is ok, false otherwise
	 */
	public boolean replaceOrAddAttributesMultiValue(String entryDN, Map<String, String[]> modifications) {
		boolean toReturn = false;
		LDAPConnection connection = null;
		try {
			// get the connection from the datasource
			connection = dataSource.getConnection();
			if(modifications.size()>0) {
				List<Modification> modifiche = new ArrayList<Modification>();
				for(String attribute : modifications.keySet()) {
					if(modifications.get(attribute)!=null && modifications.get(attribute).length>0) {
						ASN1OctetString[] temp = new ASN1OctetString[modifications.get(attribute).length];
						for (int i = 0; i < modifications.get(attribute).length; i++) {
							temp[i] = new ASN1OctetString(modifications.get(attribute)[i]);
						}
						Modification modifica = new Modification(ModificationType.REPLACE, attribute, temp);
						modifiche.add(modifica);
					}
				}
				
				// apply the modifications
				if(modifiche.size()>0) {
					LDAPResult result = connection.modify(entryDN, modifiche);
					if(result!=null && result.getResultCode().equals(ResultCode.SUCCESS)) {
						toReturn = true;
					}
				}
			}
		} catch (Exception e) {
			log.error("[ToolLDAP] *** EXCEPTION ***",e);
		}
		finally {
			dataSource.destroyConnection(connection);
		}
		return toReturn;
	}

}
