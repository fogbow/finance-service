package cloud.fogbow.fs.core.plugins.finance.prepaid;

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

public class PrePaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private DatabaseManager databaseManager;
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long creditsDeductionWaitTime = 1L;
	
	// TODO documentation
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
		
		assertTrue(prePaidFinancePlugin.managesUser(USER_ID_1));
		assertTrue(prePaidFinancePlugin.managesUser(USER_ID_2));
		assertFalse(prePaidFinancePlugin.managesUser(USER_NOT_MANAGED));
	}
}
