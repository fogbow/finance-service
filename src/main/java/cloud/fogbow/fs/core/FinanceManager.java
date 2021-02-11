package cloud.fogbow.fs.core;

import java.util.HashMap;
import java.util.List;

import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class FinanceManager {

	private List<FinancePlugin> financePlugins;
	private DatabaseManager databaseManager;
	
	public FinanceManager(List<FinancePlugin> financePlugins, 
			DatabaseManager databaseManager) {
		this.financePlugins = financePlugins;
		this.databaseManager = databaseManager;
	}

	// TODO test
	public boolean isAuthorized(AuthorizableUser user) {
		for (FinancePlugin plugin : financePlugins) {
			if (!plugin.isAuthorized(user.getUserId(), user.getOperationParameters())) {
				return false;
			}
		}
		
		return true;
	}

	public void addUser(User user) {
		this.databaseManager.registerUser(user.getUserId(), user.getProvider(), user.getFinanceOptions());
	}
	
	public void startPlugins() {
		for (FinancePlugin plugin : financePlugins) {
			plugin.startThreads();
		}
	}

	public void removeUser(String userId) {
		this.databaseManager.removeUser(userId);
	}

	public void changeOptions(String userId, HashMap<String, String> financeOptions) {
		this.databaseManager.changeOptions(userId, financeOptions);
	}

	public void updateFinanceState(String userId, HashMap<String, String> financeState) {
		this.databaseManager.updateFinanceState(userId, financeState);
	}
}
