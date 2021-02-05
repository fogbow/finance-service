package cloud.fogbow.fs.api.parameters;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class AuthorizableUser {
	private static final String USER_ID_KEY = "userId";
	private static final String OPERATION_PARAMETERS_KEY = "operationParameters";
	private String userId;
	private String operationParameters;
	
	public AuthorizableUser() {
		
	}
	
	public AuthorizableUser(String userId, Map<String, String> operationParameters) {
		Gson gson = new Gson();
		this.userId = userId;
		this.operationParameters = gson.toJson(operationParameters);
	}

	public String getUserId() {
		return userId;
	}
	
	public HashMap<String, String> getOperationParameters() {
		return new Gson().fromJson(operationParameters, HashMap.class);
	}
	
    public Map<String, String> asRequestBody() {
        HashMap<String, String> body = new HashMap<String, String>();
        body.put(USER_ID_KEY, this.userId);
        body.put(OPERATION_PARAMETERS_KEY, this.operationParameters);
        return body;
    }
}
