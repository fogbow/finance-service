package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.fs.core.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;

public class PostPaidFinancePlugin implements FinancePlugin {
	// TODO documentation
	public static final String PLUGIN_NAME = "postpaid";
	// TODO documentation
	public static final String POST_PAID_PAYMENT_MANAGER = "post_paid_payment_manager";
	// TODO documentation
	public static final String INVOICE_WAIT_TIME = "invoice_wait_time";

	private Thread paymentThread;
	private Thread stopServiceThread;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient; 
	private RasClient rasClient;
	private PaymentRunner paymentRunner;
	private StopServiceRunner stopServiceRunner;
	private DatabaseManager databaseManager;
	private long invoiceWaitTime;

	public PostPaidFinancePlugin(DatabaseManager databaseManager) throws ConfigurationErrorException {
		this(databaseManager, new AccountingServiceClient(), new RasClient(),
				PaymentManagerInstantiator.getPaymentManager(
						PropertiesHolder.getInstance().getProperty(POST_PAID_PAYMENT_MANAGER),
						databaseManager),
				Long.valueOf(PropertiesHolder.getInstance().getProperty(INVOICE_WAIT_TIME)));
	}
	
	public PostPaidFinancePlugin(DatabaseManager databaseManager, AccountingServiceClient accountingServiceClient, 
			RasClient rasClient, PaymentManager paymentManager, long invoiceWaitTime) {
		this.accountingServiceClient = accountingServiceClient;
		this.rasClient = rasClient;
		this.databaseManager = databaseManager;
		this.paymentManager = paymentManager;
		this.invoiceWaitTime = invoiceWaitTime;
	}
	
	@Override
	public void startThreads() {
		this.paymentRunner = new PaymentRunner(invoiceWaitTime, databaseManager, accountingServiceClient, paymentManager);
		this.paymentThread = new Thread(paymentRunner);
		
		this.stopServiceRunner = new StopServiceRunner(invoiceWaitTime, databaseManager, paymentManager, rasClient);
		this.stopServiceThread = new Thread(stopServiceRunner);
		
		this.paymentThread.start();
		this.stopServiceThread.start();
		
		// TODO add a wait start
	}

	@Override
	public void stopThreads() {
		this.paymentRunner.stop();
		this.stopServiceRunner.stop();
	}

	@Override
	public boolean isAuthorized(String userId, Map<String, String> operationParameters) {
		return this.paymentManager.hasPaid(userId);
	}

	@Override
	public boolean managesUser(String userId) {
		List<FinanceUser> financeUsers = this.databaseManager.getRegisteredUsersByPaymentType(PLUGIN_NAME); 
		
		for (FinanceUser financeUser : financeUsers) {
			if (financeUser.getId().equals(userId)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String getUserFinanceState(String userId, String property) {
		return this.paymentManager.getUserFinanceState(userId, property);
	}
}
