package cloud.fogbow.fs.core.plugins.finance.prepaid;

import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private DatabaseManager databaseManager;
	private PaymentManager paymentManager;
	private RasClient rasClient;
	
	public StopServiceRunner(long stopServiceWaitTime, DatabaseManager databaseManager, 
			PaymentManager paymentManager, RasClient rasClient) {
		this.sleepTime = stopServiceWaitTime;
		this.databaseManager = databaseManager;
		this.paymentManager = paymentManager;
		this.rasClient = rasClient;
	}
	
	@Override
	public void doRun() {
		// This runner depends on PrePaidFinancePlugin. Maybe we should 
		// pass the plugin name as an argument to the constructor, so we 
		// can reuse this class.
		List<FinanceUser> registeredUsers = this.databaseManager
				.getRegisteredUsersByPaymentType(PrePaidFinancePlugin.PLUGIN_NAME);

		// for each user
		for (FinanceUser user : registeredUsers) {
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
                    } catch (FogbowException e) {
                        LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES, user.getId(), 
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
                    } catch (FogbowException e) {
                        LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES, user.getId(), 
                                e.getMessage()));
                    }
                }
            } catch (InvalidParameterException e) {
                // TODO test
                LOGGER.error(e.getMessage());
            }
		}
		
		checkIfMustStop();
	}
}
