package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
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
    /**
     * The key to use to indicate the amount of time to 
     * wait between consecutive billing processes.
     */
    public static final String USER_BILLING_INTERVAL = "billing_interval";
    private InMemoryFinanceObjectsHolder objectHolder;
	private AccountingServiceClient accountingServiceClient;
	private PaymentManager paymentManager;
	private TimeUtils timeUtils;
	
	public PaymentRunner(long invoiceWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager) {
        this.timeUtils = new TimeUtils();
        this.sleepTime = invoiceWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
	}

    public PaymentRunner(long invoiceWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager, 
            TimeUtils timeUtils) {
        this.timeUtils = timeUtils;
        this.sleepTime = invoiceWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
    }

	private long getUserLastBillingTime(FinanceUser user) {
		String lastBillingTimeProperty = user.getProperty(FinanceUser.USER_LAST_BILLING_TIME);
		return Long.valueOf(lastBillingTimeProperty);
	}

	private long getUserBillingInterval(FinanceUser user) {
		return Long.valueOf(user.getProperty(USER_BILLING_INTERVAL));
	}

	@Override
	public void doRun() {
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = objectHolder.
		        getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            synchronized(user) {
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
	                        
	                        user.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(billingTime));
	                        this.objectHolder.saveUser(user);
	                    } catch (FogbowException e) {
	                        LOGGER.error(String.format(Messages.Log.FAILED_TO_GENERATE_INVOICE_FOR_USER, user.getId(), e.getMessage()));
	                    }
	                }
	            }
	            
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (InvalidParameterException e) {
	        // TODO test
	        LOGGER.error(String.format(Messages.Log.FAILED_TO_GENERATE_INVOICE, e.getMessage()));
        } catch (ModifiedListException e) {
            // TODO treat
            e.printStackTrace();
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }

		checkIfMustStop();
	}

	private boolean isBillingTime(long billingTime, long lastBillingTime, long billingInterval) {
		return billingTime - lastBillingTime >= billingInterval;
	}
}