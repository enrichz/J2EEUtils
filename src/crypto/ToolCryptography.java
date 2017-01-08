package crypto;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import com.ibm.websphere.crypto.KeySetHelper;

/**
 * This class contains methods for cryptography
 * You can pass a logger as parameter when initializing, otherwise system.out will be used
 * @author enrico guariento
 *
 */
public class ToolCryptography {

	private static Logger log = Logger.getLogger(ToolCryptography.class);
	private static ToolCryptography instance;
	
	private ToolCryptography(Logger logger) {
		if(logger!=null) {
			log = logger;
		}
		log.info("[ToolCryptography] ToolCryptography initialized");
	}
	
	public static ToolCryptography getInstance() {
		return getInstance(null);
	}
	
	public static ToolCryptography getInstance(Logger logger) {
		if(instance==null) {
			instance = new ToolCryptography(logger);
		}
		return instance;
	}
	
	/**
	 * Generate a key for AES 128bit. Algorithm used: PBKDF2WithHmacSHA1
	 * @param sale Random string used to create the key. If null, a default string will be used
	 * @param fraseChiave Random string used to create the key. If null, a default string will be used
	 * @return secret key. null if error
	 */
	public SecretKeySpec generateSecretKeyForAES(byte[] sale, String fraseChiave) {
		try {
			if(sale==null || sale.length==0) {
				sale = "Questo e' il salt associato alla chiave".getBytes();
			}
			if(fraseChiave==null || fraseChiave.length()==0) {
				fraseChiave = "Nel mezzo del cammin di nostra vita mi ritrovai per una selva oscura che la diritta via era smarrita.";
			}
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			SecretKey tmp = factory.generateSecret(new PBEKeySpec(fraseChiave.toCharArray(), sale, 30000, 128));
			return new SecretKeySpec(tmp.getEncoded(), "AES");
		}
		catch(Exception e) {
			log.error("[ToolCryptography] *** EXCEPTION ***",e);
		}
		return null;
	}
	
	/**
	 * The cypher is with AES so the key must be 16 byte
	 * @param stringToEncrypt String to crypt
	 * @param keysetName name of the keyset inside websphere
	 * @return byte array of the crypted text. null if exception
	 */
	public static byte[] encryptStringWithWASKeySetAES(String stringToEncrypt, String keysetName) {
		try {
			KeySetHelper ksh = KeySetHelper.getInstance();
			Key encryptionKey = (Key) ksh.getLatestKeyForKeySet(keysetName);
			log.debug("[ToolCryptography] String to crypt: "+stringToEncrypt+" with key of "+encryptionKey.getEncoded().length+" byte");
			Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.ENCRYPT_MODE, encryptionKey);
			return aes.doFinal(stringToEncrypt.getBytes());
		} catch (Exception e) {
			log.error("[ToolCryptography] *** EXCEPTION ***",e);
		}
		return null;
	}
	
	/**
	 * The cypher is with AES so the key must be 16 byte
	 * @param stringToEncrypt String to crypt
	 * @param keystore FileInputStream that identifies the keystore. ie: new FileInputStream("C:\\MyKeyStore.jceks")
	 * @param keystorePassword Password of the keystore in clear
	 * @param keystoreType kind of the keystore (JCEKS,JKS, ecc)
	 * @param keyAlias Name of the key to use inside the keystore
	 * @return byte array of the crypted text. null if exception
	 */
	public byte[] encryptStringWithKeyStoreFileAES(String stringToEncrypt, FileInputStream keystore, String keystorePassword, String keystoreType, String keyAlias) {
		try {
			KeyStore ks = KeyStore.getInstance(keystoreType);
			// get user password and file input stream
			byte[] password = keystorePassword.getBytes();
			char[] passchars = new char[password.length];
			for (int i = 0; i < password.length; i++) {
				passchars[i] = (char)(password[i] & 0xFF);
			}
			ks.load(keystore, passchars);
			keystore.close();
			Key encryptionKey = ks.getKey(keyAlias, passchars);
			
			log.debug("[ToolCryptography] String to crypt: "+stringToEncrypt+" with key of "+encryptionKey.getEncoded().length+" byte");
			Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.ENCRYPT_MODE, encryptionKey);
			return aes.doFinal(stringToEncrypt.getBytes());
		} catch (Exception e) {
			log.error("[ToolCryptography] *** EXCEPTION ***",e);
		}
		return null;
	}
	
	/**
	 * decrypt byte array with AES 128 bit
	 * @param arrayToDecrypt byte array to decrypt
	 * @param keysetName name of the keyset inside websphere
	 * @return decrypted string. null if exception
	 */
	public String decryptByteArrayWithWASKeySetAES(byte[] arrayToDecrypt, String keysetName) {
		try {
			KeySetHelper ksh = KeySetHelper.getInstance();
			Key encryptionKey = (Key) ksh.getLatestKeyForKeySet(keysetName);
			log.debug("[ToolCryptography] Decrypting array of "+arrayToDecrypt.length+" byte with key of "+encryptionKey.getEncoded().length+" byte");
			Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.DECRYPT_MODE, encryptionKey);
			return new String(aes.doFinal(arrayToDecrypt));
		} catch (Exception e) {
			log.error("[ToolCryptography] *** EXCEPTION ***",e);
		}
		return null;
	}
	
	/**
	 * decrypt byte array with AES 128 bit
	 * @param arrayToDecrypt byte array to decrypt
	 * @param keystore FileInputStream of the keystore. ie: new FileInputStream("C:\\MyKeyStore.jceks")
	 * @param keystorePassword Password of the keystore in clear
	 * @param keystoreType kind of the keystore (JCEKS,JKS, ecc)
	 * @param keyAlias Name of the key to use inside the keystore
	 * @return decrypted string. null if exception
	 */
	public String decryptByteArrayWithKeyStoreFileAES(byte[] arrayToDecrypt, FileInputStream keystore, String keystorePassword, String keystoreType, String keyAlias) {
		try {
			KeyStore ks = KeyStore.getInstance(keystoreType);
			// get user password and file input stream
			byte[] password = keystorePassword.getBytes();
			char[] passchars = new char[password.length];
			for (int i = 0; i < password.length; i++) {
				passchars[i] = (char)(password[i] & 0xFF);
			}
			ks.load(keystore, passchars);
			keystore.close();
			Key encryptionKey = ks.getKey(keyAlias, passchars);
			
			log.debug("[ToolCryptography] Decrypting string of "+arrayToDecrypt.length+" byte with key of "+encryptionKey.getEncoded().length+" byte");
			Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.DECRYPT_MODE, encryptionKey);
			return new String(aes.doFinal(arrayToDecrypt));
		} catch (Exception e) {
			log.error("[ToolCryptography] *** ECCEZIONE ***",e);
		}
		return null;
	}
	
	/**
	 * 
	 * @param bytes ie: 73 -113 78 3 -5 56 30 -89 97 61 122 -123 -46 -22 89 61
	 * @return byte array of the corresponding characters
	 */
	public byte[] stringOfBytesToByteArray(String bytes) {
		byte[] toReturn = null;
		if(bytes!=null && bytes.length()>0) {
			String[] temp = bytes.split(" ");
			if(temp!=null && temp.length>0) {
				toReturn = new byte[temp.length];
				for (int i = 0; i < temp.length; i++) {
					toReturn[i] = (byte) Integer.parseInt(temp[i]);
				}
			}
		}
		return toReturn;
	}
	
	/**
	 * 
	 * @param array byte array
	 * @return ie: 73 -113 78 3 -5 56 30 -89 97 61 122 -123 -46 -22 89 61
	 */
	public String byteArrayToString(byte[] array) {
		String toReturn = "";
		for (int i = 0; i < array.length; i++) {
			toReturn += array[i]+" ";
		}
		if(toReturn.length()>0) {
			toReturn = toReturn.substring(0,toReturn.length()-1);
		}
		return toReturn;
	}
	
}
