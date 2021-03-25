package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

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
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.ras.core.models.RasOperation;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, FinancePluginInstantiator.class, 
	FsPublicKeysHolder.class, AuthenticationUtil.class})
public class FinanceManagerTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_ID_3 = "userId3";
	private static final String USER_NAME_1 = "userName1";
	private static final String USER_NAME_2 = "userName2";
	private static final String USER_NAME_3 = "userName3";
	private static final String PROVIDER_USER_1 = "providerUserId1";
	private static final String PROVIDER_USER_2 = "providerUserId2";
	private static final String PROVIDER_USER_3 = "providerUserId3";
	private static final String PROPERTY_NAME_1 = "propertyName1";
	private static final String PROPERTY_VALUE_1 = "propertyValue1";
	private static final String PROPERTY_NAME_2 = "propertyName2";
	private static final String PROPERTY_VALUE_2 = "propertyValue2";
	private static final String PROPERTY_NAME_3 = "propertyName3";
	private static final String PROPERTY_VALUE_3 = "propertyValue3";
	private static final String USER_1_TOKEN = "user1Token";
	private static final String USER_2_TOKEN = "user2Token";
	private static final String USER_3_TOKEN = "user3Token";
	private List<FinancePlugin> financePlugins;
	private DatabaseManager databaseManager;
	private FinancePlugin plugin1;
	private FinancePlugin plugin2;
	private AuthorizableUser user1;
	private AuthorizableUser user2;
	private AuthorizableUser user3;
	private SystemUser systemUser1;
	private SystemUser systemUser2;
	private SystemUser systemUser3;
	private RasOperation operation1;
	private RasOperation operation2;
	private RasOperation operation3;

	// test case: When calling the constructor, it must get
	// the names of the finance plugins from a PropertiesHolder
	// instance and call the FinancePluginInstantiator to 
	// instantiate the finance plugins.
	@Test
	public void testConstructor() throws FogbowException {
		setUpFinancePlugin();

		String pluginName1 = "plugin1";
		String pluginName2 = "plugin2";
		
		String pluginString = String.join(FinanceManager.FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR, 
				pluginName1, pluginName2);
		
		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

		PowerMockito.mockStatic(FinancePluginInstantiator.class);
		BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName1, databaseManager)).willReturn(plugin1);
		BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName2, databaseManager)).willReturn(plugin2);

		
		new FinanceManager(databaseManager);
		
		
		Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		
		PowerMockito.verifyStatic(FinancePluginInstantiator.class);
		FinancePluginInstantiator.getFinancePlugin(pluginName1, databaseManager);
		PowerMockito.verifyStatic(FinancePluginInstantiator.class);
		FinancePluginInstantiator.getFinancePlugin(pluginName2, databaseManager);
	}
	
	// test case: When calling the constructor and the list
	// of finance plugins is empty, it must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorThrowsExceptionIfNoFinancePluginIsGiven() throws ConfigurationErrorException {
		String pluginString = "";

		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
		
		new FinanceManager(databaseManager);
	}
	
	// test case: When calling the constructor passing an empty list
	// of finance plugins, it must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorChecksPluginsListIsNotEmpty() throws ConfigurationErrorException {
		financePlugins = new ArrayList<FinancePlugin>();
		
		databaseManager = Mockito.mock(DatabaseManager.class);
		
		new FinanceManager(financePlugins, databaseManager);
	}
	
	// test case: When calling the isAuthorized method passing an AuthorizableUser,
	// it must check which finance plugin manages the user and call the isAuthorized 
	// method of the plugin.
	@Test
	public void testIsAuthorized() throws FogbowException {
		setUpFinancePlugin();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		assertTrue(financeManager.isAuthorized(user1));
		assertTrue(financeManager.isAuthorized(user2));
		assertFalse(financeManager.isAuthorized(user3));
	}

	// test case: When calling the isAuthorized method passing an AuthorizableUser which
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testIsAuthorizedUserIsNotManaged() throws FogbowException {
		setUpFinancePluginUnmanagedUser();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		financeManager.isAuthorized(user1);
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser, 
	// it must check which finance plugin manages the user and call the getUserFinanceState
	// method of the plugin.
	@Test
	public void testGetFinanceStateProperty() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		assertEquals(PROPERTY_VALUE_1, financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1));
		assertEquals(PROPERTY_VALUE_2, financeManager.getFinanceStateProperty(USER_ID_2, PROVIDER_USER_2, PROPERTY_NAME_2));
		assertEquals(PROPERTY_VALUE_3, financeManager.getFinanceStateProperty(USER_ID_3, PROVIDER_USER_3, PROPERTY_NAME_3));
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser
	// which is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetFinanceStatePropertyUserIsNotManaged() throws FogbowException {
		setUpFinancePluginUnmanagedUser();

		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1);
	}
	
	// test case: When calling the startPlugins method, it must call the startThreads
	// method of all the known finance plugins.
	@Test
	public void testStartThreads() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		financeManager.startPlugins();
		
		Mockito.verify(plugin1, Mockito.times(1)).startThreads();
		Mockito.verify(plugin2, Mockito.times(1)).startThreads();
	}

	// test case: When calling the stopPlugins method, it must call the stopThreads
	// method of all the known finance plugins.
	@Test
	public void testStopThreads() throws FogbowException {
		setUpFinancePlugin();

		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);

		financeManager.stopPlugins();

		Mockito.verify(plugin1, Mockito.times(1)).stopThreads();
		Mockito.verify(plugin2, Mockito.times(1)).stopThreads();
	}
	
	private void setUpFinancePlugin() throws FogbowException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		systemUser2 = new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2);
		systemUser3 = new SystemUser(USER_ID_3, USER_NAME_3, PROVIDER_USER_3);
		
		operation1 = Mockito.mock(RasOperation.class);
		operation2 = Mockito.mock(RasOperation.class);
		operation3 = Mockito.mock(RasOperation.class);

		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		Mockito.when(plugin1.managesUser(USER_ID_2, PROVIDER_USER_2)).thenReturn(false);
		Mockito.when(plugin1.managesUser(USER_ID_3, PROVIDER_USER_3)).thenReturn(true);
		Mockito.when(plugin1.isAuthorized(systemUser1, operation1)).thenReturn(true);
		Mockito.when(plugin1.isAuthorized(systemUser3, operation3)).thenReturn(false);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1)).thenReturn(PROPERTY_VALUE_1);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_3, PROVIDER_USER_3, PROPERTY_NAME_3)).thenReturn(PROPERTY_VALUE_3);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_2, PROVIDER_USER_2)).thenReturn(true);
		Mockito.when(plugin2.isAuthorized(systemUser2, operation2)).thenReturn(true);
		Mockito.when(plugin2.getUserFinanceState(USER_ID_2, PROVIDER_USER_2, PROPERTY_NAME_2)).thenReturn(PROPERTY_VALUE_2);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
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
	
	private void setUpFinancePluginUnmanagedUser() {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		operation1 = Mockito.mock(RasOperation.class);
		
		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
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
