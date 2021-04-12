package cloud.fogbow.fs.core.models;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
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

    public void deduct(ResourceItem resourceItem, Double valueToPayPerTimeUnit, Double timeUsed) 
            throws InvalidParameterException {
        if (valueToPayPerTimeUnit < 0) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_VALUE_TO_PAY, 
                    resourceItem.toString(), valueToPayPerTimeUnit));
        }
        
        if (timeUsed < 0) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_TIME_USED, 
                    resourceItem.toString(), valueToPayPerTimeUnit));
        }
        
	    this.credits -= valueToPayPerTimeUnit*timeUsed; 
	}

    public void addCredits(Double creditsToAdd) throws InvalidParameterException {
        if (creditsToAdd < 0) {
            throw new InvalidParameterException(Messages.Exception.CANNOT_ADD_NEGATIVE_CREDITS);
        }
        
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
