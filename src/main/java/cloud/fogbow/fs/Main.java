package cloud.fogbow.fs;

import java.util.ArrayList;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.ApplicationFacade;
import cloud.fogbow.fs.core.AuthorizationPluginInstantiator;
import cloud.fogbow.fs.core.FinanceManager;
import cloud.fogbow.fs.core.FinancePluginInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.SynchronizationManager;

@Component
public class Main implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
        ServiceAsymmetricKeysHolder.getInstance().setPublicKeyFilePath(publicKeyFilePath);
        ServiceAsymmetricKeysHolder.getInstance().setPrivateKeyFilePath(privateKeyFilePath);
		
        AuthorizationPlugin<FsOperation> authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin();
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        
		DatabaseManager databaseManager = new DatabaseManager();
		ApplicationFacade.getInstance().setDatabaseManager(databaseManager);
		
		// TODO refactor
		String financePluginsString = PropertiesHolder.getInstance()
				.getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

		// FIXME constant
		for (String financePluginClassName : financePluginsString.split(",")) {
			financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName, databaseManager));
		}

		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		
		SynchronizationManager synchronizationManager = new SynchronizationManager();
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
		
		financeManager.startPlugins();
	}
}
