package cloud.fogbow.fs.core.plugins.finance.prepaid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.RasOperation;

public class PrePaidFinancePlugin implements FinancePlugin {
	/**
	 * A textual reference to this plugin. 
	 * Normally used as a user property to set this class
	 * as the one to be used to manage the user.
	 */
	public static final String PLUGIN_NAME = "prepaid";
	/**
	 * The key to use in the configuration property which 
	 * indicates the {@link cloud.fogbow.fs.core.plugins.PaymentManager}
	 * to use.
	 * Required in the configuration file. 
	 */
	public static final String PRE_PAID_PAYMENT_MANAGER = "pre_paid_payment_manager";
	// TODO documentation
	private static final String PRE_PAID_DEFAULT_FINANCE_PLAN = "pre_paid_default_finance_plan";
	/**
	 * The key to use in the configuration property which
	 * indicates the delay between credits deduction attempts.
	 */
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
						databaseManager,
						PropertiesHolder.getInstance().getProperty(PRE_PAID_DEFAULT_FINANCE_PLAN)),
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
	public String getName() {
		return PLUGIN_NAME;
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
	public boolean isAuthorized(SystemUser user, RasOperation operation) {
		// TODO isAuthorized should check if the RasOperation type
		// is related to resource allocation
		return this.paymentManager.hasPaid(user.getId(), user.getIdentityProviderId());
	}

	@Override
	public boolean managesUser(String userId, String provider) {
		List<FinanceUser> financeUsers = this.databaseManager.getRegisteredUsersByPaymentType(PLUGIN_NAME);

		for (FinanceUser financeUser : financeUsers) {
			if (financeUser.getId().equals(userId)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		return this.paymentManager.getUserFinanceState(userId, provider, property);
	}

	@Override
	public void addUser(String userId, String provider, Map<String, String> financeOptions) {
		// TODO validation
		this.databaseManager.registerUser(userId, provider, PLUGIN_NAME, financeOptions);
	}

	@Override
	public void removeUser(String userId, String provider) {
		// TODO validation
		this.databaseManager.removeUser(userId, provider);
	}

	@Override
	public void changeOptions(String userId, String provider, HashMap<String, String> financeOptions) {
		// TODO validation
		this.databaseManager.changeOptions(userId, provider, financeOptions);
	}

	@Override
	public void updateFinanceState(String userId, String provider, HashMap<String, String> financeState) {
		// TODO validation
		this.databaseManager.updateFinanceState(userId, provider, financeState);
	}
}
