package cloud.fogbow.fs.core.plugins;

//TODO documentation
public interface PaymentManager {
	// TODO documentation
	boolean hasPaid(String userId);
	// TODO documentation
	void startPaymentProcess(String userId);
	// TODO documentation
	String getUserFinanceState(String userId, String property);
}
