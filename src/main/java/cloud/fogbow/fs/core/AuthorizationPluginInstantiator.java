package cloud.fogbow.fs.core;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.plugins.authorization.AdminAuthorizationPlugin;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

public class AuthorizationPluginInstantiator {
	private static FsClassFactory classFactory = new FsClassFactory();
	
	public static AuthorizationPlugin<FsOperation> getAuthorizationPlugin() throws ConfigurationErrorException {
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY)) {
            String className = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
            return (AuthorizationPlugin<FsOperation>) classFactory.createPluginInstance(className);
        } else {
            return new AdminAuthorizationPlugin();
        }
	}
}
