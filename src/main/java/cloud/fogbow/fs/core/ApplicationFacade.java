package cloud.fogbow.fs.core;

import cloud.fogbow.fs.api.parameters.AuthorizableUser;

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
}
