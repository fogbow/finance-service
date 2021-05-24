package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.plugins.FinancePluginInstantiator;
import cloud.fogbow.fs.core.plugins.PlanPluginInstantiator;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.ras.core.models.RasOperation;

// TODO update documentation
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, FinancePluginInstantiator.class, 
	FsPublicKeysHolder.class, AuthenticationUtil.class, 
	PlanPluginInstantiator.class})
public class FinanceManagerTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_ID_3 = "userId3";
	private static final String USER_ID_TO_ADD_1 = "userIdToAdd1";
	private static final String USER_ID_TO_ADD_2 = "userIdToAdd1";
	private static final String USER_NAME_1 = "userName1";
	private static final String USER_NAME_2 = "userName2";
	private static final String USER_NAME_3 = "userName3";
	private static final String PROVIDER_USER_1 = "providerUserId1";
	private static final String PROVIDER_USER_2 = "providerUserId2";
	private static final String PROVIDER_USER_3 = "providerUserId3";
	private static final String PROVIDER_USER_TO_ADD_1 = "providerUserToAdd1";
	private static final String PROVIDER_USER_TO_ADD_2 = "providerUserToAdd1";
	private static final String PROPERTY_NAME_1 = "propertyName1";
	private static final String PROPERTY_VALUE_1 = "propertyValue1";
	private static final String PROPERTY_NAME_2 = "propertyName2";
	private static final String PROPERTY_VALUE_2 = "propertyValue2";
	private static final String PROPERTY_NAME_3 = "propertyName3";
	private static final String PROPERTY_VALUE_3 = "propertyValue3";
	private static final String USER_1_TOKEN = "user1Token";
	private static final String USER_2_TOKEN = "user2Token";
	private static final String USER_3_TOKEN = "user3Token";
	private static final String PLUGIN_1_NAME = "plugin1";
	private static final String PLUGIN_2_NAME = "plugin2";
	private static final String UNKNOWN_PLUGIN_NAME = "unknownplugin";
    private static final String PLUGIN_CLASS_NAME = "pluginClassName";
	private InMemoryFinanceObjectsHolder objectHolder;
	private AuthorizableUser user1;
	private AuthorizableUser user2;
	private AuthorizableUser user3;
	private SystemUser systemUser1;
	private SystemUser systemUser2;
	private SystemUser systemUser3;
	private RasOperation operation1;
	private RasOperation operation2;
	private RasOperation operation3;
    private InMemoryUsersHolder usersHolder;
    private PlanPlugin plan1;
    private PlanPlugin plan2;

	// test case: When calling the constructor, it must get
	// the names of the finance plugins from a PropertiesHolder
	// instance and call the FinancePluginInstantiator to 
	// instantiate the finance plugins.
	@Test
	public void testConstructor() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();

		new FinanceManager(objectHolder);
		
		Mockito.verify(objectHolder).getPlanPlugins();
	}
	
	// test case: When calling the constructor and the default finance plan does 
	// not exist in the database, it must call the FinancePlanFactory to create 
	// the default plan and call the DatabaseManager to save the plan.
	@Test
	public void testContructorDefaultFinancePlanDoesNotExist() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE)).thenReturn(PLUGIN_CLASS_NAME);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        PowerMockito.mockStatic(PlanPluginInstantiator.class);
        BDDMockito.given(PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, usersHolder)).willReturn(plan1);
        
        MultiConsumerSynchronizedList<PlanPlugin> emptyPluginList = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(emptyPluginList.isEmpty()).thenReturn(true);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getPlanPlugins()).thenReturn(emptyPluginList);
        Mockito.when(objectHolder.getInMemoryUsersHolder()).thenReturn(usersHolder);
        
        new FinanceManager(objectHolder);

        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE);
        
        PowerMockito.verifyStatic(PlanPluginInstantiator.class);
        PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, usersHolder);
	}
	
	// test case: When calling the isAuthorized method passing an AuthorizableUser,
	// it must check which finance plugin manages the user and call the isAuthorized 
	// method of the plugin.
	@Test
	public void testIsAuthorizedUserIsAuthorized() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		assertTrue(financeManager.isAuthorized(user1));
	}

	// TODO documentation
    @Test
    public void testIsAuthorizedUserIsNotAuthorized() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();
        setUpAuthentication();

        FinanceManager financeManager = new FinanceManager(objectHolder);

        assertFalse(financeManager.isAuthorized(user3));
    }

	// test case: When calling the isAuthorized method passing an AuthorizableUser which
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testIsAuthorizedUserIsNotManaged() throws FogbowException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.isAuthorized(user1);
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser, 
	// it must check which finance plugin manages the user and call the getUserFinanceState
	// method of the plugin.
	@Test
	public void testGetFinanceStateProperty() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		assertEquals(PROPERTY_VALUE_1, financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1));
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser
	// which is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetFinanceStatePropertyUserIsNotManaged() throws FogbowException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();

		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1);
	}
	
	// test case: When calling the addUser method, it must add the 
	// user using the correct FinancePlugin.
	@Test
	public void testAddUser() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		Map<String, String> financeOptions = new HashMap<String, String>();
		User user1 = new User(USER_ID_TO_ADD_1, PROVIDER_USER_TO_ADD_1, PLUGIN_1_NAME, financeOptions);
		User user2 = new User(USER_ID_TO_ADD_2, PROVIDER_USER_TO_ADD_2, PLUGIN_2_NAME, financeOptions);
		
		financeManager.addUser(user1);
		financeManager.addUser(user2);

		Mockito.verify(this.plan1, Mockito.times(1)).registerUser(Mockito.any(SystemUser.class));
		Mockito.verify(this.plan2, Mockito.times(1)).registerUser(Mockito.any(SystemUser.class));
	}
	
	// test case: When calling the addUser method and the FinanceManager
	// does not know the finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testAddUserUnknownPlugin() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		Map<String, String> financeOptions = new HashMap<String, String>();
		User user1 = new User(USER_ID_TO_ADD_1, PROVIDER_USER_TO_ADD_1, UNKNOWN_PLUGIN_NAME, financeOptions);
		
		financeManager.addUser(user1);
	}
	
	// test case: When calling the removeUser method, it must remove 
	// the user using the correct FinancePlugin.
	@Test
	public void testRemoveUser() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		financeManager.removeUser(USER_ID_1, PROVIDER_USER_1);
		
		Mockito.verify(plan1, Mockito.times(1)).unregisterUser(Mockito.any(SystemUser.class));
	}
	
	// test case: When calling the removeUser method and the user
	// to be removed is not managed by any finance plugin, it must 
	// throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testRemoveUserUnmanagedUser() throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		financeManager.removeUser(USER_ID_1, PROVIDER_USER_1);
	}
	
	// test case: When calling the updateFinanceState method, it must
	// change the user's finance state using the correct FinancePlugin.
	@Test
	public void testUpdateFinanceState() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.updateFinanceState(USER_ID_1, PROVIDER_USER_1, newFinanceState);
		
		Mockito.verify(plan1, Mockito.times(1)).updateUserFinanceState(Mockito.any(SystemUser.class), Mockito.any());
	}
	
	// test case: When calling the updateFinanceState method and the user
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testUpdateFinanceStateUnmanagedUser() throws InvalidParameterException, 
	ConfigurationErrorException, InternalServerErrorException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();
		
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.updateFinanceState(USER_ID_1, PROVIDER_USER_1, newFinanceState);
	}
	
	// test case: When calling the startPlugins method, it must call the startThreads
	// method of all the known finance plugins.
	@Test
	public void testStartThreads() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		financeManager.startPlugins();
		
		Mockito.verify(plan1, Mockito.times(1)).startThreads();
		Mockito.verify(plan2, Mockito.times(1)).startThreads();
	}

	// test case: When calling the stopPlugins method, it must call the stopThreads
	// method of all the known finance plugins.
	@Test
	public void testStopThreads() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();

		FinanceManager financeManager = new FinanceManager(objectHolder);

		financeManager.stopPlugins();

        Mockito.verify(plan1, Mockito.times(1)).startThreads();
        Mockito.verify(plan2, Mockito.times(1)).startThreads();
	}
	
	// test case: When calling the reset plugins method, it must get
    // the names of the finance plugins from a PropertiesHolder
    // instance and call the FinancePluginInstantiator to 
    // instantiate the finance plugins.
	@Test
	public void testResetPlugins() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
  
	    financeManager.resetPlugins();
	    
	    Mockito.verify(objectHolder).reset();
	}
	
	// test case: When calling the createFinancePlan method, it must call the 
	// FinancePlanFactory to create a new FinancePlan and call the saveFinancePlan
	// method of the DatabaseManager.
	@Test
	public void testCreateFinancePlan() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();

	    Map<String, String> planInfo = new HashMap<String, String>();

	    PowerMockito.mockStatic(PlanPluginInstantiator.class);
	    BDDMockito.given(PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, planInfo, usersHolder)).willReturn(plan1);
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
	    
	    
        financeManager.createFinancePlan(PLUGIN_CLASS_NAME, planInfo);
        
       
        Mockito.verify(objectHolder).registerPlanPlugin(plan1);
	}
	
	private void setUpFinancePlugin() throws FogbowException, ModifiedListException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		systemUser2 = new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2);
		systemUser3 = new SystemUser(USER_ID_3, USER_NAME_3, PROVIDER_USER_3);
		
		operation1 = Mockito.mock(RasOperation.class);
		operation2 = Mockito.mock(RasOperation.class);
		operation3 = Mockito.mock(RasOperation.class);

        MultiConsumerSynchronizedList<PlanPlugin> plugins = Mockito.mock(MultiConsumerSynchronizedList.class);
        this.plan1 = Mockito.mock(PlanPlugin.class);
        Mockito.when(this.plan1.isRegisteredUser(systemUser1)).thenReturn(true);
        Mockito.when(this.plan1.isRegisteredUser(systemUser2)).thenReturn(false);
        Mockito.when(this.plan1.isRegisteredUser(systemUser3)).thenReturn(true);
        Mockito.when(this.plan1.isAuthorized(systemUser1, operation1)).thenReturn(true);
        Mockito.when(this.plan1.isAuthorized(systemUser3, operation3)).thenReturn(false);
        Mockito.when(this.plan1.getUserFinanceState(systemUser1, PROPERTY_NAME_1)).thenReturn(PROPERTY_VALUE_1);
        Mockito.when(this.plan1.getUserFinanceState(systemUser3, PROPERTY_NAME_3)).thenReturn(PROPERTY_VALUE_3);
        Mockito.when(this.plan1.getName()).thenReturn(PLUGIN_1_NAME);
        
        this.plan2 = Mockito.mock(PlanPlugin.class);
        Mockito.when(this.plan2.isRegisteredUser(systemUser1)).thenReturn(false);
        Mockito.when(this.plan2.isRegisteredUser(systemUser2)).thenReturn(true);
        Mockito.when(this.plan2.isRegisteredUser(systemUser3)).thenReturn(false);
        Mockito.when(this.plan2.isAuthorized(systemUser2, operation2)).thenReturn(true);
        Mockito.when(this.plan2.getUserFinanceState(systemUser2, PROPERTY_NAME_2)).thenReturn(PROPERTY_VALUE_2);
        Mockito.when(this.plan2.getName()).thenReturn(PLUGIN_2_NAME);
        
        this.usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Mockito.when(this.objectHolder.getInMemoryUsersHolder()).thenReturn(this.usersHolder);
        
        Mockito.when(this.objectHolder.getPlanPlugins()).thenReturn(plugins);
        Mockito.when(this.objectHolder.getPlanPlugin(PLUGIN_1_NAME)).thenReturn(this.plan1);
        Mockito.when(this.objectHolder.getPlanPlugin(PLUGIN_2_NAME)).thenReturn(this.plan2);
        Mockito.when(plugins.startIterating()).thenReturn(0);
        Mockito.when(plugins.getNext(Mockito.anyInt())).thenReturn(this.plan1, this.plan2, null);
        Mockito.when(plugins.isEmpty()).thenReturn(false);

		this.user1 = Mockito.mock(AuthorizableUser.class);
		this.user2 = Mockito.mock(AuthorizableUser.class);
		this.user3 = Mockito.mock(AuthorizableUser.class);
		
		Mockito.when(this.user1.getUserToken()).thenReturn(USER_1_TOKEN);
		Mockito.when(this.user2.getUserToken()).thenReturn(USER_2_TOKEN);
		Mockito.when(this.user3.getUserToken()).thenReturn(USER_3_TOKEN);

		Mockito.when(this.user1.getOperation()).thenReturn(operation1);
		Mockito.when(this.user2.getOperation()).thenReturn(operation2);
		Mockito.when(this.user3.getOperation()).thenReturn(operation3);
	}
	
	private void setUpFinancePluginUnmanagedUser() 
	        throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		operation1 = Mockito.mock(RasOperation.class);
		
        MultiConsumerSynchronizedList<PlanPlugin> plugins = Mockito.mock(MultiConsumerSynchronizedList.class);
        this.plan1 = Mockito.mock(PlanPlugin.class);
        Mockito.when(this.plan1.isRegisteredUser(systemUser1)).thenReturn(false);
        
        this.plan2 = Mockito.mock(PlanPlugin.class);
        Mockito.when(this.plan2.isRegisteredUser(systemUser1)).thenReturn(false);
        
        this.usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Mockito.when(this.objectHolder.getInMemoryUsersHolder()).thenReturn(this.usersHolder);
        
        Mockito.when(this.objectHolder.getPlanPlugins()).thenReturn(plugins);
        Mockito.when(plugins.startIterating()).thenReturn(0);
        Mockito.when(plugins.getNext(Mockito.anyInt())).thenReturn(this.plan1, this.plan2, null);
        Mockito.when(plugins.isEmpty()).thenReturn(false);

		this.user1 = Mockito.mock(AuthorizableUser.class);
		
		Mockito.when(this.user1.getUserToken()).thenReturn(USER_1_TOKEN);
		Mockito.when(this.user1.getOperation()).thenReturn(operation1);
	}
	
	private void setUpAuthentication() throws FogbowException {
		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		FsPublicKeysHolder fsPublicKeysHolder = Mockito.mock(FsPublicKeysHolder.class);
		RSAPublicKey rasPublicKey = Mockito.mock(RSAPublicKey.class);
		Mockito.when(fsPublicKeysHolder.getRasPublicKey()).thenReturn(rasPublicKey);
		
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(fsPublicKeysHolder);
		
		PowerMockito.mockStatic(AuthenticationUtil.class);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_1_TOKEN)).willReturn(systemUser1);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_2_TOKEN)).willReturn(systemUser2);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_3_TOKEN)).willReturn(systemUser3);
	}
}
