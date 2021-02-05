package cloud.fogbow.fs.core;

import java.util.List;

import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class FinanceManager {

	private List<FinancePlugin> financePlugins;
	
	public FinanceManager(List<FinancePlugin> financePlugins) {
		this.financePlugins = financePlugins;
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
}
