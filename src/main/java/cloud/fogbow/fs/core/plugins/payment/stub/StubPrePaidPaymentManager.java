package cloud.fogbow.fs.core.plugins.payment.stub;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class StubPrePaidPaymentManager implements PaymentManager {

	private static final String USER_CREDITS = "credits";
	private DatabaseManager databaseManager;
	
	public StubPrePaidPaymentManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}
	
	@Override
	public boolean hasPaid(String userId, String provider) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		String creditsString = user.getProperty(USER_CREDITS);
		
		if (creditsString == null) {
			return true;
		} else {
			Long credits = Long.valueOf(creditsString);
			return (credits > 0);
		}
	}

	@Override
	public void startPaymentProcess(String userId, String provider, 
	        Long paymentStartTime, Long paymentEndTime) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		String creditsString = user.getProperty(USER_CREDITS);
		
		if (creditsString != null) {
			Long credits = Long.valueOf(creditsString);
			
			if (credits > 0) {
				Long updatedCredits = credits - 1;
				user.setProperty(USER_CREDITS, updatedCredits.toString());
			}
		}
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		return user.getProperty(USER_CREDITS);
	}

	@Override
	public void setFinancePlan(String planName) {
		// do nothing
	}
}
