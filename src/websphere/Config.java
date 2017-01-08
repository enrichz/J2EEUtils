package websphere;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for the Resource Environment
 * @author enrico guariento
 *
 */
public class Config {
	private Map<String, String> attributes = null;

	public Config() {
		attributes = new HashMap<String, String>();
	}

	protected void setAttribute(String attributeName, String attributeValue) {
		attributes.put(attributeName, attributeValue);
	}

	public Object getAttribute(String attributeName) {
		return attributes.get(attributeName);
	}
}
