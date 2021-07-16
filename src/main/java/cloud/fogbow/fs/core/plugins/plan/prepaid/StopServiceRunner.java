package cloud.fogbow.fs.core.plugins.plan.prepaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.util.StoppableRunner;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private String planName;
	private InMemoryUsersHolder usersHolder;
	private CreditsManager paymentManager;
	private RasClient rasClient;
	private DebtsPaymentChecker debtsChecker;
	
    public StopServiceRunner(String planName, long stopServiceWaitTime, InMemoryUsersHolder usersHolder, 
            CreditsManager paymentManager, RasClient rasClient, DebtsPaymentChecker debtsChecker) {
        this.planName = planName;
        this.sleepTime = stopServiceWaitTime;
        this.usersHolder = usersHolder;
        this.paymentManager = paymentManager;
        this.rasClient = rasClient;
        this.debtsChecker = debtsChecker;
    }

	@Override
	public void doRun() {
		// This runner depends on PrePaidPlanPlugin. Maybe we should 
		// pass the plugin name as an argument to the constructor, so we 
		// can reuse this class.
	    
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = 
	            this.usersHolder.getRegisteredUsersByPlan(this.planName);
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
                boolean currentStateIsGood = this.paymentManager.hasPaid(user.getId(), user.getProvider());
                boolean paid = pastDebtsHaveBeenPaid && currentStateIsGood;
                boolean stoppedResources = user.stoppedResources();

                if (!paid && !stoppedResources) {
                    tryToStopResources(user);
                }

                if (paid && stoppedResources) {
                    tryToResumeResources(user);
                }
            } catch (InvalidParameterException e) {
                LOGGER.error(String.format(Messages.Log.UNABLE_TO_FIND_USER, user.getId(), user.getProvider()));
            }
        }
    }

    private void tryToStopResources(FinanceUser user) {
        try {
            tryToHibernateThenTryToStop(user);
            user.setStoppedResources(true);
            this.usersHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(),
                    e.getMessage()));
        }
    }

    private void tryToHibernateThenTryToStop(FinanceUser user) throws FogbowException {
        try {
            this.rasClient.hibernateResourcesByUser(user.getId(), user.getProvider());
        } catch (NotImplementedOperationException e) {
            this.rasClient.stopResourcesByUser(user.getId(), user.getProvider());
        }
    }
    
    private void tryToResumeResources(FinanceUser user) {
        try {
            this.rasClient.resumeResourcesByUser(user.getId(), user.getProvider());
            user.setStoppedResources(false);
            this.usersHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(),
                    e.getMessage()));
        }
    }

    public void resumeResourcesForUser(FinanceUser user) throws InternalServerErrorException, InvalidParameterException {
        try {
            this.rasClient.resumeResourcesByUser(user.getId(), user.getProvider());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
        user.setStoppedResources(false);
        this.usersHolder.saveUser(user);
    }

    public void purgeUserResources(FinanceUser user) throws InternalServerErrorException {
        try {
            this.rasClient.purgeUser(user.getId(), user.getProvider());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
}