package cloud.fogbow.fs.constants;

public class ApiDocumentation {

    public static class ApiInfo {
        public static final String API_TITLE = "Fogbow Finance Service API";
        public static final String API_DESCRIPTION =
                "This API allows clients to access and manage the financial state of Fogbow users." +
                " Operations include subscribing users to finance plans and changing the finance plans parameters." +
                " Some operations are restricted to admins."; 
    }
    
    public static class Admin {
        public static final String API = "Manages admin-only operations";
        public static final String RELOAD_OPERATION = "Reloads configuration parameters.";
        public static final String SET_POLICY_OPERATION = "Overrides current authorization policy using given policy string.";
        public static final String SET_POLICY_REQUEST_BODY = 
                "The body of the request must contain the new authorization policy, " +
                "in string format.";
        public static final String UPDATE_POLICY_OPERATION = "Updates current authorization policy using given policy string.";
        public static final String UPDATE_POLICY_REQUEST_BODY = 
                "The body of the request must contain the policy values to update, " +
                "in string format.";
        public static final String REGISTER_USER_OPERATION = "Registers the given user to be financially managed by the service.";
        public static final String REGISTER_USER_REQUEST_BODY = "The body of the request must specify the user id," +
                " the user identity provider id and the name of the finance plan used to manage the user financial state.";
        public static final String CHANGE_USER_PLAN_OPERATION = "Changes the plan used to manage the user";
        public static final String CHANGE_USER_PLAN_REQUEST_BODY = "The body of the request must specify the user id," +
                " the user identity provider id and the name of the new finance plan to be used to manage the user financial state.";
        public static final String UNREGISTER_USER = "Unregisters the user from the finance plan it currently uses." + 
                " The user financial state will remain unmanaged until the user is registered again to a finance plan.";
        public static final String USER_ID = "The ID of the specific user.";
        public static final String PROVIDER = "The ID of the specific identity provider.";
        public static final String REMOVE_USER = "Removes the given user from the service, deleting all the data related to the user.";
    }
    
	public static class Authorization {
		public static final String API = "Manages authorization queries.";
		public static final String IS_AUTHORIZED = "States whether the user is authorized regarding its finance status in the federation.";
        public static final String IS_AUTHORIZED_REQUEST_BODY = "The body of the request must specify the token of the user attempting an" +
                " operation and a description of the operation.";
	}	
}
