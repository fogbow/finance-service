package cloud.fogbow.fs.core.plugins.payment.prepaid;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class DefaultCreditsManager implements PaymentManager {
	public static final String USER_CREDITS = "USER_CREDITS";
	
    private DatabaseManager databaseManager;
	private RecordUtils recordUtils;
	private String planName;

	public DefaultCreditsManager(DatabaseManager databaseManager, String planName) {
		this.databaseManager = databaseManager;
		this.planName = planName;
		this.recordUtils = new RecordUtils();
	}
	
	public DefaultCreditsManager(DatabaseManager databaseManager, String planName, RecordUtils recordUtils) {
        this.databaseManager = databaseManager;
        this.planName = planName;
        this.recordUtils = recordUtils;
    }
	
	@Override
	public boolean hasPaid(String userId, String provider) throws InvalidParameterException {
	    UserCredits credits = databaseManager.getUserCreditsByUserId(userId, provider);
	    return credits.getCreditsValue() >= 0.0;
	}

	@Override
	public void startPaymentProcess(String userId, String provider, 
	        Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException, InvalidParameterException {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		UserCredits credits = databaseManager.getUserCreditsByUserId(userId, provider);
		FinancePlan plan = databaseManager.getFinancePlan(planName);
		List<Record> records = user.getPeriodRecords();
		
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
		
		databaseManager.saveUserCredits(credits);
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException {
       String propertyValue = "";
        
        if (property.equals(USER_CREDITS)) {
            UserCredits userCredits = databaseManager.getUserCreditsByUserId(userId, provider);
            propertyValue = String.valueOf(userCredits.getCreditsValue());
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
        }
        
        return propertyValue;
	}

    @Override
    public void setFinancePlan(String planName) {
        this.planName = planName;
    }
}
