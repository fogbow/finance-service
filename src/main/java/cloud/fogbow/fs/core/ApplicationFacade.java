package cloud.fogbow.fs.core;

import java.util.HashMap;

import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;

public class ApplicationFacade {

	private static ApplicationFacade instance;
	private FinanceManager financeManager;
	
	private ApplicationFacade() {
		
	}
	
	public static ApplicationFacade getInstance() {
		if (instance == null) {
			instance = new ApplicationFacade();
		}
		return instance;
	}

	public void setFinanceManager(FinanceManager financeManager) { 
		this.financeManager = financeManager;
	}
	
	public boolean isAuthorized(AuthorizableUser user) {
		return this.financeManager.isAuthorized(user);
	}

	public void addUser(String systemUserToken, User user) {
		// TODO authentication
		// TODO authorization
		this.financeManager.addUser(user);
	}

	public void removeUser(String systemUserToken, String userId) {
		// TODO authentication
		// TODO authorization
		this.financeManager.removeUser(userId);
	}

	public void changeOptions(String systemUserToken, String userId, HashMap<String, String> financeOptions) {
		// TODO authentication
		// TODO authorization
		this.financeManager.changeOptions(userId, financeOptions);
	}

	public void updateFinanceState(String systemUserToken, String userId, HashMap<String, String> financeState) {
		// TODO authentication
		// TODO authorization
		this.financeManager.updateFinanceState(userId, financeState);
	}
}
