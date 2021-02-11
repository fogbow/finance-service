package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.TimeUtils;

public class PaymentRunner extends StoppableRunner {
	// TODO document this string
	private static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
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
		// TODO Improve
		String lastBillingTimeProperty = user.getProperty(USER_LAST_BILLING_TIME);
		
		if (lastBillingTimeProperty == null) {
			long billingTime = this.timeUtils.getCurrentTimeMillis();
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
		// databaseManager.get registered users
		List<FinanceUser> registeredUsers = this.databaseManager
				.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);

		// for each user
		for (FinanceUser user : registeredUsers) {
			// if it is billing time
			long billingTime = this.timeUtils.getCurrentTimeMillis(); 
			if ((billingTime - getUserLastBillingTime(user) >= getUserBillingInterval(user))) {
				// get records
				try {
					List<Record> userRecords = this.accountingServiceClient.getUserRecords(user.getId(), user.getProvider(),
							toDate(getUserLastBillingTime(user)), toDate(billingTime));
					// write records on db
					user.setPeriodRecords(userRecords);
					
					// generate invoice
					this.paymentManager.startPaymentProcess(user.getId());
					
					user.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
				} catch (FogbowException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		checkIfMustStop();
	}

	private String toDate(long lastBillingTime) {
		Date date = new Date(lastBillingTime); 
		return new SimpleDateFormat(SIMPLE_DATE_FORMAT).format(date);
	}
}