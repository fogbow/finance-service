package cloud.fogbow.fs.core.datastore;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserId;

@Component
public class DatabaseManager {

	private List<FinancePlan> financePlans;
	
	@Autowired
	private UserRepository userRepository;
	
	public DatabaseManager() {
		financePlans = new ArrayList<FinancePlan>();
	}

    public void saveUser(FinanceUser user) {
        userRepository.save(user);
    }
	
	public void removeUser(String userId, String provider) throws InvalidParameterException {
	    FinanceUser user = userRepository.findByUserId(new UserId(userId, provider));
		userRepository.delete(user);
	}

	public List<FinanceUser> getRegisteredUsers() {
	    return userRepository.findAll();
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
