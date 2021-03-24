package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;

public class DatabaseManager {

	private static Logger LOGGER = Logger.getLogger(DatabaseManager.class);
	private List<FinanceUser> financeUsers;

	public DatabaseManager() {
		financeUsers = new ArrayList<FinanceUser>();
	}

	public void registerUser(String userId, String provider, String pluginName, Map<String, String> financeOptions) {
		FinanceUser user = new FinanceUser(financeOptions);

		user.setId(userId);
		user.setProvider(provider);
		user.setFinancePluginName(pluginName);

		financeUsers.add(user);
	}
	
	public void removeUser(String userId, String provider) {
		financeUsers.remove(getUserById(userId, provider));
	}

	public List<FinanceUser> getRegisteredUsers() {
		return financeUsers;
	}
	
	public FinanceUser getUserById(String id, String provider) {
		for (FinanceUser user : financeUsers) {
			if (user.getId().equals(id) && 
					user.getProvider().equals(provider)) {
				return user;
			}
		}
		
		// TODO treat this
		return null;
	}
	
	public List<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
		ArrayList<FinanceUser> selectedUsers = new ArrayList<FinanceUser>();
		
		for (FinanceUser user : financeUsers) {
			if (user.getFinancePluginName().equals(pluginName)) {
				selectedUsers.add(user);
			}
		}

		return selectedUsers;
	}

	public void changeOptions(String userId, String provider, HashMap<String, String> financeOptions) {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);
		
		for (String option : financeOptions.keySet()) {
			user.setProperty(option, financeOptions.get(option));
		}
	}

	public void updateFinanceState(String userId, String provider, HashMap<String, String> financeState) {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);

		for (String option : financeState.keySet()) {
			user.setProperty(option, financeState.get(option));
		}
	}

	public Map<String, String> getPlan(String planName) {
		// TODO Auto-generated method stub
		return null;
	}

	public void saveInvoice(Invoice invoice) {
		// TODO Auto-generated method stub
		
	}

	public Invoice getInvoice(String invoiceId) {
		// TODO Auto-generated method stub
		return null;
	}

	public UserCredits getUserCreditsByUserId(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void saveUserCredits(UserCredits credits) {
		// TODO Auto-generated method stub
		
	}
}
