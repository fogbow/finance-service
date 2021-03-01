package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;

public class PostPaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private DatabaseManager databaseManager;
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long invoiceWaitTime = 1L;

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
		Mockito.when(databaseManager.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME)).thenReturn(users);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(databaseManager, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_1));
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_2));
		assertFalse(postPaidFinancePlugin.managesUser(USER_NOT_MANAGED));
	}
}
