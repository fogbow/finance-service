package cloud.fogbow.fs.core;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class FinancePluginInstantiator {
	private static FsClassFactory classFactory = new FsClassFactory();
	
	public static FinancePlugin getFinancePlugin(String className, DatabaseManager databaseManager) {
		return (FinancePlugin) classFactory.createPluginInstance(className, databaseManager);
	}
}
