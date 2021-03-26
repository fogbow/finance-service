package cloud.fogbow.fs.core;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class PaymentManagerInstantiator {

	private static FsClassFactory classFactory = new FsClassFactory();

	public static PaymentManager getPaymentManager(String className, DatabaseManager databaseManager, 
			String planName) {
		return (PaymentManager) classFactory.createPluginInstance(className, databaseManager, planName);
	}
}
