package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.Record;

public class PaymentRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(PaymentRunner.class);
	// This string represents the date format 
	// expected by the AccountingService, as
	// specified in the RecordService class. The format
	// is specified through a private field, which 
	// I think should be made public to possible
	// clients of ACCS' API.
	@VisibleForTesting
	static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
	public static final String USER_LAST_BILLING_TIME = "last_billing_time";
	public static final String USER_BILLING_INTERVAL = "billing_interval";
	private DatabaseManager databaseManager;
	private AccountingServiceClient accountingServiceClient;
	private PaymentManager paymentManager;
	private TimeUtils timeUtils;
	
	public PaymentRunner(long invoiceWaitTime, DatabaseManager databaseManager,
			AccountingServiceClient accountingServiceClient, PaymentManager paymentManager) {
		this(invoiceWaitTime, databaseManager, accountingServiceClient, 
				paymentManager, new TimeUtils());
	}
	
	public PaymentRunner(long invoiceWaitTime, DatabaseManager databaseManager,
			AccountingServiceClient accountingServiceClient, PaymentManager paymentManager, 
			TimeUtils timeUtils) {
		this.timeUtils = timeUtils;
		this.sleepTime = invoiceWaitTime;
		this.databaseManager = databaseManager;
		this.accountingServiceClient = accountingServiceClient;
		this.paymentManager = paymentManager;
	}

	private long getUserLastBillingTime(FinanceUser user) {
		String lastBillingTimeProperty = user.getProperty(USER_LAST_BILLING_TIME);
		
		if (lastBillingTimeProperty == null) {
			long billingTime = 0L;
			user.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
			return billingTime;
		}
		
		return Long.valueOf(lastBillingTimeProperty);
	}

	private long getUserBillingInterval(FinanceUser user) {
		return Long.valueOf(user.getProperty(USER_BILLING_INTERVAL));
	}

	@Override
	public void doRun() {
		// get registered users
		List<FinanceUser> registeredUsers = this.databaseManager
				.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);

		// for each user
		for (FinanceUser user : registeredUsers) {
			// if it is billing time
			long billingTime = this.timeUtils.getCurrentTimeMillis();
			long lastBillingTime = getUserLastBillingTime(user);
			long billingInterval = getUserBillingInterval(user);
			
			if (isBillingTime(billingTime, lastBillingTime, billingInterval)) {
				// get records
				try {
					// Maybe move this conversion to ACCSClient
					String invoiceStartDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, lastBillingTime);
					String invoiceEndDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, billingTime); 
					List<Record> userRecords = this.accountingServiceClient.getUserRecords(user.getId(), 
							user.getProvider(), invoiceStartDate, invoiceEndDate);
					// write records on db
					user.setPeriodRecords(userRecords);
					
					// generate invoice
					this.paymentManager.startPaymentProcess(user.getId(), user.getProvider(),
					        lastBillingTime, billingTime);
					
					user.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
				} catch (FogbowException e) {
					LOGGER.error(String.format(Messages.Log.FAILED_TO_GENERATE_INVOICE, user.getId(), e.getMessage()));
				}
			}
		}
		
		checkIfMustStop();
	}

	private boolean isBillingTime(long billingTime, long lastBillingTime, long billingInterval) {
		return billingTime - lastBillingTime >= billingInterval;
	}
}