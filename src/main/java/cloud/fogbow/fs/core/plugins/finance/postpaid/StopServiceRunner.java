package cloud.fogbow.fs.core.plugins.finance.postpaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private InMemoryFinanceObjectsHolder objectHolder;
	private PaymentManager paymentManager;
	private RasClient rasClient;

    public StopServiceRunner(long stopServiceWaitTime, InMemoryFinanceObjectsHolder objectHolder, PaymentManager paymentManager,
            RasClient rasClient) {
        this.sleepTime = stopServiceWaitTime;
        this.objectHolder = objectHolder;
        this.paymentManager = paymentManager;
        this.rasClient = rasClient;
    }

	@Override
	public void doRun() {
		// This runner depends on PostPaidFinancePlugin. Maybe we should 
		// pass the plugin name as an argument to the constructor, so we 
		// can reuse this class.
	    
	    // TODO refactor
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = 
		        this.objectHolder.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            synchronized (user) {
	                try {
	                    boolean paid = this.paymentManager.hasPaid(user.getId(), user.getProvider());
	                    boolean stoppedResources = user.stoppedResources();
	    
	                    // if user has not paid
	                    if (!paid && !stoppedResources) {
	                        // stop resources
	                        try {
	                            this.rasClient.pauseResourcesByUser(user.getId());
	                            // write status in the db
	                            user.setStoppedResources(true);
	                            this.objectHolder.saveUser(user);
	                        } catch (FogbowException e) {
	                            LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(), 
	                                    e.getMessage()));
	                        }
	                    }
	    
	                    // if user has stopped resources but paid
	                    if (paid && stoppedResources) {
	                        // start resources
	                        try {
	                            this.rasClient.resumeResourcesByUser(user.getId());
	                            // write status in db
	                            user.setStoppedResources(false);
	                            this.objectHolder.saveUser(user);
	                        } catch (FogbowException e) {
	                            LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(), 
	                                    e.getMessage()));
	                        }
	                    }
	                } catch (InvalidParameterException e) {
	                    // TODO test
	                    LOGGER.error(e.getMessage());
	                }
	            }
	            
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (InvalidParameterException e) {
	        // TODO test
	        LOGGER.error(String.format(Messages.Log.FAILED_TO_MANAGE_RESOURCES, e.getMessage()));
        } catch (ModifiedListException e) {
            // TODO treat this
            e.printStackTrace();
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }

		checkIfMustStop();
	}
}