package cloud.fogbow.fs.core.plugins.payment;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class StubPostPaidPaymentManager implements PaymentManager {

	public static final String PAYMENT_STATUS_OK = "payment_ok";
	public static final String PAYMENT_STATUS_WAITING = "payment_waiting";
	public static final String PAYMENT_STATUS_DEFAULTING = "payment_defaulting";
	
	private DatabaseManager databaseManager;
	
	public StubPostPaidPaymentManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}
	
	@Override
	public boolean hasPaid(String userId) {
		FinanceUser user = databaseManager.getUserById(userId);
		String paymentStatusString = user.getProperty(FinanceUser.PAYMENT_STATUS_KEY);

		if (paymentStatusString.equals(PAYMENT_STATUS_OK) || paymentStatusString.equals(PAYMENT_STATUS_WAITING)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void startPaymentProcess(String userId) {
		FinanceUser user = databaseManager.getUserById(userId);
		user.setProperty(FinanceUser.PAYMENT_STATUS_KEY, PAYMENT_STATUS_WAITING);
	}
}
