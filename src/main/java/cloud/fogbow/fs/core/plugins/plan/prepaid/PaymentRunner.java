package cloud.fogbow.fs.core.plugins.plan.prepaid;

import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.StoppableRunner;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class PaymentRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(PaymentRunner.class);
	private String planName;
	private InMemoryUsersHolder usersHolder;
	private CreditsManager paymentManager;
	private AccountingServiceClient accountingServiceClient;
	private TimeUtils timeUtils;
	
    public PaymentRunner(String planName, long creditsDeductionWaitTime, InMemoryUsersHolder usersHolder,
            AccountingServiceClient accountingServiceClient, CreditsManager paymentManager) {
        super(creditsDeductionWaitTime);
        this.planName = planName;
        this.timeUtils = new TimeUtils();
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.usersHolder = usersHolder;
    }
    
    public PaymentRunner(String planName, long creditsDeductionWaitTime, InMemoryUsersHolder usersHolder,
            AccountingServiceClient accountingServiceClient, CreditsManager paymentManager, 
            TimeUtils timeUtils) {
        super(creditsDeductionWaitTime);
        this.planName = planName;
        this.timeUtils = timeUtils;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.usersHolder = usersHolder;
    }

	@Override
	public void doRun() {
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = usersHolder.
	                getRegisteredUsersByPlan(this.planName);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            tryToRunPaymentForUser(user);
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (ModifiedListException e) {
	        LOGGER.debug(Messages.Log.USER_LIST_CHANGED_SKIPPING_CREDITS_DEDUCTION);
        } catch (InternalServerErrorException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_DEDUCT_CREDITS, e.getMessage()));
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }
	}

    private void tryToRunPaymentForUser(FinanceUser user) {
        synchronized (user) {
            long billingTime = this.timeUtils.getCurrentTimeMillis();
            long lastBillingTime = user.getLastBillingTime();

            tryToDeductCreditsForUser(user, billingTime, lastBillingTime);
        }
    }

    private void tryToDeductCreditsForUser(FinanceUser user, long billingTime, long lastBillingTime) {
        try {
            List<Record> records = acquireUsageData(user, billingTime, lastBillingTime);
            this.paymentManager.startPaymentProcess(user.getId(), user.getProvider(), lastBillingTime,
                    billingTime, records);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_DEDUCT_CREDITS_FOR_USER, user.getId(), e.getMessage()));
        }
    }

    private List<Record> acquireUsageData(FinanceUser user, long billingTime, long lastBillingTime) 
            throws FogbowException {
        return this.accountingServiceClient.getUserRecords(user.getId(),
                user.getProvider(), lastBillingTime, billingTime);
    }
}
