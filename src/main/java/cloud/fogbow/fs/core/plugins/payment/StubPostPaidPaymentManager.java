package cloud.fogbow.fs.core.plugins.payment;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class StubPostPaidPaymentManager implements PaymentManager {

	private DatabaseManager databaseManager;
	
	public StubPostPaidPaymentManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}
	
	@Override
	public boolean hasPaid(String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startPaymentProcess(String userId) {
		// TODO Auto-generated method stub

	}
}
