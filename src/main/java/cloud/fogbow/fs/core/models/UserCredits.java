package cloud.fogbow.fs.core.models;

import cloud.fogbow.fs.core.plugins.payment.ResourceItem;

public class UserCredits {

    private String userId;
    private String provider;
    private Double credits;

	public UserCredits(String userId, String provider) {
        this.userId = userId;
        this.provider = provider;
        this.credits = 0.0;
    }

    public void deduct(ResourceItem resourceItem, Double valueToPayPerTimeUnit, Double timeUsed) {
	    this.credits -= valueToPayPerTimeUnit*timeUsed; 
	}

    public void addCredits(Double creditsToAdd) {
        this.credits += creditsToAdd;
    }
    
    public Double getCreditsValue() {
        return credits;
    }

    public String getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }
}
