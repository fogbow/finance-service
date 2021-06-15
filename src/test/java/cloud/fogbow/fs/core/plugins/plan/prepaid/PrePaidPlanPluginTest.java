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
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.plan.prepaid.PrePaidPlanPlugin.PrePaidPluginOptionsLoader;
import cloud.fogbow.fs.core.util.FinancePlanFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

// TODO refactor
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
    private static final String NEW_PLAN_NAME = "newPlanName";
    private static final String FINANCE_PLAN_RULES_FILE_PATH = "rulesfilepath";
    private static final String RULES_JSON = "rulesjson";
    private static final String NEW_RULES_JSON = "newrulesjson";
    private static final String FINANCE_PLAN_FILE_PATH = "financeplanfilepath";
    private InMemoryUsersHolder objectHolder;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private CreditsManager paymentManager;
    private long creditsDeductionWaitTime = 1L;
    private FinancePlanFactory planFactory;
    private JsonUtils jsonUtils;
    private FinancePlan plan;
    private Map<String, String> rulesMap = new HashMap<String, String>();
    private Map<String, String> newRulesMap = new HashMap<String, String>();
    private long newCreditsDeductionWaitTime = 2L;
    private DebtsPaymentChecker debtsChecker;
    private PaymentRunner paymentRunner;
    private StopServiceRunner stopServiceRunner;
    
    // test case: When calling the isRegisteredUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plugin.
    @Test
    public void testIsRegisteredUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.isSubscribed()).thenReturn(true);
        Mockito.when(financeUser1.getFinancePluginName()).thenReturn(PLAN_NAME);
        
        FinanceUser financeUser2 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser2.isSubscribed()).thenReturn(true);
        Mockito.when(financeUser2.getFinancePluginName()).thenReturn(PLAN_NAME);
        
        FinanceUser financeUser3 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser3.isSubscribed()).thenReturn(true);
        Mockito.when(financeUser3.getFinancePluginName()).thenReturn("otherplugin");

        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);

        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);

        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isRegisteredUser method passing as argument 
    // a user not subscribed to any plan, it must return false.
    @Test
    public void testIsRegisteredUserUserIsNotSubscribedToAnyPlan() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.isSubscribed()).thenReturn(false);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
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
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
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
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
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
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user has not paid past invoices, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin postPaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user has not paid past invoices, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin postPaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the registerUser method, it must call the DatabaseManager
    // to create the user, create a UserCredits instance for the new user and 
    // save the user credits using the DatabaseManager.
    @Test
    public void testRegisterUser() throws InternalServerErrorException, InvalidParameterException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        prePaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
    }
    
    // TODO documentation
    @Test
    public void testPurgeUser() throws FogbowException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, plan, financeOptions);
        
        
        prePaidFinancePlugin.purgeUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.stopServiceRunner).purgeUserResources(user);        
        Mockito.verify(objectHolder).removeUser(USER_ID_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the changePlan method, it must call the 
    // InMemoryUsersHolder to change the user plan. 
    @Test
    public void testChangePlan() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.invoicesArePaid()).thenReturn(true);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        prePaidFinancePlugin.changePlan(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), NEW_PLAN_NAME);
        
        Mockito.verify(this.objectHolder).changePlan(USER_ID_1, PROVIDER_USER_1, NEW_PLAN_NAME);
    }

    // TODO documentation
    @Test
    public void testUnregisterUser() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, objectHolder, accountingServiceClient,
                rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, plan, financeOptions);
        
        prePaidFinancePlugin.unregisterUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        Mockito.verify(this.stopServiceRunner).purgeUserResources(financeUser1);
        Mockito.verify(this.objectHolder).unregisterUser(USER_ID_1, PROVIDER_USER_1);
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
                this.jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, this.plan);
        
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
                this.jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, this.plan);
        
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
                this.jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, null);
        
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
                this.jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, this.plan);
        
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
                this.jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, this.plan);

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
