package cloud.fogbow.fs.core.plugins.plan.postpaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.util.StoppableRunner;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class StopServiceRunner  extends StoppableRunner {
    private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
    
    private String planName;
    private InMemoryUsersHolder userHolder;
    private InvoiceManager invoiceManager;
    private RasClient rasClient;
    private DebtsPaymentChecker debtsChecker;

    public StopServiceRunner(String planName, long stopServiceWaitTime, InMemoryUsersHolder userHolder, 
            InvoiceManager invoiceManager, RasClient rasClient, DebtsPaymentChecker debtsChecker) {
        this.sleepTime = stopServiceWaitTime;
        this.userHolder = userHolder;
        this.invoiceManager = invoiceManager;
        this.rasClient = rasClient;
        this.planName = planName;
        this.debtsChecker = debtsChecker;
    }

    @Override
    public void doRun() {
        // This runner depends on PostPaidPlanPlugin. Maybe we should 
        // pass the plugin name as an argument to the constructor, so we 
        // can reuse this class.
        MultiConsumerSynchronizedList<FinanceUser> registeredUsers = userHolder.
                getRegisteredUsersByPlan(this.planName);
        Integer consumerId = registeredUsers.startIterating();
        
        try {
            FinanceUser user = registeredUsers.getNext(consumerId);
            
            while (user != null) {
                tryToCheckUserState(user);
                user = registeredUsers.getNext(consumerId);
            }
        } catch (ModifiedListException e) {
            LOGGER.debug(Messages.Log.USER_LIST_CHANGED_SKIPPING_USER_PAYMENT_STATE_CHECK);
        } catch (InternalServerErrorException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_MANAGE_RESOURCES, e.getMessage()));
        } finally {
            registeredUsers.stopIterating(consumerId);
        }

        checkIfMustStop();
    }

    private void tryToCheckUserState(FinanceUser user) throws InternalServerErrorException {
        synchronized (user) {
            try {
                boolean pastDebtsHaveBeenPaid = this.debtsChecker.hasPaid(user.getId(), user.getProvider());
                boolean currentStateIsGood = this.invoiceManager.hasPaid(user.getId(), user.getProvider());
                boolean paid = pastDebtsHaveBeenPaid && currentStateIsGood;
                boolean stoppedResources = user.stoppedResources();
   
                if (!paid && !stoppedResources) {
                    tryToPauseResources(user);
                }
   
                if (paid && stoppedResources) {
                    tryToResumeResources(user);
                }
            } catch (InvalidParameterException e) {
                LOGGER.error(String.format(Messages.Log.UNABLE_TO_FIND_USER, user.getId(), user.getProvider()));
            }
        }
    }

    public void pauseResourcesForUser(FinanceUser user) throws InvalidParameterException, InternalServerErrorException {
        try {
            this.rasClient.pauseResourcesByUser(user.getId());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
        user.setStoppedResources(true);
        this.userHolder.saveUser(user);
    }
    
    private void tryToPauseResources(FinanceUser user) {
        try {
            this.rasClient.pauseResourcesByUser(user.getId());
            user.setStoppedResources(true);
            this.userHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(), 
                    e.getMessage()));
        }
    }
    
    public void resumeResourcesForUser(FinanceUser user) throws InternalServerErrorException, InvalidParameterException {
        try {
            this.rasClient.resumeResourcesByUser(user.getId());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
        user.setStoppedResources(false);
        this.userHolder.saveUser(user);
    }
    
    private void tryToResumeResources(FinanceUser user) {
        try {
            this.rasClient.resumeResourcesByUser(user.getId());
            user.setStoppedResources(false);
            this.userHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(), 
                    e.getMessage()));
        }
    }
}
