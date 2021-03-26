package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;

public class DatabaseManager {

	private static Logger LOGGER = Logger.getLogger(DatabaseManager.class);
	private List<FinanceUser> financeUsers;
	private List<Invoice> invoices;
	private List<FinancePlan> financePlans;

	public DatabaseManager() {
		financeUsers = new ArrayList<FinanceUser>();
		invoices = new ArrayList<Invoice>();
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

	public void changeOptions(String userId, String provider, Map<String, String> financeOptions) {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);
		
		for (String option : financeOptions.keySet()) {
			user.setProperty(option, financeOptions.get(option));
		}
	}

	public void updateFinanceState(String userId, String provider, Map<String, String> financeState) {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);

		for (String option : financeState.keySet()) {
			user.setProperty(option, financeState.get(option));
		}
	}

	public void saveInvoice(Invoice invoice) {
		for (Invoice savedInvoice : invoices) {
			if (savedInvoice.getInvoiceId().equals(invoice.getInvoiceId())) {
				invoices.remove(savedInvoice);
			}
		}
		
		invoices.add(invoice);
	}

	public Invoice getInvoice(String invoiceId) {
		for (Invoice invoice : invoices) {
			if (invoice.getInvoiceId().equals(invoiceId)) {
				return invoice;
			}
		}
		
		// FIXME treat this
		return null;
	}

	public List<Invoice> getInvoiceByUserId(String userId, String provider) {
		List<Invoice> userInvoices = new ArrayList<Invoice>();
		
		for (Invoice invoice : invoices) {
			if (invoice.getUserId().equals(userId)
					&& invoice.getProviderId().equals(provider)) {
				userInvoices.add(invoice);
			}
		}
		
		return userInvoices;
	}
	
	public UserCredits getUserCreditsByUserId(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void saveUserCredits(UserCredits credits) {
		// TODO Auto-generated method stub
		
	}

	public void saveFinancePlan(FinancePlan financePlan) {
		for (FinancePlan savedFinancePlan : financePlans) {
			if (savedFinancePlan.getName().equals(financePlan.getName())) {
				financePlans.remove(savedFinancePlan);
			}
		}
		
		// FIXME treat this
		financePlans.add(financePlan);
	}

	public FinancePlan getFinancePlan(String planName) {
		for (FinancePlan financePlan : financePlans) {
			if (financePlan.getName().equals(planName)) {
				return financePlan;
			}
		}
		
		return null;
	}

	public void removeFinancePlan(String planName) {
		for (FinancePlan financePlan : financePlans) {
			if (financePlan.getName().equals(planName)) {
				financePlans.remove(financePlan);
			}
		}
	}
}
