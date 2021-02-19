package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

import org.apache.log4j.Logger;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.SynchronizationManager;

public class ApplicationFacade {
	
	private static Logger LOGGER = Logger.getLogger(ApplicationFacade.class);
	private static ApplicationFacade instance;
	private FinanceManager financeManager;
	private DatabaseManager databaseManager;
	private AuthorizationPlugin<FsOperation> authorizationPlugin;
	private SynchronizationManager synchronizationManager;
	
	private ApplicationFacade() {
		
	}

	public void setAuthorizationPlugin(AuthorizationPlugin<FsOperation> authorizationPlugin) {
		this.authorizationPlugin = authorizationPlugin;
	}
	
	public static ApplicationFacade getInstance() {
		if (instance == null) {
			instance = new ApplicationFacade();
		}
		return instance;
	}

	public void setFinanceManager(FinanceManager financeManager) { 
		this.financeManager = financeManager;
	}
	
	public void setDatabaseManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
		
	}
	
	public void setSynchronizationManager(SynchronizationManager synchronizationManager) {
		this.synchronizationManager = synchronizationManager;
	}
	
	public boolean isAuthorized(AuthorizableUser user) {
		synchronizationManager.startOperation();
		
		try { 
			return this.financeManager.isAuthorized(user);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void addUser(String userToken, User user) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.ADDING_USER, user.getUserId()));
		
		authenticateAndAuthorize(userToken);
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.addUser(user);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void removeUser(String userToken, String userId) throws UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.REMOVING_USER, userId));
		
		authenticateAndAuthorize(userToken);
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.removeUser(userId);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void changeOptions(String userToken, String userId, HashMap<String, String> financeOptions) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.CHANGING_OPTIONS, userId));
		
		authenticateAndAuthorize(userToken);
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.changeOptions(userId, financeOptions);			
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void updateFinanceState(String userToken, String userId, HashMap<String, String> financeState) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.UPDATING_FINANCE_STATE, userId));
		
		authenticateAndAuthorize(userToken);
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.updateFinanceState(userId, financeState);			
		} finally {
			synchronizationManager.finishOperation();
		}
	}
	
	public void reload(String userToken) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.RELOADING_CONFIGURATION));
		
		authenticateAndAuthorize(userToken);
		synchronizationManager.setAsReloading();
		
		try {
			synchronizationManager.waitForRequests();
	        
			LOGGER.info(Messages.Log.RELOADING_PROPERTIES_HOLDER);
			PropertiesHolder.reset();
			
			LOGGER.info(Messages.Log.RELOADING_PUBLIC_KEYS_HOLDER);
	        FsPublicKeysHolder.reset();
	        
	        LOGGER.info(Messages.Log.RELOADING_FS_KEYS_HOLDER);
	        String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
	        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
	        ServiceAsymmetricKeysHolder.reset(publicKeyFilePath, privateKeyFilePath);
			
	        LOGGER.info(Messages.Log.RELOADING_AUTHORIZATION_PLUGIN);
			this.authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin();

			LOGGER.info(Messages.Log.RELOADING_FINANCE_PLUGINS);
			this.financeManager = new FinanceManager(databaseManager);
			financeManager.startPlugins();
		} finally {
			synchronizationManager.finishReloading();
		}
	}
	
	private void authenticateAndAuthorize(String userToken)
			throws FogbowException, UnauthenticatedUserException, UnauthorizedRequestException {
		RSAPublicKey asPublicKey = FsPublicKeysHolder.getInstance().getAsPublicKey();
        SystemUser systemUser = AuthenticationUtil.authenticate(asPublicKey, userToken);
        FsOperation operation = new FsOperation();
        this.authorizationPlugin.isAuthorized(systemUser, operation);
	}
}
