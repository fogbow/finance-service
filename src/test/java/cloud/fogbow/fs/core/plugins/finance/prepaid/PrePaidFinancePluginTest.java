package cloud.fogbow.fs.core.plugins.finance.prepaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

public class PrePaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NAME_1 = "userName1";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
	private DatabaseManager databaseManager;
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long creditsDeductionWaitTime = 1L;
	
	// test case: When calling the managesUser method, it must
	// get all managed users from a DatabaseManager instance and
	// check if the given user belongs to the managed users list.
	@Test
	public void testManagesUser() {
		FinanceUser financeUser1 = new FinanceUser();
		financeUser1.setId(USER_ID_1);
		
		FinanceUser financeUser2 = new FinanceUser();
		financeUser2.setId(USER_ID_2);
		
		ArrayList<FinanceUser> users = new ArrayList<>();
		users.add(financeUser1);
		users.add(financeUser2);
		
		this.databaseManager = Mockito.mock(DatabaseManager.class);
		Mockito.when(databaseManager.getRegisteredUsersByPaymentType(PrePaidFinancePlugin.PLUGIN_NAME)).thenReturn(users);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		assertTrue(prePaidFinancePlugin.managesUser(USER_ID_1, PROVIDER_USER_1));
		assertTrue(prePaidFinancePlugin.managesUser(USER_ID_2, PROVIDER_USER_2));
		assertFalse(prePaidFinancePlugin.managesUser(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user is financially OK, 
	// the method must return true.
	@Test
	public void testIsAuthorizedCreateOperationUserIsOKFinancially() {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for an
	// operation other than creation and the user is not financially OK, 
	// the method must return true.
	@Test
	public void testIsAuthorizedNonCreationOperationUserIsNotOKFinancially() {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user is not financially OK, 
	// the method must return false.
	@Test
	public void testIsAuthorizedCreationOperationUserIsNotOKFinancially() {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
	}
}
