package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunnerTest {

	private static final String ID_USER_1 = "userId1";
	private static final String ID_USER_2 = "userId2";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private long stopServiceWaitTime = 1L;
	private StopServiceRunner stopServiceRunner;
	private DatabaseManager databaseManager;
	private PaymentManager paymentManager;
	private RasClient rasClient;
	private ArrayList<FinanceUser> userList;
	private FinanceUser user1;
	private FinanceUser user2;
	
	// TODO documentation
	@Test
	public void testStoppingUserServices() {
		setUpDatabase();
		
		paymentManager = Mockito.mock(PaymentManager.class);
		
		Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1);
		Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2);
		
		rasClient = Mockito.mock(RasClient.class);
		
		stopServiceRunner = new StopServiceRunner(stopServiceWaitTime ,databaseManager, paymentManager, rasClient);
		
		stopServiceRunner.doRun();
		
		assertTrue(this.user1.stoppedResources());
		assertFalse(this.user2.stoppedResources());
		
		Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
		Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_2);
	}
	
	// TODO documentation
	@Test
	public void testResumingUserServices() {
		setUpDatabaseResumeResources();
		
		paymentManager = Mockito.mock(PaymentManager.class);
		
		Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1);
		Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2);
		
		rasClient = Mockito.mock(RasClient.class);
		
		stopServiceRunner = new StopServiceRunner(stopServiceWaitTime ,databaseManager, paymentManager, rasClient);
		
		stopServiceRunner.doRun();
		
		assertTrue(this.user1.stoppedResources());
		assertFalse(this.user2.stoppedResources());
		
		Mockito.verify(rasClient, Mockito.never()).resumeResourcesByUser(ID_USER_1);
		Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_2);
	}
	
	private void setUpDatabase() {
		this.databaseManager = Mockito.mock(DatabaseManager.class);
		this.userList = new ArrayList<FinanceUser>();
		Mockito.doReturn(userList).when(databaseManager).getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
		
		this.user1 = new FinanceUser(new HashMap<String, String>());
		user1.setId(ID_USER_1);
		user1.setProvider(PROVIDER_USER_1);

		this.user2 = new FinanceUser(new HashMap<String, String>());
		user2.setId(ID_USER_2);
		user2.setProvider(PROVIDER_USER_2);

		userList.add(user1);
		userList.add(user2);
	}
	
	private void setUpDatabaseResumeResources() {
		setUpDatabase();
		
		user1.setStoppedResources(true);
		user2.setStoppedResources(true);
	}
}
