package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.SynchronizationManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FsPublicKeysHolder.class, AuthenticationUtil.class})
public class ApplicationFacadeTest {

	private String adminId = "adminId";
	private String adminUserName = "adminUserName";
	private String adminProvider = "adminProvider";
	private String adminToken = "token";
	
	private String userIdToAdd = "userIdToAdd";
	private String userProviderToAdd = "userProviderToAdd";
	private Map<String, String> userFinanceOptionsToAdd = new HashMap<String, String>();

	private String userIdToRemove = "userIdToRemove";
	
	private String userIdToChange = "userIdToChange";
	private HashMap<String, String> newOptions = new HashMap<String, String>();
	private HashMap<String, String> newState = new HashMap<String, String>();
	
	private FsPublicKeysHolder keysHolder;
	private RSAPublicKey asPublicKey;
	private User user;
	private FinanceManager financeManager;
	private SynchronizationManager synchronizationManager;
	private SystemUser systemUser;
	private FsOperation operation;
	private AuthorizationPlugin<FsOperation> authorizationPlugin;

	// test case: When calling the addUser method, it must authorize the 
	// operation and call the FinanceManager. Also, it must start and finish 
	// operations correctly using the SynchronizationManager.
	@Test
	public void testAddUser() throws FogbowException {
		setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization();
        setUpApplicationFacade();
        
        this.user = new User(userIdToAdd, userProviderToAdd, userFinanceOptionsToAdd);
        
        ApplicationFacade.getInstance().addUser(adminToken, user);
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
        Mockito.verify(financeManager, Mockito.times(1)).addUser(user);
	}
	
	// test case: When calling the addUser method, if the call to 
	// FinanceManager.addUser throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testAddUserFinishesOperationIfOperationFails() throws FogbowException  {
		setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization();
        
        this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);
        
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        ApplicationFacade.getInstance().setFinanceManager(financeManager);
        ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
        
        this.user = new User(userIdToAdd, userProviderToAdd, userFinanceOptionsToAdd);
        Mockito.doThrow(FogbowException.class).when(this.financeManager).addUser(user);
        
        try {
			ApplicationFacade.getInstance().addUser(adminToken, user);
			Assert.fail("addUser is expected to throw exception.");
		} catch (FogbowException e) {
		}
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
	// test case: When calling the removeUser method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testRemoveUser() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();
		setUpApplicationFacade();

		ApplicationFacade.getInstance().removeUser(adminToken, this.userIdToRemove);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).removeUser(this.userIdToRemove);
	}
	
	// test case: When calling the removeUser method, if the call to
	// FinanceManager.removeUser throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testRemoveUserFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).removeUser(userIdToRemove);

		try {
			ApplicationFacade.getInstance().removeUser(adminToken, userIdToRemove);
			Assert.fail("removeUser is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}

	// test case: When calling the changeOptions method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testChangeOptions() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();
		setUpApplicationFacade();

		ApplicationFacade.getInstance().changeOptions(adminToken, this.userIdToChange, this.newOptions);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).changeOptions(this.userIdToChange, this.newOptions);
	}
	
	// test case: When calling the changeOptions method, if the call to
	// FinanceManager.changeOptions throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testChangeOptionsFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).changeOptions(this.userIdToChange, this.newOptions);

		try {
			ApplicationFacade.getInstance().changeOptions(this.adminToken, this.userIdToChange, this.newOptions);
			Assert.fail("changeOptions is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
	// test case: When calling the updateFinanceState method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testUpdateFinanceState() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();
		setUpApplicationFacade();

		ApplicationFacade.getInstance().updateFinanceState(adminToken, this.userIdToChange, this.newState);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).updateFinanceState(this.userIdToChange, this.newState);
	}
	
	// test case: When calling the updateFinanceState method, if the call to
	// FinanceManager.updateFinanceState throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testUpdateFinanceStateFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization();

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).updateFinanceState(this.userIdToChange, this.newState);

		try {
			ApplicationFacade.getInstance().updateFinanceState(this.adminToken, this.userIdToChange, this.newState);
			Assert.fail("updateFinanceState is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
	// TODO add reload test
	
	private void setUpApplicationFacade() {
		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);
        
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        ApplicationFacade.getInstance().setFinanceManager(financeManager);
        ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
	}

	private void setUpPublicKeysHolder() throws FogbowException {
		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		this.keysHolder = Mockito.mock(FsPublicKeysHolder.class);
		this.asPublicKey = Mockito.mock(RSAPublicKey.class);
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(keysHolder);
		Mockito.when(keysHolder.getAsPublicKey()).thenReturn(asPublicKey);
	}
	
	private void setUpAuthentication() throws UnauthenticatedUserException {
		PowerMockito.mockStatic(AuthenticationUtil.class);
		this.systemUser = new SystemUser(adminId, adminUserName, adminProvider);
		BDDMockito.given(AuthenticationUtil.authenticate(asPublicKey, adminToken)).willReturn(systemUser);
	}
	
	private void setUpAuthorization() throws UnauthorizedRequestException {
		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
        this.operation = new FsOperation();
        Mockito.when(authorizationPlugin.isAuthorized(systemUser, operation)).thenReturn(true);
	}
}
