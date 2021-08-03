package cloud.fogbow.fs.constants;

public class Messages {
	public static class Exception {
	    public static final String ADMIN_ROLE_NOT_SPECIFIED = "Admin role is not specified in the configuration file.";
	    public static final String CANNOT_ADD_NEGATIVE_CREDITS = "Cannot add negative credits value.";
	    public static final String CANNOT_CHANGE_INVOICE_STATE = "Cannot change invoice state from '%s' to '%s'.";
	    public static final String CANNOT_REMOVE_DEFAULT_FINANCE_PLAN = "Cannot remove default finance plan.";
	    public static final String FINANCE_PLAN_ALREADY_EXISTS = "Finance plan '%s' already exists.";
	    public static final String FINANCE_PLAN_HAS_REGISTERED_USERS = "Finance plan '%s' has registered users.";
		public static final String GENERIC_EXCEPTION_S = "Operation returned error: %s.";
		public static final String INVALID_COMPUTE_ITEM_FIELD = "Invalid compute item field.";
		public static final String INVALID_CONSUMER_INDEX = "Invalid consumer index.";
		public static final String INVALID_FINANCE_OPTION = "Invalid value '%s' for finance option '%s'.";
		public static final String INVALID_FINANCE_STATE_PROPERTY = "Invalid value '%s' for finance state property '%s'.";
		public static final String INVALID_NUMBER_OF_COMPUTE_ITEM_FIELDS = "Invalid number of compute item fields.";
		public static final String INVALID_NUMBER_OF_VOLUME_ITEM_FIELDS = "Invalid number of volume item fields.";
		public static final String INVALID_TIME_USED = "Invalid time used for item %s: %f.";
		public static final String INVALID_VALUE_TO_PAY = "Invalid value to pay for item %s: %f.";
		public static final String INVALID_VOLUME_ITEM_FIELD = "Invalid volume item field.";
		public static final String MISSING_FINANCE_OPTION = "Missing finance option: %s.";
		public static final String MISSING_FINANCE_STATE_PROPERTY = "Missing finance state property: %s.";
		public static final String NEGATIVE_COMPUTE_RAM = "Negative compute ram value.";
		public static final String NEGATIVE_COMPUTE_VCPU = "Negative compute vCPU value.";
		public static final String NEGATIVE_RESOURCE_ITEM_VALUE = "Negative resource item financial value.";
		public static final String NEGATIVE_VOLUME_SIZE = "Negative volume size value.";
		public static final String NO_ADMIN_SPECIFIED = "No admin specified in the configuration file.";
		public static final String NO_FINANCE_PLAN_CREATION_METHOD_PROVIDED = "No FinancePlan creation method provided in the options.";
		public static final String NO_FINANCE_PLUGIN_SPECIFIED = "No finance plugin specified in the configuration file.";
		public static final String OPERATION_NOT_IMPLEMENTED = "Received not implemented error code when accessing endpoint %s.";
		public static final String UNABLE_TO_FIND_CLASS_S = "Unable to find class %s.";
		public static final String UNABLE_TO_FIND_INVOICE = "Unable to find invoice %s.";
		public static final String UNABLE_TO_FIND_PLAN = "Unable to find plan %s.";
		public static final String UNABLE_TO_FIND_USER = "Unable to find user %s, provider %s.";
		public static final String UNABLE_TO_FIND_USER_CREDITS = "Unable to find credits for user %s, provider %s.";
		public static final String UNABLE_TO_READ_CONFIGURATION_FILE_S = "Unable to read configuration file: %s.";
		public static final String UNKNOWN_FINANCE_PROPERTY = "Unknown finance property: %s.";
		public static final String UNKNOWN_INVOICE_STATE = "Unknown invoice state: %s.";
		public static final String UNKNOWN_OPERATION_TYPE = "Unknown operation type: %s.";
		public static final String UNKNOWN_RESOURCE_ITEM = "Unknown resource item: %s.";
		public static final String UNKNOWN_RESOURCE_ITEM_TYPE = "Unknown resource item type: %s.";
		public static final String UNKNOWN_USER_STATE = "Unknown user state: %s.";
		public static final String UNMANAGED_USER = "The user %s is not managed by any finance plugin.";
		public static final String USER_ALREADY_EXISTS = "User %s.%s already exists.";
		public static final String USER_DOES_NOT_HAVE_ENOUGH_PERMISSION = "User does not have permission to perform operation.";
		public static final String USER_HAS_NOT_PAID_ALL_INVOICES = "User '%s', provider '%s', has not paid all invoices.";
		public static final String USER_IS_ALREADY_SUBSCRIBED_TO_A_PLAN = "User '%s' is already subscribed to plan '%s'.";
		public static final String USER_IS_MANAGED_BY_PLUGIN = "User '%s', provider '%s', is managed by plugin '%s'.";
		public static final String USER_IS_NOT_ADMIN = "Not-admin user trying to perform admin-only operation.";
		public static final String USER_IS_NOT_SUBSCRIBED_TO_ANY_PLAN = "User '%s' is not subscribed to any plan.";
        public static final String WRONG_POLICY_TYPE = "Wrong policy type. Type should be '%s' but is '%s'.";
        public static final String POLICY_CLASS_NOT_SPECIFIED = "Policy class is not specified in the configuration file.";
        public static final String POLICY_FILE_NAME_NOT_SPECIFIED = "Policy file name is not specified in the configuration file.";
	}
	
	public static class Log {
		public static final String ADDING_USER = "Adding user: '%s', provider '%s', plan '%s'.";
		public static final String ADDING_SELF = "Adding user '%s', provider '%s', plan '%s'.";
		public static final String CHANGING_OPTIONS = "Changing finance options for plan: %s.";
		public static final String CHANGING_SELF_PLAN = "Changing self plan. User '%s', provider '%s', to plan '%s'.";
		public static final String CHANGING_USER_PLAN = "Changing user '%s', provider '%s', to plan '%s'.";
		public static final String CREATING_FINANCE_PLAN = "Creating finance plan '%s' using plugin '%s'.";
		public static final String FAILED_TO_GENERATE_INVOICE = "Failed to generate invoice. Error message: %s.";
		public static final String FAILED_TO_GENERATE_INVOICE_FOR_USER = "Failed to generate invoice for user %s. Error message: %s.";
		public static final String FAILED_TO_DEDUCT_CREDITS = "Failed to deduct credits. Error message: %s.";
		public static final String FAILED_TO_DEDUCT_CREDITS_FOR_USER = "Failed to deduct credits for user %s. Error message: %s.";
		public static final String FAILED_TO_MANAGE_RESOURCES = "Failed to manage resources. Error message: %s.";
		public static final String FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER = "Failed to pause resources for user %s. Error message: %s.";
		public static final String FAILED_TO_RESUME_USER_RESOURCES_FOR_USER = "Failed to resume resources for user %s. Error message: %s.";
		public static final String GETTING_FINANCE_STATE = "Getting finance state: user '%s', provider '%s' and property '%s'.";
		public static final String GETTING_FINANCE_PLAN = "Getting finance plan: %s.";
		public static final String GET_PUBLIC_KEY = "Get public key received.";
		public static final String PROPERTY_FILE_S_NOT_FOUND = "Property file '%s' not found.";
		public static final String RECEIVING_GET_VERSION = "Receiving get version.";
		public static final String RECEIVED_ADD_SELF = "Received addSelf.";
		public static final String RECEIVED_CHANGE_SELF_PLAN = "Received changeSelfPlan.";
		public static final String RECEIVED_UNREGISTER_SELF = "Received unregisterSelf.";
		public static final String RELOADING_AUTHORIZATION_PLUGIN = "Reloading authorization plugin.";
		public static final String RELOADING_CONFIGURATION = "Reloading service configuration.";
		public static final String RELOADING_FINANCE_PLUGINS = "Reloading finance plugins.";
		public static final String RELOADING_FS_KEYS_HOLDER = "Reloading service keys.";
		public static final String RELOADING_PROPERTIES_HOLDER = "Reloading properties holder.";
		public static final String RELOADING_PUBLIC_KEYS_HOLDER = "Reloading public keys holder.";
		public static final String REMOVING_FINANCE_PLAN = "Removing finance plan: %s.";
		public static final String UNREGISTERING_SELF = "Unregistering self: user '%s', provider '%s'.";
		public static final String REMOVING_USER = "Removing user '%s', provider '%s'.";
		public static final String SETTING_POLICY = "Setting policy.";
		public static final String STOPPING_FINANCE_PLUGINS = "Stopping finance plugins.";
		public static final String UNABLE_TO_CLOSE_FILE_S = "Unable to close file '%s'.";
		public static final String UNABLE_TO_FIND_USER = "Unable to find user '%s', provider '%s'.";
		public static final String UNREGISTERING_USER = "Unregistering user '%s', provider '%s'.";
		public static final String UPDATING_FINANCE_STATE = "Updating finance state of user '%s', provider '%s'.";
		public static final String UPDATING_FINANCE_PLAN = "Updating finance plan: %s.";
		public static final String UPDATING_POLICY = "Updating policy.";
		public static final String USER_LIST_CHANGED_SKIPPING_CREDITS_DEDUCTION = "User list changed. Skipping credits deduction.";
		public static final String USER_LIST_CHANGED_SKIPPING_INVOICE_GENERATION = "User list changed. Skipping invoice generation.";
        public static final String USER_LIST_CHANGED_SKIPPING_USER_PAYMENT_STATE_CHECK = "User list changed. Skipping user payment state check.";
	}
}
