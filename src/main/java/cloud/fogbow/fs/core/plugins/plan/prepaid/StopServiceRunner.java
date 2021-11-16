package cloud.fogbow.fs.core.plugins.plan.prepaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.StoppableRunner;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.ResourcesPolicy;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private String planName;
	private InMemoryUsersHolder usersHolder;
	private RasClient rasClient;
	private ResourcesPolicy resourcesPolicy;
	
    public StopServiceRunner(String planName, long stopServiceWaitTime, 
            InMemoryUsersHolder usersHolder, RasClient rasClient, 
            ResourcesPolicy resourcesPolicy) {
        super(stopServiceWaitTime);
        this.planName = planName;
        this.usersHolder = usersHolder;
        this.rasClient = rasClient;
        this.resourcesPolicy = resourcesPolicy;
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
	}

    private void tryToCheckUserState(FinanceUser user) throws InternalServerErrorException {
        synchronized (user) {
            try {
                resourcesPolicy.updateUserState(user);
                usersHolder.saveUser(user);
            } catch (InvalidParameterException e) {
                LOGGER.error(String.format(Messages.Log.UNABLE_TO_FIND_USER, user.getId(), user.getProvider()));
            }
        }
    }

    public void resumeResourcesForUser(FinanceUser user) throws InternalServerErrorException, InvalidParameterException {
        try {
            this.rasClient.resumeResourcesByUser(user.getId(), user.getProvider());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public void purgeUserResources(FinanceUser user) throws InternalServerErrorException {
        try {
            this.rasClient.purgeUser(user.getId(), user.getProvider());
        } catch (FogbowException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
}