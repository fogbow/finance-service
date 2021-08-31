package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserId;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;

@Component
public class DatabaseManager {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private FinancePlanRepository planRepository;
	
	public DatabaseManager() {
	}

    public void saveUser(FinanceUser user) {
        userRepository.save(user);
    }
	
	public void removeUser(String userId, String provider) throws InvalidParameterException {
	    FinanceUser user = userRepository.findByUserId(new UserId(userId, provider));
		userRepository.delete(user);
	}

	public List<FinanceUser> getRegisteredUsers() {
	    List<FinanceUser> users = new ArrayList<FinanceUser>();
	    
	    // For an unknown reason, Spring is not loading invoice data correctly. 
	    // Thus, we use this iteration to force the loading of invoice data.
	    // Needs further investigation.
	    for (FinanceUser user : userRepository.findAll()) {
	        for (Invoice invoice : user.getInvoices()) {
	            invoice.getInvoiceId();
	        }
	        
	        users.add(user);
	    }
	    
	    return users;
	}

    public List<PersistablePlanPlugin> getRegisteredPlans() {
        return planRepository.findAll();
    }

    public void savePlan(PersistablePlanPlugin plan) {
        planRepository.save(plan);
    }

    public void removePlan(PersistablePlanPlugin plan) {
        planRepository.delete(plan);
    }
}
