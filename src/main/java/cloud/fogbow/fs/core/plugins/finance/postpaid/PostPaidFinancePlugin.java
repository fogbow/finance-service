package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;

public class PostPaidFinancePlugin implements FinancePlugin {

	public static final String PLUGIN_NAME = "postpaid";

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
		this.accountingServiceClient = new AccountingServiceClient();
		this.rasClient = new RasClient();
		this.databaseManager = databaseManager;
		
		this.paymentManager = PaymentManagerInstantiator.getPaymentManager(
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POST_PAID_PAYMENT_MANAGER), 
				databaseManager);
		
		this.invoiceWaitTime = Long.valueOf(PropertiesHolder.getInstance().
				getProperty(ConfigurationPropertyKeys.INVOICE_WAIT_TIME));
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
}
