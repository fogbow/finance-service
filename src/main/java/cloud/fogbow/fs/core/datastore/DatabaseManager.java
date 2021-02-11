package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.HashMap;
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
		user.setProvider(provider);

		financeUsers.add(user);
	}
	
	public void removeUser(String userId) {
		financeUsers.remove(getUserById(userId));
	}

	public List<FinanceUser> getRegisteredUsers() {
		return financeUsers;
	}
	
	public FinanceUser getUserById(String id) {
		for (FinanceUser user : financeUsers) {
			if (user.getId().equals(id)) {
				return user;
			}
		}
		
		return null;
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

	public void changeOptions(String userId, HashMap<String, String> financeOptions) {
		// TODO validation
		FinanceUser user = getUserById(userId);
		
		for (String option : financeOptions.keySet()) {
			user.setProperty(option, financeOptions.get(option));
		}
	}

	public void updateFinanceState(String userId, HashMap<String, String> financeState) {
		// TODO validation
		FinanceUser user = getUserById(userId);

		for (String option : financeState.keySet()) {
			user.setProperty(option, financeState.get(option));
		}
	}
}
