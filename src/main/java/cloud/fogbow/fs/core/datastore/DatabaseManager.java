package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cloud.fogbow.fs.core.models.FinanceUser;

public class DatabaseManager {

	private List<FinanceUser> financeUsers;

	public DatabaseManager() {
		financeUsers = new ArrayList<FinanceUser>();
	}

	public void registerUser(String userId, String provider, Map<String, String> financeOptions) {
		FinanceUser user = new FinanceUser(financeOptions);

		user.setId(userId);
		user.setId(provider);

		financeUsers.add(user);
	}

	public List<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
		ArrayList<FinanceUser> selectedUsers = new ArrayList<FinanceUser>();

		for (FinanceUser user : financeUsers) {
			if (user.getProperty(FinanceUser.PAYMENT_TYPE_KEY).equals(pluginName)) {
				selectedUsers.add(user);
			}
		}

		return financeUsers;
	}
}
