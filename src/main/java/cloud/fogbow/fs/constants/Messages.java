package cloud.fogbow.fs.constants;

public class Messages {
	public static class Exception {
		public static final String NO_ADMIN_SPECIFIED = "No admin specified in the configuration file.";
		public static final String NO_FINANCE_PLUGIN_SPECIFIED = "No finance plugin specified in the configuration file.";
		public static final String UNABLE_TO_FIND_CLASS_S = "Unable to find class %s.";
		public static final String USER_IS_NOT_ADMIN = "Not-admin user trying to perform admin-only operation.";
		public static final String UNMANAGED_USER = "The user %s is not managed by any finance plugin.";
	}
	
	public static class Log {
		public static final String ADDING_USER = "Adding user: %s.";
		public static final String CHANGING_OPTIONS = "Changing finance options for user: %s.";
		public static final String FAILED_TO_GENERATE_INVOICE = "Failed to generate invoice for user %s. Error message: %s.";
		public static final String FAILED_TO_DEDUCT_CREDITS = "Failed to deduct credits for user %s. Error message: %s.";
		public static final String GETTING_FINANCE_STATE = "Getting finance state: user '%s' and property '%s'";
		public static final String RELOADING_AUTHORIZATION_PLUGIN = "Reloading authorization plugin.";
		public static final String RELOADING_CONFIGURATION = "Reloading service configuration.";
		public static final String RELOADING_FINANCE_PLUGINS = "Reloading finance plugins.";
		public static final String RELOADING_FS_KEYS_HOLDER = "Reloading service keys.";
		public static final String RELOADING_PROPERTIES_HOLDER = "Reloading properties holder.";
		public static final String RELOADING_PUBLIC_KEYS_HOLDER = "Reloading public keys holder.";
		public static final String REMOVING_USER = "Removing user: %s.";
		public static final String UPDATING_FINANCE_STATE = "Updating finance state of user: %s.";
	}
}
