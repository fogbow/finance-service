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
	
	public static class Plan {
	    public static final String API = "Manages operations on finance plans";
        public static final String UPDATE_FINANCE_STATE_OPERATION = "Updates the given user's finance state using given state map.";
        public static final String UPDATE_FINANCE_STATE_REQUEST_BODY = "The body of the request must contain the finance state as a map.";
        public static final String GET_FINANCE_STATE_PROPERTY_OPERATION = "Returns a property of the given user's finance state.";
        public static final String GET_FINANCE_STATE_PROPERTY_REQUEST_BODY = "The body of the request must contain a public key, used to encrypt the result.";
        public static final String CREATE_FINANCE_PLAN_OPERATION = "Creates a finance plan.";
        public static final String CREATE_FINANCE_PLAN_REQUEST_BODY = "The body of the request must contain the parameters used to create the finance plan:" +
                " the class name of the finance plugin used to instantiate the finance plan, the finance plan name and the other finance plan options as a map.";
        public static final String GET_FINANCE_PLAN_OPERATION = "Returns a finance plan's properties.";
        public static final String CHANGE_FINANCE_PLAN_OPTIONS_OPERATION = "Updates the options used by a finance plan.";
        public static final String CHANGE_FINANCE_PLAN_OPTIONS_REQUEST_BODY = "The body of the request must contain the finance plan options as a map.";
        public static final String REMOVE_FINANCE_PLAN_OPERATION = "Removes a finance plan.";
        public static final String PROPERTY = "A string describing the user's finance state property.";
        public static final String USER_ID = "The ID of the specific user.";
        public static final String PROVIDER = "The ID of the specific identity provider.";
        public static final String PLAN_NAME = "The name of the specific plan.";
	}
	
	public static class User {
	    public static final String API = "Manages operations that can be performed by any user.";
        public static final String REGISTER_SELF_OPERATION = "Registers the user represented by the user token to be financially managed by the given finance plan.";
        public static final String UNREGISTER_SELF_OPERATION = "Unregisters the user represented by the user token from the finance plan it currently uses." + 
                " The user financial state will remain unmanaged until the user is registered again to a finance plan.";
        public static final String CHANGE_SELF_PLAN_OPERATION = "Changes the plan used to manage the user represented by the user token.";
        public static final String PLAN_NAME = "The name of the specific plan.";
	}

	// TODO the string examples for PLAN_INFO and OPERATION need improvements
    public static class Model {
        public static final String AUTHORIZED = "true";
        public static final String PLAN_NAME = "my-plan";
        public static final String PLAN_INFO = "{\"property1\": \"value1\", \"property2\": \"value2\"}";
        public static final String USER_TOKEN = "AOgqYhXIsTsbGgJ8TiKfIxQh1SGRN03w5KesX4Vzd+2XNQbOKxHpgx4UAxgOfT+cU" +
                "uptzGLiixr4tJWaqbkPB1djx6f6P8X6tDrEZXbsyUAAj53eTi6AJ/Rh9qfdDd9XnqLjnLbU7+96gmnKIcgs9Yc1eMcDe" +
                "g4kuruMd6tl7Iarvls9Gn1zqLxyhc3K1JTtys9vhwwPZoSMHJeNG7vflCGh+YxDPx1A2d0nw+wEHDQksWct9+RKEgUkl" +
                "KYhGOQa11RMZn+Nsj7t9UZnQ5KwQEyAPBKSFgEuXShQvKXgmwLjUw0/E34qacwQzL/c4MoU6756/zf4l5h7VC8yjz7Lm" +
                "A==!^!MXfZhdcTdonx8fjVPTJ4dVEYlf4KISyP8Sad1G0FwTs/nz23vchkf54jXq/asApSEOaXejOtk3HFv/Efgxz1c8" +
                "hylPQgwpdpZEK4MvSnNrlVvVBio6D125hMi6gW4HNFWoeRJG6PQTZu/XD9gMM6Zg8M78P+qQ4dXrlFUK4DHnjbhETapl" +
                "7JTkdp2d8uipRrBObvhtKjFtU72ftu6OasQCEnRFRjpUXxXrz7yNXytp4waEsGQtrjTvlN59obPUD43u8NeURqQknXSN" +
                "6VPtFNechcCgzFU9rS7iFEspmrZTDDIo8L1Jrpz3s6UUKrEgLuapKBqojphSZd5mwNVm6PGiC2TO8gFvNX2uagmI8Tdg" +
                "PA+RvSvdVnmQq1MasTaQTHTfrjytrfgSN+QvdO/6rdrkSFdRyyZNJ2E1CQ7v9IY+WcgAzlXbTtUMzSHRmrhCdyoPFmQu" +
                "4pJQy4rhZhF+G4Oawd/KUzpf9mM5yz6OEk/Idy7vkXCoB9YXJGtyuW34gNFlDsR1lOfHOX2ScMJEHRv7Vp8VIR3MqL7O" +
                "ewGg/0m7lnSoYH+Ln9eNOzMWtfznPJT6yGvLbLnbfkW6oTW/jX9twfUEJ9rOAJNrN7mWcnUJTwI5UWu77gwmZ858Rr6E" +
                "AeHj3IJOvPlogluttQQAnhoPQ4OKUZtqjt7CslNLjeGqbWJZ5dL3gVSov306FwfF3WpviYcaZlWPJwVKjr5VGY5nC9Vm" +
                "4bZVbBwZZk/2S9DokBsgGHXNdgtwSsO0do/Qx9gP4Qk+XLa48Y0hQ+Q3+f9KgX4u3ilxumgJqK8/caTO24NlXBGfhFvn" +
                "fNkA9dsp99h/6GTkZF5B8CuOkYZTHJ+aJBjiUAmacplyEhf/zxGqpQ98sDfozp50+C5/trj0mToPo1jTjetuXIPUE+zJ" +
                "E+rQaXhyRjbXFdn0X3djUY1oUv+Q13S+NyCb8r7bxK8Ltj2J3hl2P5SVak2X462F2JJ0ttzDBVXMsN3Vws5U4cHYpn+K" +
                "kBMKN9c+Vulh0UuXOmAWD8vn1zkiMWby/aE0/uF4h4QSytU3wrlzttcNDJbDJ7qcQhT24pPH+N0mYu+HjaZDqnylpzQc" +
                "lAxph72yXytck8tCdEwCDG5+s6BAqoa4A=";
        public static final String OPERATION = "{\"operationType\":\"CREATE\",\"resourceType\":\"COMPUTE\"}";
        public static final String PLUGIN_CLASS_NAME = "cloud.fogbow.fs.core.plugins.plan.prepaid.PrePaidPlanPlugin";
        public static final String POLICY = "policy-string";
        public static final String POLICY_NOTE = "(an authorization policy)";
        public static final String USER_ID = "user-id";
        public static final String PROVIDER_ID = "provider-id";
      
    }
}
