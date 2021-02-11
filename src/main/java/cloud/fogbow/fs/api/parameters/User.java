package cloud.fogbow.fs.api.parameters;

import java.util.Map;

public class User {
	private String userId;
	private String provider;
	private Map<String, String> financeOptions;
	
	public String getUserId() {
		return userId;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public Map<String, String> getFinanceOptions() {
		return financeOptions;
	}
}
