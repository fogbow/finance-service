package cloud.fogbow.fs.core.plugins.finance.prepaid;

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

	static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
	public static final String USER_LAST_BILLING_TIME = "last_billing_time";
	public static final String USER_BILLING_INTERVAL = "billing_interval";
	private DatabaseManager databaseManager;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient;
	private TimeUtils timeUtils;
	
	public PaymentRunner(long creditsDeductionWaitTime, DatabaseManager databaseManager,
			AccountingServiceClient accountingServiceClient, PaymentManager paymentManager) {
		this(creditsDeductionWaitTime, databaseManager, accountingServiceClient, 
				paymentManager, new TimeUtils());
	}
	
	public PaymentRunner(long creditsDeductionWaitTime, DatabaseManager databaseManager,
			AccountingServiceClient accountingServiceClient, PaymentManager paymentManager, 
			TimeUtils timeUtils) {
		this.timeUtils = timeUtils;
		this.sleepTime = creditsDeductionWaitTime;
		this.databaseManager = databaseManager;
		this.accountingServiceClient = accountingServiceClient;
		this.paymentManager = paymentManager;
	}
	
	private long getUserLastBillingTime(FinanceUser user) {
		String lastBillingTimeProperty = user.getProperty(USER_LAST_BILLING_TIME);
		
		if (lastBillingTimeProperty == null) {
			long billingTime = this.timeUtils.getCurrentTimeMillis();
			user.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
			return billingTime;
		}
		
		return Long.valueOf(lastBillingTimeProperty);
	}
	
	@Override
	public void doRun() {
		List<FinanceUser> financeUsers = this.databaseManager.getRegisteredUsersByPaymentType(PrePaidFinancePlugin.PLUGIN_NAME);
		
		for (FinanceUser user : financeUsers) {
			long billingTime = this.timeUtils.getCurrentTimeMillis();
			long lastBillingTime = getUserLastBillingTime(user);
			
			String invoiceStartDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, lastBillingTime);
			String invoiceEndDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, billingTime); 
			
			try {
				List<Record> userRecords = this.accountingServiceClient.getUserRecords(user.getId(), 
						user.getProvider(), invoiceStartDate, invoiceEndDate);
				
				user.setPeriodRecords(userRecords);
				this.paymentManager.startPaymentProcess(user.getId());
				
				user.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
			} catch (FogbowException e) {
				// TODO add log
				e.printStackTrace();
			}
		}
		
		checkIfMustStop();
	}
}
