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
	public boolean hasPaid(String userId) {
		FinanceUser user = databaseManager.getUserById(userId);
		String creditsString = user.getProperty(USER_CREDITS);
		
		if (creditsString == null) {
			return true;
		} else {
			Long credits = Long.valueOf(creditsString);
			return (credits > 0);
		}
	}

	@Override
	public void startPaymentProcess(String userId) {
		FinanceUser user = databaseManager.getUserById(userId);
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
	public String getUserFinanceState(String userId, String property) {
		// TODO Implement
		FinanceUser user = databaseManager.getUserById(userId);
		return user.getProperty(USER_CREDITS);
	}
}
