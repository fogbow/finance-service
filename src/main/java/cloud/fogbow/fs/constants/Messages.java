package cloud.fogbow.fs.constants;

public class Messages {
	public static class Exception {
		public static final String GENERIC_EXCEPTION_S = "Operation returned error: %s.";
		public static final String INVALID_COMPUTE_ITEM_FIELD = "Invalid compute item field.";
		public static final String INVALID_NUMBER_OF_COMPUTE_ITEM_FIELDS = "Invalid number of compute item fields.";
		public static final String INVALID_NUMBER_OF_VOLUME_ITEM_FIELDS = "Invalid number of volume item fields.";
		public static final String INVALID_VOLUME_ITEM_FIELD = "Invalid volume item field.";
		public static final String NEGATIVE_RESOURCE_ITEM_VALUE = "Negative resource item financial value.";
		public static final String NO_ADMIN_SPECIFIED = "No admin specified in the configuration file.";
		public static final String NO_FINANCE_PLUGIN_SPECIFIED = "No finance plugin specified in the configuration file.";
		public static final String UNABLE_TO_FIND_CLASS_S = "Unable to find class %s.";
		public static final String UNKNOWN_FINANCE_PROPERTY = "Unknown finance property: %s.";
		public static final String UNKNOWN_INVOICE_STATE = "Unknown invoice state: %s.";
		public static final String UNKNOWN_RESOURCE_ITEM = "Unknown resource item: %s.";
		public static final String UNKNOWN_RESOURCE_ITEM_TYPE = "Unknown resource item type: %s.";
		public static final String UNMANAGED_USER = "The user %s is not managed by any finance plugin.";
		public static final String USER_IS_NOT_ADMIN = "Not-admin user trying to perform admin-only operation.";
	}
	
	public static class Log {
		public static final String ADDING_USER = "Adding user: %s.";
		public static final String CHANGING_OPTIONS = "Changing finance options for user: %s.";
		public static final String CREATING_FINANCE_PLAN = "Creating finance plan: %s.";
		public static final String FAILED_TO_GENERATE_INVOICE = "Failed to generate invoice for user %s. Error message: %s.";
		public static final String FAILED_TO_DEDUCT_CREDITS = "Failed to deduct credits for user %s. Error message: %s.";
		public static final String FAILED_TO_PAUSE_USER_RESOURCES = "Failed to pause resources for user %s. Error message: %s.";
		public static final String FAILED_TO_RESUME_USER_RESOURCES = "Failed to resume resources for user %s. Error message: %s.";
		public static final String GETTING_FINANCE_STATE = "Getting finance state: user '%s' and property '%s'";
		public static final String GETTING_FINANCE_PLAN = "Getting finance plan: %s.";
		public static final String GET_PUBLIC_KEY = "Get public key received.";
		public static final String RELOADING_AUTHORIZATION_PLUGIN = "Reloading authorization plugin.";
		public static final String RELOADING_CONFIGURATION = "Reloading service configuration.";
		public static final String RELOADING_FINANCE_PLUGINS = "Reloading finance plugins.";
		public static final String RELOADING_FS_KEYS_HOLDER = "Reloading service keys.";
		public static final String RELOADING_PROPERTIES_HOLDER = "Reloading properties holder.";
		public static final String RELOADING_PUBLIC_KEYS_HOLDER = "Reloading public keys holder.";
		public static final String REMOVING_FINANCE_PLAN = "Removing finance plan: %s.";
		public static final String REMOVING_USER = "Removing user: %s.";
		public static final String UPDATING_FINANCE_STATE = "Updating finance state of user: %s.";
		public static final String UPDATING_FINANCE_PLAN = "Updating finance plan: %s.";
	}
}
