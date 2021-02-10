package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunner extends StoppableRunner {
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
		List<FinanceUser> registeredUsers = this.databaseManager
				.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);

		// for each user
		for (FinanceUser user : registeredUsers) {
			boolean paid = this.paymentManager.hasPaid(user.getId());
			boolean stoppedResources = user.stoppedResources();

			// if user has not paid
			if (!paid && !stoppedResources) {
				// stop resources
				this.rasClient.pauseResourcesByUser(user.getId());
				// write status in the db
				user.setStoppedResources(true);
			}

			// if user has stopped resources but paid
			if (paid && stoppedResources) {
				// start resources
				this.rasClient.resumeResourcesByUser(user.getId());
				// write status in db
				user.setStoppedResources(false);
			}
		}
		
		checkIfMustStop();
	}
}