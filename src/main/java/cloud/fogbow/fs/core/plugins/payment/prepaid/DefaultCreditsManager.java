package cloud.fogbow.fs.core.plugins.payment.prepaid;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class DefaultCreditsManager implements PaymentManager {
	public static final String USER_CREDITS = "USER_CREDITS";
	
	private RecordUtils recordUtils;
	private String planName;
	private InMemoryFinanceObjectsHolder objectHolder;

	public DefaultCreditsManager(InMemoryFinanceObjectsHolder objectHolder, String planName) {
	    this.objectHolder = objectHolder;
        this.planName = planName;
        this.recordUtils = new RecordUtils();
    }
	
    public DefaultCreditsManager(InMemoryFinanceObjectsHolder objectHolder, String planName, RecordUtils recordUtils) {
        this.objectHolder = objectHolder;
        this.planName = planName;
        this.recordUtils = recordUtils;
    }
	
	@Override
	public boolean hasPaid(String userId, String provider) throws InvalidParameterException {
	    UserCredits credits = this.objectHolder.getUserCreditsByUserId(userId, provider);
	    synchronized(credits) {
	        return credits.getCreditsValue() >= 0.0;
	    }
	}

	@Override
	public void startPaymentProcess(String userId, String provider, 
	        Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException, InvalidParameterException {
	    FinanceUser user = this.objectHolder.getUserById(userId, provider);
	    List<Record> records = user.getPeriodRecords();
	    UserCredits credits = this.objectHolder.getUserCreditsByUserId(userId, provider);
	    
	    synchronized(credits) {
	        FinancePlan plan = this.objectHolder.getFinancePlan(planName);
	        
	        for (Record record : records) {
	            ResourceItem resourceItem;
	            Double valueToPayPerTimeUnit;
	            
	            try {
	                resourceItem = recordUtils.getItemFromRecord(record);
	                valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
	            } catch (InvalidParameterException e) {
	                throw new InternalServerErrorException(e.getMessage());
	            }
	            
	            Double timeUsed = recordUtils.getTimeFromRecord(record, 
	                    paymentStartTime, paymentEndTime);
	            
	            credits.deduct(resourceItem, valueToPayPerTimeUnit, timeUsed);
	        }
	        
	        this.objectHolder.saveUserCredits(credits);
	    }
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException {
       String propertyValue = "";
        
        if (property.equals(USER_CREDITS)) {
            UserCredits userCredits = this.objectHolder.getUserCreditsByUserId(userId, provider);
            synchronized(userCredits) {
                propertyValue = String.valueOf(userCredits.getCreditsValue());
            }
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
        }
        
        return propertyValue;
	}

    @Override
    public void setFinancePlan(String planName) throws InvalidParameterException {
        this.objectHolder.getFinancePlan(planName);
        
        this.planName = planName;
    }
}
