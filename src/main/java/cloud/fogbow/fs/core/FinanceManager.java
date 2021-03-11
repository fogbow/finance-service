package cloud.fogbow.fs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class FinanceManager {
	@VisibleForTesting
	static final String FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR = ",";
	private List<FinancePlugin> financePlugins;
	private DatabaseManager databaseManager;
	
	public FinanceManager(DatabaseManager databaseManager) throws ConfigurationErrorException {
		String financePluginsString = PropertiesHolder.getInstance()
				.getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

		if (financePluginsString.isEmpty()) {
			throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
		}
		
		for (String financePluginClassName : financePluginsString.split(FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR)) {
			financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName, databaseManager));
		}
		
		this.financePlugins = financePlugins;
		this.databaseManager = databaseManager;
	}
	
	public FinanceManager(List<FinancePlugin> financePlugins, 
			DatabaseManager databaseManager) throws ConfigurationErrorException {
		if (financePlugins.isEmpty()) {
			throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
		}
		this.financePlugins = financePlugins;
		this.databaseManager = databaseManager;
	}

	public boolean isAuthorized(AuthorizableUser user) throws InvalidParameterException {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(user.getUserId())) {
				return plugin.isAuthorized(user.getUserId(), user.getOperationParameters());
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, user.getUserId()));
	}

	public void addUser(User user) {
		this.databaseManager.registerUser(user.getUserId(), user.getProvider(), user.getFinanceOptions());
	}
	
	public void startPlugins() {
		for (FinancePlugin plugin : financePlugins) {
			plugin.startThreads();
		}
	}

	public void stopPlugins() {
		for (FinancePlugin plugin : financePlugins) {
			plugin.stopThreads();
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

	public String getFinanceStateProperty(String userId, String property) throws FogbowException {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(userId)) {
				return plugin.getUserFinanceState(userId, property);
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, userId));
	}
}
