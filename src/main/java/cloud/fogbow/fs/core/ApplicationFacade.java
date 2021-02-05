package cloud.fogbow.fs.core;

import cloud.fogbow.fs.api.parameters.AuthorizableUser;

public class ApplicationFacade {

	private static ApplicationFacade instance;
	
	public static ApplicationFacade getInstance() {
		if (instance == null) {
			instance = new ApplicationFacade();
		}
		return instance;
	}

	public boolean isAuthorized(AuthorizableUser user) {
		// TODO Auto-generated method stub
		return false;
	}

}
