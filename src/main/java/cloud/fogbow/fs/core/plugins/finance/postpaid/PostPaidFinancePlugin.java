package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class PostPaidFinancePlugin implements FinancePlugin {
	/**
	 * A textual reference to this plugin. 
	 * Normally used as a user property to set this class
	 * as the one to be used to manage the user.
	 */
	public static final String PLUGIN_NAME = "postpaid";
	/**
	 * The key to use in the configuration property which 
	 * indicates the {@link cloud.fogbow.fs.core.plugins.PaymentManager}
	 * to use.
	 * Required in the configuration file. 
	 */
	public static final String POST_PAID_PAYMENT_MANAGER = "post_paid_payment_manager";
	// TODO documentation
	public static final String POST_PAID_DEFAULT_FINANCE_PLAN = "post_paid_default_finance_plan";
	/**
	 * The key to use in the configuration property which
	 * indicates the delay between invoice generation attempts.
	 * Normally the PaymentRunner checks if the user configuration
	 * allows the invoice generation at each attempt.
	 */
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
	private boolean threadsAreRunning;

	public PostPaidFinancePlugin(DatabaseManager databaseManager) throws ConfigurationErrorException {
		this(databaseManager, new AccountingServiceClient(), new RasClient(),
				PaymentManagerInstantiator.getPaymentManager(
						PropertiesHolder.getInstance().getProperty(POST_PAID_PAYMENT_MANAGER),
						databaseManager,
						PropertiesHolder.getInstance().getProperty(POST_PAID_DEFAULT_FINANCE_PLAN)),
				Long.valueOf(PropertiesHolder.getInstance().getProperty(INVOICE_WAIT_TIME)));
	}
	
	public PostPaidFinancePlugin(DatabaseManager databaseManager, AccountingServiceClient accountingServiceClient, 
			RasClient rasClient, PaymentManager paymentManager, long invoiceWaitTime) {
		this.accountingServiceClient = accountingServiceClient;
		this.rasClient = rasClient;
		this.databaseManager = databaseManager;
		this.paymentManager = paymentManager;
		this.invoiceWaitTime = invoiceWaitTime;
		this.threadsAreRunning = false;
	}
	
	@Override
	public String getName() {
		return PLUGIN_NAME;
	}
	
	@Override
	public void startThreads() {
		if (!this.threadsAreRunning) {
			this.paymentRunner = new PaymentRunner(invoiceWaitTime, databaseManager, accountingServiceClient, paymentManager);
			this.paymentThread = new Thread(paymentRunner);
			
			this.stopServiceRunner = new StopServiceRunner(invoiceWaitTime, databaseManager, paymentManager, rasClient);
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
	public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException {
		if (operation.getOperationType().equals(Operation.CREATE)) {
			return this.paymentManager.hasPaid(user.getId(), user.getIdentityProviderId());
		}
		
		return true;
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
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException {
		return this.paymentManager.getUserFinanceState(userId, provider, property);
	}

	@Override
	public void addUser(String userId, String provider, Map<String, String> financeOptions) {
		// TODO validation
		// TODO This operation should have some level of thread protection
		// TODO test
		this.databaseManager.registerUser(userId, provider, PLUGIN_NAME, financeOptions);
	}

	@Override
	public void removeUser(String userId, String provider) throws InvalidParameterException {
		// TODO validation
		// TODO This operation should have some level of thread protection
		// TODO This operation should also remove the user invoices
		// TODO test
		this.databaseManager.removeUser(userId, provider);
	}

	@Override
	public void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException {
		// TODO validation
		// TODO This operation should have some level of thread protection
		// TODO test
		this.databaseManager.changeOptions(userId, provider, financeOptions);
	}

	@Override
	public void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException {
		// TODO test
		// TODO This operation should have some level of thread protection

		for (String invoiceId : financeState.keySet()) {
			Invoice invoice = this.databaseManager.getInvoice(invoiceId);
			invoice.setState(InvoiceState.fromValue(financeState.get(invoiceId)));
			this.databaseManager.saveInvoice(invoice);
		}
	}
}
