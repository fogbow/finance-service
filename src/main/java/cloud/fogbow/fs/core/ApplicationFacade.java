package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
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
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

// TODO add logging
public class ApplicationFacade {
	
	private static Logger LOGGER = Logger.getLogger(ApplicationFacade.class);
	private static ApplicationFacade instance;
	private FinanceManager financeManager;
	private DatabaseManager databaseManager;
	private AuthorizationPlugin<FsOperation> authorizationPlugin;
	private boolean reloading;
	private long onGoingRequests;
	
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
	
	public boolean isAuthorized(AuthorizableUser user) {
		startOperation();
		
		try { 
			return this.financeManager.isAuthorized(user);
		} finally {
			finishOperation();
		}
	}

	public void addUser(String userToken, User user) throws FogbowException {
		authenticateAndAuthorize(userToken);
		startOperation();
		
		try {
			this.financeManager.addUser(user);
		} finally {
			finishOperation();
		}
	}

	public void removeUser(String userToken, String userId) throws UnauthorizedRequestException, FogbowException {
		authenticateAndAuthorize(userToken);
		startOperation();
		
		try {
			this.financeManager.removeUser(userId);
		} finally {
			finishOperation();
		}
	}

	public void changeOptions(String userToken, String userId, HashMap<String, String> financeOptions) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		authenticateAndAuthorize(userToken);
		startOperation();
		
		try {
			this.financeManager.changeOptions(userId, financeOptions);			
		} finally {
			finishOperation();
		}
	}

	public void updateFinanceState(String userToken, String userId, HashMap<String, String> financeState) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		authenticateAndAuthorize(userToken);
		startOperation();
		
		try {
			this.financeManager.updateFinanceState(userId, financeState);			
		} finally {
			finishOperation();
		}
	}

	
	public void reload(String userToken) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		authenticateAndAuthorize(userToken);
		setAsReloading();
		
		try {
	        while (this.onGoingRequests != 0) {
	            try {
	                Thread.sleep(10);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
			
	        
			PropertiesHolder.reset();
	        FSPublicKeysHolder.reset();
	        
	        String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
	        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
	        ServiceAsymmetricKeysHolder.reset(publicKeyFilePath, privateKeyFilePath);
			
			this.authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin();
			
			// TODO refactor
			String financePluginsString = PropertiesHolder.getInstance()
					.getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
			ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

			// FIXME constant
			for (String financePluginClassName : financePluginsString.split(",")) {
				LOGGER.info(financePluginClassName);
				financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName, databaseManager));
			}
			
			this.financeManager = new FinanceManager(financePlugins, databaseManager);
			
			financeManager.startPlugins();
		} finally {
			finishReloading();
		}
	}
	
	private void authenticateAndAuthorize(String userToken)
			throws FogbowException, UnauthenticatedUserException, UnauthorizedRequestException {
		RSAPublicKey asPublicKey = FSPublicKeysHolder.getInstance().getAsPublicKey();
        SystemUser systemUser = AuthenticationUtil.authenticate(asPublicKey, userToken);
        FsOperation operation = new FsOperation();
        this.authorizationPlugin.isAuthorized(systemUser, operation);
	}
	
	private void setAsReloading() {
        this.reloading = true;
    }
    
    private void finishReloading() {
    	this.reloading = false;
    }
	
    private void startOperation() {
        while (reloading)
            ;
        synchronized (this) {
            this.onGoingRequests++;
        }
    }
    
    private synchronized void finishOperation() {
        this.onGoingRequests--;
    }
}
