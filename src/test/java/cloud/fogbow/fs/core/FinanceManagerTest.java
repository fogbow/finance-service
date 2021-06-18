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
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.plugins.PlanPluginInstantiator;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.ras.core.models.RasOperation;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, FsPublicKeysHolder.class,
    AuthenticationUtil.class, PlanPluginInstantiator.class})
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
    private static final String PLAN_NAME = "planName";
    private static final String NEW_PLAN_NAME = "newPlanName";
	private InMemoryFinanceObjectsHolder objectHolder;
	private SystemUser systemUser1;
	private SystemUser systemUser2;
	private SystemUser systemUser3;
	private RasOperation operation1;
	private RasOperation operation2;
	private RasOperation operation3;
    private InMemoryUsersHolder usersHolder;
    private PersistablePlanPlugin plan1;
    private PersistablePlanPlugin plan2;
    private MultiConsumerSynchronizedList<PersistablePlanPlugin> plugins;
    private FinanceUser financeUser1;
    private FinanceUser financeUser2;
    private FinanceUser financeUser3;

	// test case: When calling the constructor, it must get
	// the finance plans list from the InMemoryFinanceObjectsHolder.
	@Test
	public void testConstructor() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();

		new FinanceManager(objectHolder);
		
		Mockito.verify(objectHolder).getPlanPlugins();
	}
	
	// test case: When calling the constructor and the list of finance plans acquired from
	// the InMemoryFinanceObjectsHolder is empty, it must call the PlanPluginInstantiator to
	// create the default plan and call the InMemoryFinanceObjectsHolder to register the plan.
	@Test
	public void testContructorDefaultFinancePlanDoesNotExist() 
	        throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE)).thenReturn(PLUGIN_CLASS_NAME);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_PLAN_NAME)).thenReturn(PLAN_NAME);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        PowerMockito.mockStatic(PlanPluginInstantiator.class);
        BDDMockito.given(PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, PLAN_NAME, 
                usersHolder)).willReturn(plan1);
        
        MultiConsumerSynchronizedList<PersistablePlanPlugin> emptyPluginList = 
                Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(emptyPluginList.isEmpty()).thenReturn(true);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getPlanPlugins()).thenReturn(emptyPluginList);
        Mockito.when(objectHolder.getInMemoryUsersHolder()).thenReturn(usersHolder);
        
        new FinanceManager(objectHolder);

        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE);
        
        PowerMockito.verifyStatic(PlanPluginInstantiator.class);
        PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, PLAN_NAME, usersHolder);
        
        Mockito.verify(objectHolder).registerPlanPlugin(plan1);
	}
	
	// test case: When calling the isAuthorized method passing a SystemUser,
	// it must check which finance plan manages the user and call the isAuthorized 
	// method of the plan. If the user is authorized by the plan, the method must
	// return true.
	@Test
	public void testIsAuthorizedUserIsAuthorized() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		assertTrue(financeManager.isAuthorized(systemUser1, operation1));
	}

	// test case: When calling the isAuthorized method passing a SystemUser,
    // it must check which finance plan manages the user and call the isAuthorized 
    // method of the plan. If the user is not authorized by the plan, the method must
    // return false.
    @Test
    public void testIsAuthorizedUserIsNotAuthorized() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();
        setUpAuthentication();

        FinanceManager financeManager = new FinanceManager(objectHolder);

        assertFalse(financeManager.isAuthorized(systemUser3, operation3));
    }

	// test case: When calling the isAuthorized method passing a SystemUser which
	// is not managed by any finance plan, it must throw an InvalidParameterException.
	@Test
	public void testIsAuthorizedUserIsNotManaged() throws FogbowException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		assertFalse(financeManager.isAuthorized(systemUser1, operation1));
	}
	
	// test case: When calling the isAuthorized method, if the finance plans list throws a 
	// ModifiedListException while searching for the correct finance plan, the method must
	// restart the iteration over the finance plans list.
	@Test
	public void testIsAuthorizedModifiedListExceptionIsThrown() throws ModifiedListException, FogbowException {
        setUpFinancePlugin();
        setUpAuthentication();
	    
	    Mockito.when(plugins.getNext(Mockito.anyInt())).
	    thenReturn(this.plan1).
	    thenThrow(new ModifiedListException()).
	    thenReturn(this.plan1, this.plan2, null);
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
	    assertTrue(financeManager.isAuthorized(systemUser2, operation2));
	}
	
	// test case: When calling the getFinanceStateProperty method passing a SystemUser, 
	// it must call the method getFinanceState of the correct FinanceUser.
	@Test
	public void testGetFinanceStateProperty() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		assertEquals(PROPERTY_VALUE_1, financeManager.getFinanceStateProperty(systemUser1, PROPERTY_NAME_1));
	}
	
	// test case: When calling the getFinanceStateProperty method passing a SystemUser which does not
	// exist in the system, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetFinanceStatePropertyUserDoesNotExist() throws FogbowException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();

		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.getFinanceStateProperty(systemUser1, PROPERTY_NAME_1);
	}
	
	// test case: When calling the addUser method, it must add the 
	// user using the correct finance plan.
	@Test
	public void testAddUser() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);

		financeManager.addUser(new SystemUser(USER_ID_TO_ADD_1, USER_ID_TO_ADD_1, 
		        PROVIDER_USER_TO_ADD_1), PLUGIN_1_NAME);
		financeManager.addUser(new SystemUser(USER_ID_TO_ADD_2, USER_ID_TO_ADD_2, 
		        PROVIDER_USER_TO_ADD_2), PLUGIN_2_NAME);

		Mockito.verify(this.plan1, Mockito.times(1)).registerUser(Mockito.any(SystemUser.class));
		Mockito.verify(this.plan2, Mockito.times(1)).registerUser(Mockito.any(SystemUser.class));
	}
	
	// test case: When calling the addUser method and the FinanceManager
	// does not know the finance plan, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testAddUserUnknownPlugin() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		financeManager.addUser(new SystemUser(USER_ID_TO_ADD_1, USER_ID_TO_ADD_1, 
		        PROVIDER_USER_TO_ADD_1), UNKNOWN_PLUGIN_NAME);
	}
	
	// test case: When calling the addUser method, if the finance plans list throws a 
    // ModifiedListException while searching for the correct finance plan, the method must
    // restart the iteration over the finance plans list.
	@Test
	public void testAddUserModifiedListExceptionIsThrown() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        Mockito.when(plugins.getNext(Mockito.anyInt())).
        thenReturn(this.plan1).
        thenThrow(new ModifiedListException()).
        thenReturn(this.plan1, this.plan2, null);
        
        FinanceManager financeManager = new FinanceManager(objectHolder);

        financeManager.addUser(new SystemUser(USER_ID_TO_ADD_2, USER_ID_TO_ADD_2, 
                PROVIDER_USER_TO_ADD_2), PLUGIN_2_NAME);

        Mockito.verify(this.plan2, Mockito.times(1)).registerUser(Mockito.any(SystemUser.class));
	}

	// test case: When calling the removeUser method, it must check if the user is 
	// not managed by any finance plan and then call the InMemoryUsersHolder to
	// remove the user.
	@Test
	public void testRemoveUser() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();
	    
	    Mockito.when(this.plan1.isRegisteredUser(systemUser1)).thenReturn(false);
	    Mockito.when(this.plan2.isRegisteredUser(systemUser1)).thenReturn(false);
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
	    
	    financeManager.removeUser(systemUser1);
	    
	    Mockito.verify(usersHolder).removeUser(USER_ID_1, PROVIDER_USER_1);
	}
	
	// test case: When calling the removeUser method and the user
    // to be removed does not exist in the system, it must 
    // throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveUserUnmanagedUser() 
            throws ConfigurationErrorException, InvalidParameterException,
            InternalServerErrorException, ModifiedListException {
        setUpFinancePluginUnmanagedUser();
        
        FinanceManager financeManager = new FinanceManager(objectHolder);
        
        financeManager.removeUser(systemUser1);
    }
	
	// test case: When calling the removeUser method and the user
    // to be removed is still managed by a finance plan, it must 
    // throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testRemoveUserStillManagedByPlan() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        FinanceManager financeManager = new FinanceManager(objectHolder);

        financeManager.removeUser(systemUser1);
	}
	
	// test case: When calling the unregisterUser method, it must
	// find the finance plan which manages the user and then 
	// call the unregisterUser method of the plan.
	@Test
	public void testUnregisterUser() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        FinanceManager financeManager = new FinanceManager(objectHolder);

        financeManager.unregisterUser(systemUser1);

        Mockito.verify(this.plan1).unregisterUser(new SystemUser(USER_ID_1, USER_ID_1, PROVIDER_USER_1));
	}
	
	// test case: When calling the changePlan method, it must
	// find the finance plan which manages the user and then
	// call the changePlan method of the plan.
	@Test
	public void testChangePlan() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
	    
	    financeManager.changePlan(systemUser1, NEW_PLAN_NAME);
	    
	    Mockito.verify(this.plan1).changePlan(new SystemUser(USER_ID_1, USER_ID_1, PROVIDER_USER_1), NEW_PLAN_NAME);
	}
	
	// test case: When calling the updateFinanceState method, it must call
	// the method updateFinanceState of the correct user and then call the
	// InMemoryUsersHolder to save the user.
	@Test
	public void testUpdateFinanceState() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.updateFinanceState(systemUser1, newFinanceState);
		
		Mockito.verify(financeUser1).updateFinanceState(newFinanceState);
		Mockito.verify(this.usersHolder).saveUser(financeUser1);
	}
	
	// test case: When calling the updateFinanceState method and the user does not
	// exist in the system, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testUpdateFinanceStateUserDoesNotExist() throws InvalidParameterException, 
	ConfigurationErrorException, InternalServerErrorException, ModifiedListException {
		setUpFinancePluginUnmanagedUser();
		
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		financeManager.updateFinanceState(systemUser1, newFinanceState);
	}
	
	// test case: When calling the startPlugins method, it must call the startThreads
	// method of all the known finance plans.
	@Test
	public void testStartThreads() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
		Mockito.when(plan1.isStarted()).thenReturn(false);
		Mockito.when(plan2.isStarted()).thenReturn(false);
		
		FinanceManager financeManager = new FinanceManager(objectHolder);
		
		financeManager.startPlugins();
		
		Mockito.verify(plan1, Mockito.times(1)).startThreads();
		Mockito.verify(plan2, Mockito.times(1)).startThreads();
	}
	
	// test case: When calling the startPlugins method, if the finance plans list throws a 
    // ModifiedListException while iterating over the plans, the method must restart the
	// iteration over the finance plan list and must not call the startThreads method of 
	// an already started plan.
	@Test
	public void testStartThreadsModifiedListException() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        Mockito.when(plan1.isStarted()).thenReturn(false, true);
        Mockito.when(plan2.isStarted()).thenReturn(false);
        
        Mockito.when(plugins.getNext(Mockito.anyInt())).
        thenReturn(this.plan1).
        thenThrow(new ModifiedListException()).
        thenReturn(this.plan1, this.plan2, null);
        
        FinanceManager financeManager = new FinanceManager(objectHolder);

        financeManager.startPlugins();

        Mockito.verify(plan1, Mockito.times(1)).startThreads();
        Mockito.verify(plan2, Mockito.times(1)).startThreads();
	}

	// test case: When calling the stopPlugins method, it must call the stopThreads
	// method of all the known finance plans.
	@Test
	public void testStopThreads() throws FogbowException, ModifiedListException {
		setUpFinancePlugin();
		
        Mockito.when(plan1.isStarted()).thenReturn(true);
        Mockito.when(plan2.isStarted()).thenReturn(true);

		FinanceManager financeManager = new FinanceManager(objectHolder);

		financeManager.stopPlugins();

        Mockito.verify(plan1, Mockito.times(1)).stopThreads();
        Mockito.verify(plan2, Mockito.times(1)).stopThreads();
	}
	
	// test case: When calling the stopPlugins method, if the finance plans list throws a 
    // ModifiedListException while iterating over the plans, the method must restart the
    // iteration over the finance plan list and must not call the stopThreads method of 
    // an already stopped plan.
	@Test
	public void testStopThreadsModifiedListException() throws FogbowException, ModifiedListException {
        setUpFinancePlugin();

        Mockito.when(plan1.isStarted()).thenReturn(true, false);
        Mockito.when(plan2.isStarted()).thenReturn(true);
        
        Mockito.when(plugins.getNext(Mockito.anyInt())).
        thenReturn(this.plan1).
        thenThrow(new ModifiedListException()).
        thenReturn(this.plan1, this.plan2, null);
        
        FinanceManager financeManager = new FinanceManager(objectHolder);

        financeManager.stopPlugins();

        Mockito.verify(plan1, Mockito.times(1)).stopThreads();
        Mockito.verify(plan2, Mockito.times(1)).stopThreads();
	}
	
	// test case: When calling the resetPlugins method, it must call
	// the reset method of the InMemoryFinanceObjectsHolder.
	@Test
	public void testResetPlugins() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
  
	    financeManager.resetPlugins();
	    
	    Mockito.verify(objectHolder).reset();
	}
	
	// test case: When calling the createFinancePlan method, it must call the 
	// PlanPluginFactory to create a new finance plan and call the registerPlanPlugin
	// method of the InMemoryFinanceObjectsHolder.
	@Test
	public void testCreateFinancePlan() throws FogbowException, ModifiedListException {
	    setUpFinancePlugin();

	    Map<String, String> planInfo = new HashMap<String, String>();

	    PowerMockito.mockStatic(PlanPluginInstantiator.class);
	    BDDMockito.given(PlanPluginInstantiator.getPlanPlugin(PLUGIN_CLASS_NAME, PLAN_NAME, planInfo, usersHolder)).willReturn(plan1);
	    
	    FinanceManager financeManager = new FinanceManager(objectHolder);
	    
	    
        financeManager.createFinancePlan(PLUGIN_CLASS_NAME, PLAN_NAME, planInfo);
        
       
        Mockito.verify(objectHolder).registerPlanPlugin(plan1);
	}
	
	private void setUpFinancePlugin() throws FogbowException, ModifiedListException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		systemUser2 = new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2);
		systemUser3 = new SystemUser(USER_ID_3, USER_NAME_3, PROVIDER_USER_3);
		
		operation1 = Mockito.mock(RasOperation.class);
		operation2 = Mockito.mock(RasOperation.class);
		operation3 = Mockito.mock(RasOperation.class);

        plugins = Mockito.mock(MultiConsumerSynchronizedList.class);
        this.plan1 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(this.plan1.isRegisteredUser(systemUser1)).thenReturn(true);
        Mockito.when(this.plan1.isRegisteredUser(systemUser2)).thenReturn(false);
        Mockito.when(this.plan1.isRegisteredUser(systemUser3)).thenReturn(true);
        Mockito.when(this.plan1.isAuthorized(systemUser1, operation1)).thenReturn(true);
        Mockito.when(this.plan1.isAuthorized(systemUser3, operation3)).thenReturn(false);
        Mockito.when(this.plan1.getName()).thenReturn(PLUGIN_1_NAME);
        
        this.plan2 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(this.plan2.isRegisteredUser(systemUser1)).thenReturn(false);
        Mockito.when(this.plan2.isRegisteredUser(systemUser2)).thenReturn(true);
        Mockito.when(this.plan2.isRegisteredUser(systemUser3)).thenReturn(false);
        Mockito.when(this.plan2.isAuthorized(systemUser2, operation2)).thenReturn(true);
        Mockito.when(this.plan2.getName()).thenReturn(PLUGIN_2_NAME);
        
        this.financeUser1 = Mockito.mock(FinanceUser.class);
        this.financeUser2 = Mockito.mock(FinanceUser.class);
        this.financeUser3 = Mockito.mock(FinanceUser.class);
        Mockito.when(this.financeUser1.getFinanceState(PROPERTY_NAME_1)).thenReturn(PROPERTY_VALUE_1);
        Mockito.when(this.financeUser2.getFinanceState(PROPERTY_NAME_2)).thenReturn(PROPERTY_VALUE_2);
        Mockito.when(this.financeUser1.getFinanceState(PROPERTY_NAME_3)).thenReturn(PROPERTY_VALUE_3);
        
        this.usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(this.usersHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(this.financeUser1);
        Mockito.when(this.usersHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(this.financeUser2);
        Mockito.when(this.usersHolder.getUserById(USER_ID_3, PROVIDER_USER_3)).thenReturn(this.financeUser3);
        
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Mockito.when(this.objectHolder.getInMemoryUsersHolder()).thenReturn(this.usersHolder);
        
        Mockito.when(this.objectHolder.getPlanPlugins()).thenReturn(plugins);
        Mockito.when(this.objectHolder.getPlanPlugin(PLUGIN_1_NAME)).thenReturn(this.plan1);
        Mockito.when(this.objectHolder.getPlanPlugin(PLUGIN_2_NAME)).thenReturn(this.plan2);
        Mockito.when(plugins.startIterating()).thenReturn(0);
        Mockito.when(plugins.getNext(Mockito.anyInt())).thenReturn(this.plan1, this.plan2, null);
        Mockito.when(plugins.isEmpty()).thenReturn(false);
	}
	
	private void setUpFinancePluginUnmanagedUser() 
	        throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		operation1 = Mockito.mock(RasOperation.class);
		
        MultiConsumerSynchronizedList<PersistablePlanPlugin> plugins = Mockito.mock(MultiConsumerSynchronizedList.class);
        this.plan1 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(this.plan1.isRegisteredUser(systemUser1)).thenReturn(false);
        
        this.plan2 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(this.plan2.isRegisteredUser(systemUser1)).thenReturn(false);
        
        this.usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(this.usersHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenThrow(new InvalidParameterException());
        
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Mockito.when(this.objectHolder.getInMemoryUsersHolder()).thenReturn(this.usersHolder);
        
        Mockito.when(this.objectHolder.getPlanPlugins()).thenReturn(plugins);
        Mockito.when(plugins.startIterating()).thenReturn(0);
        Mockito.when(plugins.getNext(Mockito.anyInt())).thenReturn(this.plan1, this.plan2, null);
        Mockito.when(plugins.isEmpty()).thenReturn(false);
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
