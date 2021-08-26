package cloud.fogbow.fs.api.parameters;

// TODO documentation
public class User {
	private String userId;
	private String provider;
	private String financePlanName;
	
	public User() {
		
	}
	
    public User(String userId, String provider, String financePlanName) {
		this.userId = userId;
		this.provider = provider;
		this.financePlanName = financePlanName;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public String getFinancePlanName() {
		return financePlanName;
	}
}
