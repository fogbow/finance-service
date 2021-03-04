package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, FinancePluginInstantiator.class})
public class FinanceManagerTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_ID_3 = "userId3";
	private static final String PROPERTY_NAME_1 = "propertyName1";
	private static final String PROPERTY_VALUE_1 = "propertyValue1";
	private static final String PROPERTY_NAME_2 = "propertyName2";
	private static final String PROPERTY_VALUE_2 = "propertyValue2";
	private static final String PROPERTY_NAME_3 = "propertyName3";
	private static final String PROPERTY_VALUE_3 = "propertyValue3";
	private List<FinancePlugin> financePlugins;
	private DatabaseManager databaseManager;
	private Map<String, String> operationParameters;
	private FinancePlugin plugin1;
	private FinancePlugin plugin2;
	private AuthorizableUser user1;
	private AuthorizableUser user2;
	private AuthorizableUser user3;

	// TODO documentation
	@Test
	public void testConstructor() throws ConfigurationErrorException {
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
	
	// TODO documentation
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
	
	// TODO documentation
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorChecksPluginsListIsNotEmpty() throws ConfigurationErrorException {
		financePlugins = new ArrayList<FinancePlugin>();
		
		databaseManager = Mockito.mock(DatabaseManager.class);
		
		new FinanceManager(financePlugins, databaseManager);
	}
	
	// TODO documentation
	@Test
	public void testIsAuthorized() throws InvalidParameterException, ConfigurationErrorException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		assertTrue(financeManager.isAuthorized(user1));
		assertTrue(financeManager.isAuthorized(user2));
		assertFalse(financeManager.isAuthorized(user3));
	}

	// TODO documentation
	@Test(expected = InvalidParameterException.class)
	public void testIsAuthorizedUserIsNotManaged() throws InvalidParameterException, ConfigurationErrorException {
		setUpFinancePluginUnmanagedUser();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		financeManager.isAuthorized(user1);
	}
	
	// TODO documentation
	@Test
	public void testGetFinanceStateProperty() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		assertEquals(PROPERTY_VALUE_1, financeManager.getFinanceStateProperty(USER_ID_1, PROPERTY_NAME_1));
		assertEquals(PROPERTY_VALUE_2, financeManager.getFinanceStateProperty(USER_ID_2, PROPERTY_NAME_2));
		assertEquals(PROPERTY_VALUE_3, financeManager.getFinanceStateProperty(USER_ID_3, PROPERTY_NAME_3));
	}
	
	// TODO documentation
	@Test(expected = InvalidParameterException.class)
	public void testGetFinanceStatePropertyUserIsNotManaged() throws FogbowException {
		setUpFinancePluginUnmanagedUser();

		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		financeManager.getFinanceStateProperty(USER_ID_1, PROPERTY_NAME_1);
	}
	
	// TODO documentation
	@Test
	public void testStartThreads() throws ConfigurationErrorException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);
		
		financeManager.startPlugins();
		
		Mockito.verify(plugin1, Mockito.times(1)).startThreads();
		Mockito.verify(plugin2, Mockito.times(1)).startThreads();
	}

	// TODO documentation
	@Test
	public void testStopThreads() throws ConfigurationErrorException {
		setUpFinancePlugin();

		FinanceManager financeManager = new FinanceManager(financePlugins, databaseManager);

		financeManager.stopPlugins();

		Mockito.verify(plugin1, Mockito.times(1)).stopThreads();
		Mockito.verify(plugin2, Mockito.times(1)).stopThreads();
	}
	
	private void setUpFinancePlugin() {
		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1)).thenReturn(true);
		Mockito.when(plugin1.managesUser(USER_ID_2)).thenReturn(false);
		Mockito.when(plugin1.managesUser(USER_ID_3)).thenReturn(true);
		Mockito.when(plugin1.isAuthorized(USER_ID_1, operationParameters)).thenReturn(true);
		Mockito.when(plugin1.isAuthorized(USER_ID_3, operationParameters)).thenReturn(false);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_1, PROPERTY_NAME_1)).thenReturn(PROPERTY_VALUE_1);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_3, PROPERTY_NAME_3)).thenReturn(PROPERTY_VALUE_3);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_2)).thenReturn(true);
		Mockito.when(plugin2.isAuthorized(USER_ID_2, operationParameters)).thenReturn(true);
		Mockito.when(plugin2.getUserFinanceState(USER_ID_2, PROPERTY_NAME_2)).thenReturn(PROPERTY_VALUE_2);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
		this.user1 = new AuthorizableUser(USER_ID_1, operationParameters);
		this.user2 = new AuthorizableUser(USER_ID_2, operationParameters);
		this.user3 = new AuthorizableUser(USER_ID_3, operationParameters);
	}
	
	private void setUpFinancePluginUnmanagedUser() {
		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1)).thenReturn(false);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_1)).thenReturn(false);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
		this.user1 = new AuthorizableUser(USER_ID_1, operationParameters);
	}
}
