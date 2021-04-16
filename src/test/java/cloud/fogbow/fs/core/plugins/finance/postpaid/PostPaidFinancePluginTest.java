package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;


public class PostPaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NAME_1 = "userName1";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String USER_BILLING_INTERVAL_1 = "10";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
	private DatabaseManager databaseManager;
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long invoiceWaitTime = 1L;
    private Map<String, String> financeOptions;
    private Map<String, String> financeState;

	// test case: When calling the managesUser method, it must
	// get all managed users from a DatabaseManager instance and
	// check if the given user belongs to the managed users list.
	@Test
	public void testManagesUser() {
		FinanceUser financeUser1 = new FinanceUser();
		financeUser1.setId(USER_ID_1);
		financeUser1.setProvider(PROVIDER_USER_1);
		
		FinanceUser financeUser2 = new FinanceUser();
		financeUser2.setId(USER_ID_2);
		financeUser2.setProvider(PROVIDER_USER_2);
		
		ArrayList<FinanceUser> users = new ArrayList<>();
		users.add(financeUser1);
		users.add(financeUser2);
		
		this.databaseManager = Mockito.mock(DatabaseManager.class);
		Mockito.when(databaseManager.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME)).thenReturn(users);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_1, PROVIDER_USER_1));
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_2, PROVIDER_USER_2));
		assertFalse(postPaidFinancePlugin.managesUser(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for an
	// operation other than creation and the user financial state is not good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is not good, 
	// the method must return false.
	@Test
	public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the addUser method, it must call the DatabaseManager
	// to register the user using given parameters.
	@Test
	public void testAddUser() throws InvalidParameterException {
	    this.databaseManager = Mockito.mock(DatabaseManager.class);
	    
	    financeOptions = new HashMap<String, String>();
	    financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL_1);

	    PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
	            accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
	    
	    
	    postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
	    
	    
	    Mockito.verify(this.databaseManager).registerUser(USER_ID_1, PROVIDER_USER_1, 
	            PostPaidFinancePlugin.PLUGIN_NAME, financeOptions);
	}
	
	// test case: When calling the addUser method and the finance options map 
	// does not contain some required financial option, it must throw an
	// InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testAddUserMissingOption() throws InvalidParameterException {
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        
        financeOptions = new HashMap<String, String>();

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the addUser method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testAddUserInvalidOption() throws InvalidParameterException {
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidvalue");

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method, it must call the DatabaseManager
    // to change the options for the given user.
    @Test
    public void testChangeOptions() throws InvalidParameterException {
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL_1);

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
        
        
        Mockito.verify(this.databaseManager).changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // does not contain some required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsMissingOption() throws InvalidParameterException {
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        
        financeOptions = new HashMap<String, String>();

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsInvalidOption() throws InvalidParameterException {
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidoption");

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the updateFinanceState method, it must get the correct invoices
    // from the database, change the invoices states and save the invoices.
    @Test
    public void testUpdateFinanceState() throws InvalidParameterException {
        Invoice invoice1 = Mockito.mock(Invoice.class);
        Invoice invoice2 = Mockito.mock(Invoice.class);
        
        this.databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.getInvoice(INVOICE_ID_1)).thenReturn(invoice1);
        Mockito.when(databaseManager.getInvoice(INVOICE_ID_2)).thenReturn(invoice2);
        
        financeState = new HashMap<String, String>();
        financeState.put(INVOICE_ID_1, InvoiceState.PAID.getValue());
        financeState.put(INVOICE_ID_2, InvoiceState.DEFAULTING.getValue());

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.updateFinanceState(USER_ID_1, PROVIDER_USER_1, financeState);
        
        
        Mockito.verify(invoice1).setState(InvoiceState.PAID);
        Mockito.verify(invoice2).setState(InvoiceState.DEFAULTING);
        Mockito.verify(databaseManager).saveInvoice(invoice1);
        Mockito.verify(databaseManager).saveInvoice(invoice2);
    }
}
