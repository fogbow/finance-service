package cloud.fogbow.fs.api.parameters;

import java.util.HashMap;

public class AuthorizableUser {
	private String userId;
	private HashMap<String, String> operationParameters;
	
	public String getUserId() {
		return userId;
	}
	
	public HashMap<String, String> getOperationParameters() {
		return operationParameters;
	}
}
