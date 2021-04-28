package cloud.fogbow.fs.core.plugins.finance.prepaid;

import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
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
	static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
	private InMemoryFinanceObjectsHolder objectHolder;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient;
	private TimeUtils timeUtils;
	
    public PaymentRunner(long creditsDeductionWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager) {
        this.timeUtils = new TimeUtils();
        this.sleepTime = creditsDeductionWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
    }
    
    public PaymentRunner(long creditsDeductionWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager, 
            TimeUtils timeUtils) {
        this.timeUtils = timeUtils;
        this.sleepTime = creditsDeductionWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
    }
	
	private long getUserLastBillingTime(FinanceUser user) {
		String lastBillingTimeProperty = user.getProperty(FinanceUser.USER_LAST_BILLING_TIME);
		return Long.valueOf(lastBillingTimeProperty);
	}
	
	@Override
	public void doRun() {
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = objectHolder.
	                getRegisteredUsersByPaymentType(PrePaidFinancePlugin.PLUGIN_NAME);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            synchronized (user) {
	                long billingTime = this.timeUtils.getCurrentTimeMillis();
	                long lastBillingTime = getUserLastBillingTime(user);

	                // Maybe move this conversion to ACCSClient
	                String invoiceStartDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, lastBillingTime);
	                String invoiceEndDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, billingTime);

	                try {
	                    List<Record> userRecords = this.accountingServiceClient.getUserRecords(user.getId(),
	                            user.getProvider(), invoiceStartDate, invoiceEndDate);

	                    user.setPeriodRecords(userRecords);
	                    this.paymentManager.startPaymentProcess(user.getId(), user.getProvider(), lastBillingTime,
	                            billingTime);

	                    user.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(billingTime));
	                    this.objectHolder.saveUser(user);
	                } catch (FogbowException e) {
	                    LOGGER.error(String.format(Messages.Log.FAILED_TO_DEDUCT_CREDITS_FOR_USER, user.getId(), e.getMessage()));
	                }
	            }
	            
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (ModifiedListException e) {
            // TODO treat
            e.printStackTrace();
        } catch (InternalServerErrorException e) {
            // TODO test
            LOGGER.error(String.format(Messages.Log.FAILED_TO_DEDUCT_CREDITS, e.getMessage()));
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }
        
		checkIfMustStop();
	}
}
