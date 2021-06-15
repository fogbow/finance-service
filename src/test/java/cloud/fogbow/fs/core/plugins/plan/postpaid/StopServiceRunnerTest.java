package cloud.fogbow.fs.core.plugins.plan.postpaid;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class StopServiceRunnerTest {
    private static final String ID_USER_1 = "userId1";
    private static final String ID_USER_2 = "userId2";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final Integer CONSUMER_ID = 0;
    private static final String PLAN_NAME = "planName";
    private long stopServiceWaitTime = 1L;
    private StopServiceRunner stopServiceRunner;
    private InMemoryUsersHolder objectHolder;
    private InvoiceManager paymentManager;
    private RasClient rasClient;
    private FinanceUser user1;
    private FinanceUser user2;
    private MultiConsumerSynchronizedList<FinanceUser> users;
    private DebtsPaymentChecker debtsChecker;
    
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
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        
        
        stopServiceRunner.doRun();
        
        
        
        Mockito.verify(this.user1).setStoppedResources(true);
        // User has paid. Therefore, its state must not change.
        Mockito.verify(this.user2, Mockito.never()).setStoppedResources(true);

        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
        Mockito.verify(objectHolder).saveUser(user1);
        
        // User has paid. Therefore, must not call RasClient to pause resources.
        Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_2);
        Mockito.verify(objectHolder, Mockito.never()).saveUser(user2);
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
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).pauseResourcesByUser(ID_USER_1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime ,objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        
        
        stopServiceRunner.doRun();
        
        
        // Since an exception was thrown when pausing user resources, 
        // the user state must not change.
        Mockito.verify(this.user1, Mockito.never()).setStoppedResources(true);
        Mockito.verify(this.user2).setStoppedResources(true);

        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
        Mockito.verify(objectHolder, Mockito.never()).saveUser(user1);
        
        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_2);
        Mockito.verify(objectHolder).saveUser(user2);
    }
    
    // test case: When calling the method purgeUserResources, it must
    // call the RasClient to purge the user resources.
    @Test
    public void testPurgeUserResources() 
            throws ModifiedListException, FogbowException {
        setUpDatabase();
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime ,objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        stopServiceRunner.purgeUserResources(user1);
        
        Mockito.verify(rasClient).purgeUser(ID_USER_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the method purgeUserResources and
    // the RasClient throws a FogbowException, it must catch the exception
    // and throw an InternalServerErrorException.
    @Test
    public void testPurgeUserResourcesRasClientThrowsException() 
            throws ModifiedListException, FogbowException {
        setUpDatabase();
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).purgeUser(ID_USER_1, PROVIDER_USER_1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime ,objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        try {
            stopServiceRunner.purgeUserResources(user1);
            Assert.fail("purgeUserResources is expected to throw InternalServerErrorException.");
        } catch (InternalServerErrorException e) {
            
        }
            
        Mockito.verify(rasClient).purgeUser(ID_USER_1, PROVIDER_USER_1);
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
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);

        
        
        stopServiceRunner.doRun();
        
        
        
        Mockito.verify(this.user1, Mockito.never()).setStoppedResources(false);
        Mockito.verify(this.user2).setStoppedResources(false);

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
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).resumeResourcesByUser(ID_USER_1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);

        
        
        stopServiceRunner.doRun();
        
        
        // Since an exception was thrown when resuming user resources, 
        // the user state must not change.
        Mockito.verify(this.user1, Mockito.never()).setStoppedResources(false);
        Mockito.verify(this.user2).setStoppedResources(false);

        Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_1);
        Mockito.verify(rasClient, Mockito.times(1)).resumeResourcesByUser(ID_USER_2);
    }
    
    // test case: When calling the method resumeResourcesForUser, it must
    // call the RasClient to resume the resources and update the user state.
    @Test
    public void testResumeResourcesForUser() 
            throws ModifiedListException, FogbowException {
        setUpDatabaseResumeResources();
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        stopServiceRunner.resumeResourcesForUser(user1);

        Mockito.verify(this.user1).setStoppedResources(false);
        Mockito.verify(rasClient).resumeResourcesByUser(ID_USER_1);
        Mockito.verify(objectHolder).saveUser(user1);
    }
    
    // test case: When calling the method resumeResourcesForUser and
    // the RasClient throws a FogbowException, it must catch the exception
    // and throw an InternalServerErrorException. Also, the user state
    // must remain unchanged.
    @Test
    public void testResumeResourcesForUserRasClientThrowsException() 
            throws ModifiedListException, FogbowException {
        setUpDatabaseResumeResources();
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).resumeResourcesByUser(ID_USER_1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        try {
            stopServiceRunner.resumeResourcesForUser(user1);
            Assert.fail("resumeResourcesForUser is expected to throw InternalServerErrorException.");
        } catch (InternalServerErrorException e) {
            
        }
        
        Mockito.verify(this.user1, Mockito.never()).setStoppedResources(false);
        Mockito.verify(rasClient).resumeResourcesByUser(ID_USER_1);
        Mockito.verify(objectHolder, Mockito.never()).saveUser(user1);
    }
    
    // test case: When calling the method doRun and the user payment
    // state check fails, it must skip the process for the current user and 
    // continue checking the other users' states.
    @Test
    public void testFailedToCheckIfUserPaid() throws ModifiedListException, FogbowException {
        // 
        // Set up
        //
        setUpDatabase();
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doThrow(InvalidParameterException.class).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(true).when(debtsChecker).hasPaid(ID_USER_2, PROVIDER_USER_2);
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        
        
        stopServiceRunner.doRun();
        
        
        // Failed to determine whether user has paid or not.
        Mockito.verify(this.user1, Mockito.never()).setStoppedResources(Mockito.anyBoolean());
        // User has not paid. Therefore, its state must change.
        Mockito.verify(this.user2).setStoppedResources(true);

        // Failed to determine whether user has paid or not.
        Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_1);
        // User has not paid. Therefore, must call RasClient to pause resources.
        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_2);
    }
    
    // test case: When calling the method doRun and a ModifiedListException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testUserListChanges() throws ModifiedListException, FogbowException {
        // 
        // Set up
        //
        setUpDatabaseUserListChanges();
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        
        stopServiceRunner.doRun();
        

        Mockito.verify(this.user1).setStoppedResources(true);
        // Failed to get user2. Therefore, its state must not change.
        Mockito.verify(this.user2, Mockito.never()).setStoppedResources(Mockito.anyBoolean());

        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
        // Failed to get user2. Therefore, its state must not change.
        Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_2);
    }
    
    // test case: When calling the method doRun and a InternalServerErrorException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testFailedToGetUser() throws ModifiedListException, FogbowException {
        // 
        // Set up
        //
        setUpDatabaseFailToGetUser();
        
        paymentManager = Mockito.mock(InvoiceManager.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        
        Mockito.doReturn(false).when(paymentManager).hasPaid(ID_USER_1, PROVIDER_USER_1);
        Mockito.doReturn(false).when(debtsChecker).hasPaid(ID_USER_1, PROVIDER_USER_1);
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                paymentManager, rasClient, debtsChecker);
        
        
        stopServiceRunner.doRun();
        

        Mockito.verify(this.user1).setStoppedResources(true);
        // Failed to get user2. Therefore, its state must not change.
        Mockito.verify(this.user2, Mockito.never()).setStoppedResources(Mockito.anyBoolean());

        Mockito.verify(rasClient, Mockito.times(1)).pauseResourcesByUser(ID_USER_1);
        // Failed to get user2. Therefore, its state must not change.
        Mockito.verify(rasClient, Mockito.never()).pauseResourcesByUser(ID_USER_2);
    }
    
    private void setUpDatabase() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(user1, user2, null);

        setUpObjectHolder();
    }
    
    private void setUpDatabaseUserListChanges() throws InternalServerErrorException, ModifiedListException { 
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(user1).thenThrow(new ModifiedListException());

        setUpObjectHolder();
    }
    
    private void setUpDatabaseFailToGetUser() throws InternalServerErrorException, ModifiedListException {
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(user1).thenThrow(new InternalServerErrorException());

        setUpObjectHolder();
    }

    private void setUpUsers() {
        this.user1 = Mockito.mock(FinanceUser.class);
        Mockito.when(this.user1.getId()).thenReturn(ID_USER_1);
        Mockito.when(this.user1.getProvider()).thenReturn(PROVIDER_USER_1);

        this.user2 = Mockito.mock(FinanceUser.class);
        Mockito.when(this.user2.getId()).thenReturn(ID_USER_2);
        Mockito.when(this.user2.getProvider()).thenReturn(PROVIDER_USER_2);
    }
    
    private void setUpObjectHolder() {
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getRegisteredUsersByPlan(PLAN_NAME)).thenReturn(users);
    }
    
    private void setUpDatabaseResumeResources() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpDatabase();
        
        Mockito.when(user1.stoppedResources()).thenReturn(true);
        Mockito.when(user2.stoppedResources()).thenReturn(true);
    }
}
