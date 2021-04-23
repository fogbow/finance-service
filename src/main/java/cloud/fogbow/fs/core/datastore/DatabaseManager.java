package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;

public class DatabaseManager {

	private static Logger LOGGER = Logger.getLogger(DatabaseManager.class);
	private List<FinanceUser> financeUsers;
	private List<Invoice> invoices;
	private List<FinancePlan> financePlans;
	private List<UserCredits> creditsList;
	
	public DatabaseManager() {
		financeUsers = new ArrayList<FinanceUser>();
		invoices = new ArrayList<Invoice>();
		financePlans = new ArrayList<FinancePlan>();
		creditsList = new ArrayList<UserCredits>();
	}

	public void registerUser(String userId, String provider, String pluginName, Map<String, String> financeOptions) {
		FinanceUser user = new FinanceUser(financeOptions);

		user.setId(userId);
		user.setProvider(provider);
		user.setFinancePluginName(pluginName);

		financeUsers.add(user);
	}
	
    public void saveUser(FinanceUser user) {
        financeUsers.add(user);
    }
	
	public void removeUser(String userId, String provider) throws InvalidParameterException {
		financeUsers.remove(getUserById(userId, provider));
	}

	public List<FinanceUser> getRegisteredUsers() {
		return financeUsers;
	}
	
	public FinanceUser getUserById(String id, String provider) throws InvalidParameterException {
		for (FinanceUser user : financeUsers) {
			if (user.getId().equals(id) && 
					user.getProvider().equals(provider)) {
				return user;
			}
		}
		
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider));
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

	public void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);
		
		for (String option : financeOptions.keySet()) {
			user.setProperty(option, financeOptions.get(option));
		}
		
		this.saveUser(user);
	}
	
    public void changeOptions(FinanceUser user, Map<String, String> financeOptions) {
        this.saveUser(user);
    }

	public void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException {
		// TODO validation
		FinanceUser user = getUserById(userId, provider);

		for (String option : financeState.keySet()) {
			user.setProperty(option, financeState.get(option));
		}
	}

	public void saveInvoice(Invoice invoice) {
	    if (!invoices.contains(invoice)) {
	        invoices.add(invoice);
	    }
	}
	
    public List<Invoice> getRegisteredInvoices() {
        return invoices;
    }

	public Invoice getInvoice(String invoiceId) throws InvalidParameterException {
		for (Invoice invoice : invoices) {
			if (invoice.getInvoiceId().equals(invoiceId)) {
				return invoice;
			}
		}
		
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_INVOICE, invoiceId));
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
	
    public List<UserCredits> getRegisteredUserCredits() {
        return creditsList;
    }
	
	public UserCredits getUserCreditsByUserId(String userId, String provider) throws InvalidParameterException {
	    for (UserCredits userCredits : creditsList) {
	        if (userCredits.getUserId().equals(userId) &&
	                userCredits.getProvider().equals(provider)) {
	            return userCredits;
	        }
	    }
	    
	    throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER_CREDITS, userId, provider));
	}

	public void saveUserCredits(UserCredits credits) {
        if (!creditsList.contains(credits)) {
            creditsList.add(credits);
        }
	}

	public void saveFinancePlan(FinancePlan financePlan) {
		if (!financePlans.contains(financePlan)) {
		    financePlans.add(financePlan);
		}
	}

	public FinancePlan getFinancePlan(String planName) throws InvalidParameterException {
		for (FinancePlan financePlan : financePlans) {
			if (financePlan.getName().equals(planName)) {
				return financePlan;
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, planName));
	}
	
	public List<FinancePlan> getRegisteredFinancePlans() {
	    return financePlans;
	}
	
	public void removeFinancePlan(String planName) {
	    FinancePlan planToRemove = null;
	    
		for (FinancePlan financePlan : financePlans) {
			if (financePlan.getName().equals(planName)) {
			    planToRemove = financePlan;
			}
		}
		
		financePlans.remove(planToRemove);
	}
}
