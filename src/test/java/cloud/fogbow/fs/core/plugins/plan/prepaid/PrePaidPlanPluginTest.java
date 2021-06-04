package cloud.fogbow.fs.core.plugins.plan.prepaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.plan.prepaid.PrePaidPlanPlugin.PrePaidPluginOptionsLoader;
import cloud.fogbow.fs.core.util.FinancePlanFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

// TODO review documentation
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class PrePaidPlanPluginTest {
    private static final String USER_ID_1 = "userId1";
    private static final String USER_ID_2 = "userId2";
    private static final String USER_NAME_1 = "userName1";
    private static final String USER_NAME_2 = "userName2";
    private static final String USER_NOT_MANAGED = "userNotManaged";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String PLAN_NAME = "pluginName";
    private static final String FINANCE_PLAN_RULES_FILE_PATH = "rulesfilepath";
    private static final String RULES_JSON = "rulesjson";
    private static final String NEW_RULES_JSON = "newrulesjson";
    private static final String FINANCE_PLAN_FILE_PATH = "financeplanfilepath";
    private InMemoryUsersHolder objectHolder;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private CreditsManager paymentManager;
    private long creditsDeductionWaitTime = 1L;
    private UserCredits userCredits;
    private FinancePlanFactory planFactory;
    private JsonUtils jsonUtils;
    private FinancePlan plan;
    private Map<String, String> rulesMap = new HashMap<String, String>();
    private Map<String, String> newRulesMap = new HashMap<String, String>();
    private long newCreditsDeductionWaitTime = 2L;
    
    // test case: When calling the managesUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plugin.
    @Test
    public void testManagesUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser financeUser1 = new FinanceUser();
        financeUser1.setUserId(USER_ID_1, PROVIDER_USER_1);
        financeUser1.subscribeToPlan(PLAN_NAME);

        FinanceUser financeUser2 = new FinanceUser();
        financeUser2.setUserId(USER_ID_2, PROVIDER_USER_2);
        financeUser2.subscribeToPlan(PLAN_NAME);

        FinanceUser financeUser3 = new FinanceUser();
        financeUser3.setUserId(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED);
        financeUser3.subscribeToPlan("otherplugin");

        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);

        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);

        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user financial state is not good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is not good, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the addUser method, it must call the DatabaseManager
    // to create the user, create a UserCredits instance for the new user and 
    // save the user credits using the DatabaseManager.
    @Test
    public void testAddUser() throws InternalServerErrorException, InvalidParameterException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        prePaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
    }
    
    // test case: When calling the updateFinanceState method, it must get 
    // the UserCredits for the given user, then update and save the credits state.
    @Test
    public void testUpdateFinanceState() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        FinanceUser user = Mockito.mock(FinanceUser.class);
        Mockito.when(user.getCredits()).thenReturn(userCredits);
        
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidPlanPlugin.CREDITS_TO_ADD, "10.5");
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);

        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);

        
        Mockito.verify(userCredits).addCredits(10.5);
    }
    
    // test case: When calling the updateFinanceState method and 
    // a required state property is missing, it must throw an 
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateMissingFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);

        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);
    }
    
    // test case: When calling the updateFinanceState method and
    // a required state property has an invalid value, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateInvalidFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidPlanPlugin.CREDITS_TO_ADD, "invalidproperty");
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
    
        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);
    }

    // TODO documentation
    @Test
    public void testSetOptions() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        Map<String, String> optionsBefore = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), optionsBefore.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(RULES_JSON, optionsBefore.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        
        prePaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        // rules are not updated
        assertEquals(RULES_JSON, optionsAfter.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNotNull() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);
        Mockito.when(this.jsonUtils.fromJson(NEW_RULES_JSON, Map.class)).thenReturn(newRulesMap);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        Map<String, String> optionsBefore = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), optionsBefore.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(RULES_JSON, optionsBefore.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        
        prePaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        Mockito.verify(this.plan).update(newRulesMap);
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNull() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(newRulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(newRulesMap)).thenReturn(NEW_RULES_JSON);
        Mockito.when(this.jsonUtils.fromJson(NEW_RULES_JSON, Map.class)).thenReturn(newRulesMap);
        
        this.planFactory = Mockito.mock(FinancePlanFactory.class);
        Mockito.when(this.planFactory.createFinancePlan(PLAN_NAME, newRulesMap)).thenReturn(plan);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, null);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        
        prePaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(NEW_RULES_JSON, optionsAfter.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        Mockito.verify(this.planFactory).createFinancePlan(PLAN_NAME, newRulesMap);
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRuleFromFile() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(newRulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(newRulesMap)).thenReturn(NEW_RULES_JSON);
        
        this.planFactory = Mockito.mock(FinancePlanFactory.class);
        Mockito.when(this.planFactory.createFinancePlan(PLAN_NAME, FINANCE_PLAN_FILE_PATH)).thenReturn(plan);
        
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        
        prePaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(NEW_RULES_JSON, optionsAfter.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        Mockito.verify(this.planFactory).createFinancePlan(PLAN_NAME, FINANCE_PLAN_FILE_PATH);
    }
    
    // TODO documentation
    @Test
    public void testGetOptions() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);

        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);

        Map<String, String> options = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), options.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(RULES_JSON, options.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
    }

    // TODO documentation
    @Test
    public void testPrePaidPluginOptionsLoaderValidOptions() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), FINANCE_PLAN_RULES_FILE_PATH);
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        Map<String, String> options = loader.load();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), options.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(FINANCE_PLAN_RULES_FILE_PATH, options.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH));
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingCreditsDeductionWaitTime() throws ConfigurationErrorException {
        setUpOptions(null, FINANCE_PLAN_RULES_FILE_PATH);
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingFinancePlanRulesFilePath() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), null);
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    private void setUpOptions(String creditsDeductionWaitTime, String financePlanRulesPath) {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME)).thenReturn(creditsDeductionWaitTime);
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH)).thenReturn(financePlanRulesPath);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }
}
