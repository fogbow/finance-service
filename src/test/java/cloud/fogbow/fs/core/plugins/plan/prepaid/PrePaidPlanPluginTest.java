package cloud.fogbow.fs.core.plugins.plan.prepaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
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
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.plan.postpaid.PostPaidPlanPlugin;
import cloud.fogbow.fs.core.plugins.plan.prepaid.PrePaidPlanPlugin.PrePaidPluginOptionsLoader;
import cloud.fogbow.fs.core.util.FinancePolicyFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

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
    private static final String RULES_STRING = "rulesString";
    private static final String NEW_RULES_STRING = "newRulesString";
    private static final Double DEFAULT_RESOURCE_VALUE = 12.0;
    private static final Double NEW_DEFAULT_RESOURCE_VALUE = 14.0;
    private static final String FINANCE_PLAN_FILE_PATH = "financeplanfilepath";
    private static final String TIME_TO_WAIT_BEFORE_STOPPING = "100000";
    private static final String DEFAULT_RESOURCE_VALUE_STRING = "1.0";
    private static final String CREDITS_DEDUCTION_WAIT_TIME = "100";
    private InMemoryUsersHolder objectHolder;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private CreditsManager paymentManager;
    private long creditsDeductionWaitTime = 1L;
    private long timeToWaitBeforeStopping = 100L;
    private long newTimeToWaitBeforeStopping = 101L;
    private FinancePolicyFactory planFactory;
    private JsonUtils jsonUtils;
    private FinancePolicy policy;
    private Map<String, String> rulesMap = new HashMap<String, String>();
    private Map<String, String> newRulesMap = new HashMap<String, String>();
    private long newCreditsDeductionWaitTime = 2L;
    private DebtsPaymentChecker debtsChecker;
    private PaymentRunner paymentRunner;
    private StopServiceRunner stopServiceRunner;
    private HashMap<String, String> financeOptions;
    private FinancePolicy newPlan;
    
    @Before
    public void setUp() throws InvalidParameterException {
        setUpFinanceOptions();
        setUpPlan();
    }
    
    // test case: When calling the isRegisteredUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plan.
    @Test
    public void testIsRegisteredUser() 
            throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
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

        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);

        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isRegisteredUser method passing as argument 
    // a user not subscribed to any plan, it must return false.
    @Test
    public void testIsRegisteredUserUserIsNotSubscribedToAnyPlan() 
            throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.isSubscribed()).thenReturn(false);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);;
        
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user financial state is not good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is not good, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user has not paid past invoices, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user has not paid past invoices, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the registerUser method, it must call the InMemoryUsersHolder
    // to register the user using given parameters and resume the user resources.
    @Test
    public void testRegisterUser() throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        prePaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
        Mockito.verify(this.stopServiceRunner).resumeResourcesForUser(user);
    }
    
    // test case: When calling the purgeUser method, it must call the InMemoryUsersHolder to remove
    // the user and purge the user resources through the StopServiceRunner.
    @Test
    public void testPurgeUser() throws FogbowException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        
        prePaidFinancePlugin.purgeUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.stopServiceRunner).purgeUserResources(user);        
        Mockito.verify(objectHolder).removeUser(USER_ID_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the changePlan method, it must call the 
    // InMemoryUsersHolder to change the user plan. 
    @Test
    public void testChangePlan() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        prePaidFinancePlugin.changePlan(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), NEW_PLAN_NAME);
        
        Mockito.verify(this.objectHolder).changePlan(USER_ID_1, PROVIDER_USER_1, NEW_PLAN_NAME);
    }

    // test case: When calling the unregisterUser method, it must call the 
    // InMemoryUsersHolder to unregister the user then purge the user resources through
    // the StopServiceRunner.
    @Test
    public void testUnregisterUser() throws InvalidParameterException, InternalServerErrorException {
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy, financeOptions);
        
        prePaidFinancePlugin.unregisterUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        Mockito.verify(this.stopServiceRunner).purgeUserResources(financeUser1);
        Mockito.verify(this.objectHolder).unregisterUser(USER_ID_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the setOptions method, if the finance plan used by
    // the PrePaid plan is not null, then the method must update the finance plan
    // using the rules passed as argument and also update the other plan parameters.
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNotNull() throws InvalidParameterException, InternalServerErrorException {
        // set up the PostPaid plan with a not null finance plan
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        // verify plan options before the setOptions operation
        Map<String, String> optionsBefore = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), optionsBefore.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(String.valueOf(timeToWaitBeforeStopping), optionsBefore.get(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING));
        assertEquals(RULES_STRING, optionsBefore.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(newTimeToWaitBeforeStopping));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        prePaidFinancePlugin.setOptions(financeOptions);
        
        // verify
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        
        // updated the plan rules
        Mockito.verify(this.policy).update(newRulesMap);
    }
    
    // test case: When calling the setOptions method, if the finance plan used by
    // the PrePaid plan is null, then the method must create a new finance plan instance 
    // using the plan factory, then update the other plan parameters.
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNull() throws InvalidParameterException, InternalServerErrorException {
        // set up the PostPaid plan with a null finance plan
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, null);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        financeOptions.put(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(newTimeToWaitBeforeStopping));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        prePaidFinancePlugin.setOptions(financeOptions);
        
        // verify
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(NEW_RULES_STRING, optionsAfter.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // created a new plan using the rules passed as argument
        Mockito.verify(this.planFactory).createFinancePolicy(PLAN_NAME, NEW_DEFAULT_RESOURCE_VALUE, newRulesMap);
    }
    
    // test case: When calling the setOptions method, if the finance options map passed
    // as argument contains a finance plan rules file path, then the method must create
    // a new finance plan instance using the plan factory, passing the finance plan file path,
    // then update the other plan parameters.
    @Test
    public void testSetOptionsWithPlanRuleFromFile() throws InvalidParameterException, InternalServerErrorException {
        // set up the PostPaid plan with a not null finance plan
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        financeOptions.put(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(newTimeToWaitBeforeStopping));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        prePaidFinancePlugin.setOptions(financeOptions);
        
        // verify
        Map<String, String> optionsAfter = prePaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newCreditsDeductionWaitTime), optionsAfter.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(NEW_RULES_STRING, optionsAfter.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // created a new plan using the finance plan file path passed as argument
        Mockito.verify(this.planFactory).createFinancePolicy(PLAN_NAME, NEW_DEFAULT_RESOURCE_VALUE, FINANCE_PLAN_FILE_PATH);
    }
    
    // test case: When calling the setOptions method, if the finance options map passed
    // as argument contains no finance plan startup option, then the method must
    // throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsThrowsExceptionIfNoPlanStartUpOptionIsPassed() throws InvalidParameterException, InternalServerErrorException {
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        Map<String, String> optionsBefore = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), optionsBefore.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(RULES_STRING, optionsBefore.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(newCreditsDeductionWaitTime));
        
        prePaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // does not contain some required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsMissingOption() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        prePaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // contains no finance plan creation method, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsNoFinancePlanCreationMethod() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, CREDITS_DEDUCTION_WAIT_TIME);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, DEFAULT_RESOURCE_VALUE_STRING);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        prePaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsUnparsableOptionAsLong() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, "invalidoption");
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, DEFAULT_RESOURCE_VALUE_STRING);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        prePaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsUnparsableOptionAsDouble() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, CREDITS_DEDUCTION_WAIT_TIME);
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, "invalidoption");
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);
        
        prePaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the getOptions method, it must return a Map containing 
    // Strings that represent all the finance options used by the PrePaid plan.
    @Test
    public void testGetOptions() throws InvalidParameterException, InternalServerErrorException {
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, creditsDeductionWaitTime, timeToWaitBeforeStopping, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, this.policy);

        Map<String, String> options = prePaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), options.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(RULES_STRING, options.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES));
    }

    // test case: When calling the PrePaidPluginOptionsLoader.load method, it must
    // get all the necessary PrePaid parameters from a PropertiesHolder instance and
    // return these parameters as a Map.
    @Test
    public void testPrePaidPluginOptionsLoaderValidOptions() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), FINANCE_PLAN_RULES_FILE_PATH, String.valueOf(timeToWaitBeforeStopping), 
                String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        Map<String, String> options = loader.load();
        
        assertEquals(String.valueOf(creditsDeductionWaitTime), options.get(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME));
        assertEquals(FINANCE_PLAN_RULES_FILE_PATH, options.get(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH));
    }
    
    // test case: When calling the PrePaidPluginOptionsLoader.load method and the property
    // CreditsDeductionWaitTime is missing, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingCreditsDeductionWaitTime() throws ConfigurationErrorException {
        setUpOptions(null, FINANCE_PLAN_RULES_FILE_PATH, String.valueOf(timeToWaitBeforeStopping), 
                String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PrePaidPluginOptionsLoader.load method and the property
    // FinancePlanRulesFilePath is missing, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingFinancePlanRulesFilePath() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), null, String.valueOf(timeToWaitBeforeStopping), 
                String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PrePaidPluginOptionsLoader.load method and the property
    // TimeToWaitBeforeStopping is missing, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingTimeToWaitBeforeStopping() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), FINANCE_PLAN_RULES_FILE_PATH, null,
                String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PrePaidPluginOptionsLoader.load method and the property
    // DefaultResourceValue is missing, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testPrePaidPluginOptionsLoaderMissingDefaultResourceValue() throws ConfigurationErrorException {
        setUpOptions(String.valueOf(creditsDeductionWaitTime), FINANCE_PLAN_RULES_FILE_PATH, 
                String.valueOf(timeToWaitBeforeStopping), null);
        
        PrePaidPluginOptionsLoader loader = new PrePaidPluginOptionsLoader();
        
        loader.load();
    }
    
    private void setUpFinanceOptions() {
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        financeOptions.put(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(timeToWaitBeforeStopping));
        financeOptions.put(PrePaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(DEFAULT_RESOURCE_VALUE));
    }
    
    private void setUpOptions(String creditsDeductionWaitTime, String financePlanRulesPath, String timeToWaitBeforeStopping, 
            String defaultResourceValue) {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME)).thenReturn(creditsDeductionWaitTime);
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH)).thenReturn(financePlanRulesPath);
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING)).thenReturn(timeToWaitBeforeStopping);
        Mockito.when(propertiesHolder.getProperty(PrePaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE)).thenReturn(defaultResourceValue);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }
    
    private void setUpPlan() throws InvalidParameterException {
        this.policy = Mockito.mock(FinancePolicy.class);
        Mockito.when(this.policy.toString()).thenReturn(RULES_STRING);
        
        this.newPlan = Mockito.mock(FinancePolicy.class);
        Mockito.when(this.newPlan.toString()).thenReturn(NEW_RULES_STRING);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);
        Mockito.when(this.jsonUtils.toJson(newRulesMap)).thenReturn(NEW_RULES_JSON);
        Mockito.when(this.jsonUtils.fromJson(NEW_RULES_JSON, Map.class)).thenReturn(newRulesMap);
        
        this.planFactory = Mockito.mock(FinancePolicyFactory.class);
        Mockito.when(this.planFactory.createFinancePolicy(PLAN_NAME, NEW_DEFAULT_RESOURCE_VALUE, newRulesMap)).thenReturn(newPlan);
        Mockito.when(this.planFactory.createFinancePolicy(PLAN_NAME, NEW_DEFAULT_RESOURCE_VALUE, FINANCE_PLAN_FILE_PATH)).thenReturn(newPlan);
    }
}
