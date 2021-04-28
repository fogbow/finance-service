package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunnerTest {

	private static final String ID_USER_1 = "userId1";
	private static final String ID_USER_2 = "userId2";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
    private static final Integer CONSUMER_ID = 0;
	private long stopServiceWaitTime = 1L;
	private StopServiceRunner stopServiceRunner;
	private InMemoryFinanceObjectsHolder objectHolder;
	private PaymentManager paymentManager;
	private RasClient rasClient;
	private FinanceUser user1;
	private FinanceUser user2;
	
	// test case: When calling the method doRun, it must get the
	// list of users from the DatabaseManager. For each user, 
	// if the user has not paid and its resources have not been stopped yet,
	// then the method must call the RasClient to stop the resources
	// and update the user state.
	@Test
	public void testStoppingUserServices() throws FogbowException, ModifiedListException {
		// 
		// Set up
		//
		setUpDatabase();
		
		paymentManager = Mockito.mock(PaymentManager.class);
		
		Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
		Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
		
		rasClient = Mockito.mock(RasClient.class);
		
		stopServiceRunner = new StopServiceRunner(stopServiceWaitTime, objectHolder, paymentManager, rasClient);
		
		
		
		stopServiceRunner.doRun();
		
		
		
		assertTrue(this.user1.stoppedResources());
		// User has paid. Therefore, its state must not change.
		assertFalse(this.user2.stoppedResources());
		
		Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
		// User has paid. Therefore, must not call RasClient to pause resources.
		Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_2);
	}
	
	// test case: When calling the method doRun, it must get the
    // list of users from the DatabaseManager. For each user, 
    // if the user has not paid and its resources have not been stopped yet,
    // then the method must call the RasClient to stop the resources
    // and update the user state. If an exception is thrown by the RasClient,
	// then the method must skip the current user and continue checking the other
	// users' states.
    @Test
    public void testStoppingUserServicesRasClientThrowsException() throws FogbowException, ModifiedListException {
        // 
        // Set up
        //
        setUpDatabase();
        
        paymentManager = Mockito.mock(PaymentManager.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).pauseResourcesByUser(ID_USER_1);
        
        stopServiceRunner = new StopServiceRunner(stopServiceWaitTime ,objectHolder, paymentManager, rasClient);
        
        
        
        stopServiceRunner.doRun();
        
        
        // Since an exception was thrown when pausing user resources, 
        // the user state must not change.
        assertFalse(this.user1.stoppedResources());
        assertTrue(this.user2.stoppedResources());
        
        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_2);
    }
	
	// test case: When calling the method doRun, it must get the
	// list of users from the DatabaseManager. For each user,
	// if the user has paid and its resources have not been resumed yet,
	// then the method must call the RasClient to resume the resources
	// and update the user state.
	@Test
	public void testResumingUserServices() throws FogbowException, ModifiedListException {
		//
		// Set up
		//
		setUpDatabaseResumeResources();
		
		paymentManager = Mockito.mock(PaymentManager.class);
		
		Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
		Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
		
		rasClient = Mockito.mock(RasClient.class);
		
		stopServiceRunner = new StopServiceRunner(stopServiceWaitTime, objectHolder, paymentManager, rasClient);

		
		
		stopServiceRunner.doRun();
		
		
		
		assertTrue(this.user1.stoppedResources());
		assertFalse(this.user2.stoppedResources());
		
		Mockito.verify(rasClient, Mockito.never()).resumeResourcesByUser(ID_USER_1);
		Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_2);
	}
	
	// test case: When calling the method doRun, it must get the
    // list of users from the DatabaseManager. For each user,
    // if the user has paid and its resources have not been resumed yet,
    // then the method must call the RasClient to resume the resources
    // and update the user state. If an exception is thrown by the RasClient,
    // then the method must skip the current user and continue checking the other
    // users' states.
    @Test
    public void testResumingUserServicesRasClientThrowsException() throws FogbowException, ModifiedListException {
        //
        // Set up
        //
        setUpDatabaseResumeResources();
        
        paymentManager = Mockito.mock(PaymentManager.class);
        
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).resumeResourcesByUser(ID_USER_1);
        
        stopServiceRunner = new StopServiceRunner(stopServiceWaitTime, objectHolder, paymentManager, rasClient);

        
        
        stopServiceRunner.doRun();
        
        
        // Since an exception was thrown when resuming user resources, 
        // the user state must not change.
        assertTrue(this.user1.stoppedResources());
        assertFalse(this.user2.stoppedResources());
        
        Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_1);
        Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_2);
    }
	
	private void setUpDatabase() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
		this.user1 = new FinanceUser(new HashMap<String, String>());
		user1.setId(ID_USER_1);
		user1.setProvider(PROVIDER_USER_1);

		this.user2 = new FinanceUser(new HashMap<String, String>());
		user2.setId(ID_USER_2);
		user2.setProvider(PROVIDER_USER_2);

        MultiConsumerSynchronizedList<FinanceUser> users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(user1, user2, null);

        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME)).thenReturn(users);
	}
	
	private void setUpDatabaseResumeResources() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
		setUpDatabase();
		
		user1.setStoppedResources(true);
		user2.setStoppedResources(true);
	}
}
