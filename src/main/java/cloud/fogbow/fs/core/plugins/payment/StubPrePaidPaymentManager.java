package cloud.fogbow.fs.core.plugins.payment;

import cloud.fogbow.fs.core.plugins.PaymentManager;

public class StubPrePaidPaymentManager implements PaymentManager {

	@Override
	public boolean hasPaid(String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startPaymentProcess(String userId) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getUserFinanceState(String userId, String property) {
		// TODO Auto-generated method stub
		return null;
	}

}
