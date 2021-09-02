package cloud.fogbow.fs.core.plugins.plan.postpaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
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
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.plan.postpaid.PostPaidPlanPlugin.PostPaidPluginOptionsLoader;
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
public class PostPaidPlanPluginTest {
    private static final String INVOICE_WAIT_TIME = "30000";
    private static final String FINANCE_PLAN_RULES_FILE_PATH = "rules_path";
    private static final String USER_BILLING_INTERVAL = "60000";
    private static final String TIME_TO_WAIT_BEFORE_STOPPING = "100000";
    private static final String FINANCE_PLAN_DEFAULT_RESOURCE_VALUE = "1.0";
    private static final String USER_ID_1 = "userId1";
    private static final String USER_ID_2 = "userId2";
    private static final String USER_NAME_1 = "userName1";
    private static final String USER_NAME_2 = "userName2";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final String USER_NOT_MANAGED = "userNotManaged";
    private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String PLAN_NAME = "planName";
    private static final String RULES_JSON = "rulesjson";
    private static final String RULES_STRING = "rulesString";
    private static final String NEW_RULES_JSON = "newRulesJson";
    private static final String NEW_RULES_STRING = "newRulesString";
    private static final String FINANCE_PLAN_FILE_PATH = "financeplanfilepath";
    private static final String NEW_PLAN_NAME = "newPlanName";
    private static final Double DEFAULT_RESOURCE_VALUE = 10.0;
    private static final Double NEW_DEFAULT_RESOURCE_VALUE = 12.0;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private InvoiceManager paymentManager;
    private FinancePolicyFactory planFactory;
    private long userBillingInterval = 10L;
    private long newUserBillingInterval = 20L;
    private long invoiceWaitTime = 1L;
    private long newInvoiceWaitTime = 2L;
    private long timeToWaitBeforeStopping = 100L;
    private long newTimeToWaitBeforeStopping = 101L;
    private InMemoryUsersHolder objectHolder;
    private JsonUtils jsonUtils;
    private FinancePolicy plan;
    private Map<String, String> rulesMap = new HashMap<String, String>();
    private Map<String, String> newRulesMap = new HashMap<String, String>();
    private HashMap<String, String> financeOptions;
    private DebtsPaymentChecker debtsChecker;
    private StopServiceRunner stopServiceRunner;
    private PaymentRunner paymentRunner;
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
    public void testIsRegisteredUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
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
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        assertTrue(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isRegisteredUser method passing as argument 
    // a user not subscribed to any plan, it must return false.
    @Test
    public void testIsRegisteredUserUserIsNotSubscribedToAnyPlan() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.isSubscribed()).thenReturn(false);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        assertFalse(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user financial state is not good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is not good, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user has not paid past invoices, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user has not paid past invoices, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserHasNotPaidPastInvoices() throws InvalidParameterException, InternalServerErrorException {
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        this.debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        Mockito.when(this.debtsChecker.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the registerUser method, it must call the InMemoryUsersHolder
    // to register the user using given parameters and resume the user resources.
    @Test
    public void testRegisterUser() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        
        postPaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
        Mockito.verify(this.stopServiceRunner).resumeResourcesForUser(user);
    }
    
    // test case: When calling the purgeUser method, it must call the InMemoryUsersHolder to remove
    // the user and purge the user resources through the StopServiceRunner.
    @Test
    public void testPurgeUser() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        
        postPaidFinancePlugin.purgeUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.objectHolder).removeUser(USER_ID_1, PROVIDER_USER_1);
        Mockito.verify(this.stopServiceRunner).purgeUserResources(user);
    }
    
    // test case: When calling the changePlan method, it must check if the user has paid 
    // all invoices, call the PaymentRunner to generate the last payment for the user and 
    // then call the InMemoryUsersHolder to change the user plan. 
    @Test
    public void testChangePlan() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.invoicesArePaid()).thenReturn(true);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        postPaidFinancePlugin.changePlan(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), NEW_PLAN_NAME);
        
        Mockito.verify(this.paymentRunner).runLastPaymentForUser(USER_ID_1, PROVIDER_USER_1); ;
        Mockito.verify(this.objectHolder).changePlan(USER_ID_1, PROVIDER_USER_1, NEW_PLAN_NAME);
    }
    
    // test case: When calling the changePlan method and the user passed as argument
    // has not paid all invoices, it must throw an InvalidParameterException.
    @Test
    public void testChangePlanUserHasNotPaidAllInvoices() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.invoicesArePaid()).thenReturn(false);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        try {
            postPaidFinancePlugin.changePlan(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), NEW_PLAN_NAME);
            Assert.fail("changePlan is expected to throw an InvalidParameterException.");
        } catch (InvalidParameterException e) {
            
        }
        
        Mockito.verify(this.paymentRunner, Mockito.never()).runLastPaymentForUser(USER_ID_1, PROVIDER_USER_1); ;
        Mockito.verify(this.objectHolder, Mockito.never()).changePlan(USER_ID_1, PROVIDER_USER_1, NEW_PLAN_NAME);
    }
    
    // test case: When calling the unregisterUser method, it must check if the user has paid 
    // all invoices, call the PaymentRunner to generate the last payment for the user, 
    // call the InMemoryUsersHolder to unregister the user then purge the user resources through
    // the StopServiceRunner.
    @Test
    public void testUnregisterUser() throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        Mockito.when(user.invoicesArePaid()).thenReturn(true);
        
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        
        postPaidFinancePlugin.unregisterUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.objectHolder).unregisterUser(USER_ID_1, PROVIDER_USER_1);
        Mockito.verify(this.paymentRunner).runLastPaymentForUser(USER_ID_1, PROVIDER_USER_1);
        Mockito.verify(this.stopServiceRunner).purgeUserResources(user);
    }
    
    // test case: When calling the unregisterUser method and the user passed as argument
    // has not paid all invoices, it must throw an InvalidParameterException.
    @Test
    public void testUnregisterUserUserHasNotPaidAllInvoices() throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = Mockito.mock(FinanceUser.class);
        Mockito.when(user.invoicesArePaid()).thenReturn(false);
        
        this.stopServiceRunner = Mockito.mock(StopServiceRunner.class);
        this.paymentRunner = Mockito.mock(PaymentRunner.class);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        try {
            postPaidFinancePlugin.unregisterUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
            Assert.fail("unregisterUser is expected to throw an InvalidParameterException.");
        } catch (InvalidParameterException e) {
            
        }
        
        Mockito.verify(this.objectHolder, Mockito.never()).unregisterUser(USER_ID_1, PROVIDER_USER_1);
        Mockito.verify(this.paymentRunner, Mockito.never()).runLastPaymentForUser(USER_ID_1, PROVIDER_USER_1);
        Mockito.verify(this.stopServiceRunner, Mockito.never()).purgeUserResources(user);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // does not contain some required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsMissingOption() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }

    // test case: When calling the setOptions method and the finance options map 
    // contains no finance plan creation method, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsNoFinancePlanCreationMethod() throws InvalidParameterException, InternalServerErrorException {
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, FINANCE_PLAN_DEFAULT_RESOURCE_VALUE);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsUnparsableOptionAsLong() throws InvalidParameterException, InternalServerErrorException {
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidoption");
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, FINANCE_PLAN_DEFAULT_RESOURCE_VALUE);
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsUnparsableOptionAsDouble() throws InvalidParameterException, InternalServerErrorException {
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        financeOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, TIME_TO_WAIT_BEFORE_STOPPING);
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, "invalidoption");
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the setOptions method, if the finance plan used by
    // the PostPaid plan is not null, then the method must update the finance plan
    // using the rules passed as argument and also update the other plan parameters.
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNotNull() throws InvalidParameterException, InternalServerErrorException {
        // set up the PostPaid plan with a not null finance plan
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                this.plan);
        
        // verify plan options before the setOptions operation
        Map<String, String> optionsBefore = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), optionsBefore.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), optionsBefore.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_STRING, optionsBefore.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> newFinanceOptions = new HashMap<String, String>();
        newFinanceOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        newFinanceOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        newFinanceOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, 
                String.valueOf(newTimeToWaitBeforeStopping));
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        postPaidFinancePlugin.setOptions(newFinanceOptions);
        
        
        // verify
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        
        // updated the plan rules
        Mockito.verify(this.plan).update(newRulesMap);
    }
    
    // test case: When calling the setOptions method, if the finance plan used by
    // the PostPaid plan is null, then the method must create a new finance plan instance 
    // using the plan factory, then update the other plan parameters.
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNull() throws InvalidParameterException, InternalServerErrorException {
        // set up the PostPaid plan with a null finance plan
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                null);
        
        // new options
        Map<String, String> newFinanceOptions = new HashMap<String, String>();
        newFinanceOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        newFinanceOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        newFinanceOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, 
                String.valueOf(newTimeToWaitBeforeStopping));
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        postPaidFinancePlugin.setOptions(newFinanceOptions);
        
        // verify
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(NEW_RULES_STRING, optionsAfter.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
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
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        // new options
        Map<String, String> newFinanceOptions = new HashMap<String, String>();
        newFinanceOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        newFinanceOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        newFinanceOptions.put(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING, 
                String.valueOf(newTimeToWaitBeforeStopping));
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        newFinanceOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(NEW_DEFAULT_RESOURCE_VALUE));
        
        // exercise
        postPaidFinancePlugin.setOptions(newFinanceOptions);
        
        // verify
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        // updated the plan parameters
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(NEW_RULES_STRING, optionsAfter.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // created a new plan using the finance plan file path passed as argument
        Mockito.verify(this.planFactory).createFinancePolicy(PLAN_NAME, NEW_DEFAULT_RESOURCE_VALUE, FINANCE_PLAN_FILE_PATH);
    }
    
    // test case: When calling the setOptions method, if the finance options map passed
    // as argument contains no finance plan startup option, then the method must
    // throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSetOptionsThrowsExceptionIfNoPlanStartUpOptionIsPassed() throws InvalidParameterException, InternalServerErrorException {
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                plan, financeOptions);
        
        Map<String, String> optionsBefore = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), optionsBefore.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), optionsBefore.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_STRING, optionsBefore.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the getOptions method, it must return a Map containing 
    // Strings that represent all the finance options used by the PostPaid plan.
    @Test
    public void testGetOptions() throws InvalidParameterException, InternalServerErrorException {
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, timeToWaitBeforeStopping, objectHolder, accountingServiceClient, rasClient, 
                paymentManager, planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, 
                this.plan);

        Map<String, String> options = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), options.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), options.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_STRING, options.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method, it must
    // get all the necessary PostPaid parameters from a PropertiesHolder instance and
    // return these parameters as a Map.
    @Test
    public void testPostPaidPluginOptionsLoaderValidOptions() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, 
                INVOICE_WAIT_TIME, TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        Map<String, String> options = loader.load();
        
        assertEquals(USER_BILLING_INTERVAL, options.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(FINANCE_PLAN_RULES_FILE_PATH, options.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH));
        assertEquals(INVOICE_WAIT_TIME, options.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method and the property
    // UserBillingInterval is missing, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingUserBillingInterval() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(null, FINANCE_PLAN_RULES_FILE_PATH, 
                INVOICE_WAIT_TIME, TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method and the property
    // FinancePlanRulesFilePath is missing, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingFinancePlanRulesFilePath() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(USER_BILLING_INTERVAL, null, 
                INVOICE_WAIT_TIME, TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method and the property
    // InvoiceWaitTime is missing, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingInvoiceWaitTime() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, 
                null, TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method and the property
    // TimeToWaitBeforeStopping is missing, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingTimeToWaitBeforeStopping() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, 
                INVOICE_WAIT_TIME, null, String.valueOf(DEFAULT_RESOURCE_VALUE));
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // test case: When calling the PostPaidPluginOptionsLoader.load method and the property
    // DefaultResourceValue is missing, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingDefaultResourceValue() throws ConfigurationErrorException {
        setUpPropertiesHolderOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, 
                INVOICE_WAIT_TIME, TIME_TO_WAIT_BEFORE_STOPPING, null);
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    private void setUpFinanceOptions() {
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE, String.valueOf(DEFAULT_RESOURCE_VALUE));
    }
    
    private void setUpPropertiesHolderOptions(String userBillingInterval, String financePlanRulesPath, 
            String invoiceWaitTime, String timeToWaitBeforeStopping, String defaultResourceValue) {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        
        Mockito.when(propertiesHolder.getProperty(PaymentRunner.USER_BILLING_INTERVAL)).thenReturn(userBillingInterval);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH)).thenReturn(financePlanRulesPath);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.INVOICE_WAIT_TIME)).thenReturn(invoiceWaitTime);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.TIME_TO_WAIT_BEFORE_STOPPING)).thenReturn(timeToWaitBeforeStopping);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.FINANCE_PLAN_DEFAULT_RESOURCE_VALUE)).thenReturn(defaultResourceValue);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }

    private void setUpPlan() throws InvalidParameterException {
        this.plan = Mockito.mock(FinancePolicy.class);
        Mockito.when(this.plan.toString()).thenReturn(RULES_STRING);
        
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
