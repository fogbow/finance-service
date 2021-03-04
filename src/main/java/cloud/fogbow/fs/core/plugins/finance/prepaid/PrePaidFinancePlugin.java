package cloud.fogbow.fs.core.plugins.finance.prepaid;

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

public class PrePaidFinancePlugin implements FinancePlugin {
	// TODO documentation
	public static final String PLUGIN_NAME = "prepaid";
	// TODO documentation
	public static final String PRE_PAID_PAYMENT_MANAGER = "pre_paid_payment_manager";
	// TODO documentation
	public static final String CREDITS_DEDUCTION_WAIT_TIME = "credits_deduction_wait_time";
	
	private Thread paymentThread;
	private Thread stopServiceThread;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient; 
	private RasClient rasClient;
	private PaymentRunner paymentRunner;
	private StopServiceRunner stopServiceRunner;
	private DatabaseManager databaseManager;
	private long creditsDeductionWaitTime;
	private boolean threadsAreRunning;
	
	public PrePaidFinancePlugin(DatabaseManager databaseManager) throws ConfigurationErrorException {
		this(databaseManager, new AccountingServiceClient(), new RasClient(),
				PaymentManagerInstantiator.getPaymentManager(
						PropertiesHolder.getInstance().getProperty(PRE_PAID_PAYMENT_MANAGER),
						databaseManager),
				Long.valueOf(PropertiesHolder.getInstance().getProperty(CREDITS_DEDUCTION_WAIT_TIME)));
	}
	
	public PrePaidFinancePlugin(DatabaseManager databaseManager, AccountingServiceClient accountingServiceClient, 
			RasClient rasClient, PaymentManager paymentManager, long creditsDeductionWaitTime) {
		this.accountingServiceClient = accountingServiceClient;
		this.rasClient = rasClient;
		this.databaseManager = databaseManager;
		this.paymentManager = paymentManager;
		this.creditsDeductionWaitTime = creditsDeductionWaitTime;
		this.threadsAreRunning = false;
	}
	
	@Override
	public void startThreads() {
		if (!this.threadsAreRunning) {
			this.paymentRunner = new PaymentRunner(creditsDeductionWaitTime, databaseManager, 
					accountingServiceClient, paymentManager);
			this.paymentThread = new Thread(paymentRunner);
			
			this.stopServiceRunner = new StopServiceRunner(creditsDeductionWaitTime, databaseManager, 
					paymentManager, rasClient);
			this.stopServiceThread = new Thread(stopServiceRunner);
			
			this.paymentThread.start();
			this.stopServiceThread.start();
			
			while (!this.paymentRunner.isActive());
			while (!this.stopServiceRunner.isActive());
			
			this.threadsAreRunning = true;
		}
	}

	@Override
	public void stopThreads() {
		if (this.threadsAreRunning) {
			this.paymentRunner.stop();
			this.stopServiceRunner.stop();
			
			this.threadsAreRunning = false;
		}
	}

	@Override
	public boolean isAuthorized(String userId, Map<String, String> operationParameters) {
		// I believe this implementation should be more complex
		// and take into account the operation parameters
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
