package cloud.fogbow.fs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.core.ApplicationFacade;
import cloud.fogbow.fs.core.AuthorizationPluginInstantiator;
import cloud.fogbow.fs.core.FinanceManager;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.FinanceDataProtector;
import cloud.fogbow.fs.core.util.SynchronizationManager;

@Component
public class Main implements ApplicationRunner {

    @Autowired
    private DatabaseManager databaseManager;
    
	@Override
	public void run(ApplicationArguments args) throws Exception {
		String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
        ServiceAsymmetricKeysHolder.getInstance().setPublicKeyFilePath(publicKeyFilePath);
        ServiceAsymmetricKeysHolder.getInstance().setPrivateKeyFilePath(privateKeyFilePath);
		
        AuthorizationPlugin<FsOperation> authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin();
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        
        InMemoryUsersHolder usersHolder = new InMemoryUsersHolder(databaseManager);
		InMemoryFinanceObjectsHolder objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder);
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		
		FinanceDataProtector financeDataProtector = new FinanceDataProtector();
		ApplicationFacade.getInstance().setFinanceDataProtector(financeDataProtector);
		
		SynchronizationManager synchronizationManager = new SynchronizationManager();
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
		
		financeManager.startPlugins();
	}
}
