package cloud.fogbow.fs.core.plugins.plan.postpaid;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.ResourcesPolicy;
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
    private RasClient rasClient;
    private FinanceUser user1;
    private FinanceUser user2;
    private MultiConsumerSynchronizedList<FinanceUser> users;
    private ResourcesPolicy resourcesPolicy;
    
    // test case: When calling the method doRun, for each user in the users list, it 
    // must call the method updateUserState of the ResourcesPolicy.
    @Test
    public void testCheckingUserState() 
            throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
        // 
        // Set up
        //
        setUpDatabase();
        
        this.resourcesPolicy = Mockito.mock(ResourcesPolicy.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        stopServiceRunner.doRun();
        
        Mockito.verify(this.resourcesPolicy).updateUserState(user1);
        Mockito.verify(this.resourcesPolicy).updateUserState(user2);
    }

    // test case: When calling the method doRun, for each user in the users list, it 
    // must call the method updateUserState of the ResourcesPolicy. If the call to updateUserState
    // throws an InvalidParameterException, it must continue the iteration over the users list.
    @Test
    public void testCheckingUserStateResourcesPolicyThrowsException() 
            throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
        // 
        // Set up
        //
        setUpDatabase();
        
        this.resourcesPolicy = Mockito.mock(ResourcesPolicy.class);
        
        Mockito.doThrow(InvalidParameterException.class).when(this.resourcesPolicy).updateUserState(user1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        stopServiceRunner.doRun();
        
        Mockito.verify(this.resourcesPolicy).updateUserState(user1);
        Mockito.verify(this.resourcesPolicy).updateUserState(user2);
    }
    
    // test case: When calling the method purgeUserResources, it must
    // call the RasClient to purge the user resources.
    @Test
    public void testPurgeUserResources() 
            throws ModifiedListException, FogbowException {
        setUpDatabase();
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
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
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        try {
            stopServiceRunner.purgeUserResources(user1);
            Assert.fail("purgeUserResources is expected to throw InternalServerErrorException.");
        } catch (InternalServerErrorException e) {
            
        }
            
        Mockito.verify(rasClient).purgeUser(ID_USER_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the method resumeResourcesForUser, it must
    // call the RasClient to resume the resources and update the user state.
    @Test
    public void testResumeResourcesForUser() 
            throws ModifiedListException, FogbowException {
        setUpDatabase();
        
        rasClient = Mockito.mock(RasClient.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        stopServiceRunner.resumeResourcesForUser(user1);

        Mockito.verify(rasClient).resumeResourcesByUser(ID_USER_1, PROVIDER_USER_1);
    }
    
    // test case: When calling the method resumeResourcesForUser and
    // the RasClient throws a FogbowException, it must catch the exception
    // and throw an InternalServerErrorException. Also, the user state
    // must remain unchanged.
    @Test
    public void testResumeResourcesForUserRasClientThrowsException() 
            throws ModifiedListException, FogbowException {
        setUpDatabase();
        
        rasClient = Mockito.mock(RasClient.class);
        Mockito.doThrow(new FogbowException("message")).when(rasClient).resumeResourcesByUser(ID_USER_1, PROVIDER_USER_1);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        try {
            stopServiceRunner.resumeResourcesForUser(user1);
            Assert.fail("resumeResourcesForUser is expected to throw InternalServerErrorException.");
        } catch (InternalServerErrorException e) {
            
        }
        
        Mockito.verify(rasClient).resumeResourcesByUser(ID_USER_1, PROVIDER_USER_1);
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

        this.resourcesPolicy = Mockito.mock(ResourcesPolicy.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        
        stopServiceRunner.doRun();
        

        Mockito.verify(this.resourcesPolicy).updateUserState(user1);
        Mockito.verify(this.resourcesPolicy, Mockito.never()).updateUserState(user2);
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

        this.resourcesPolicy = Mockito.mock(ResourcesPolicy.class);
        
        stopServiceRunner = new StopServiceRunner(PLAN_NAME, stopServiceWaitTime, objectHolder, 
                rasClient, resourcesPolicy);
        
        
        stopServiceRunner.doRun();

        Mockito.verify(this.resourcesPolicy).updateUserState(user1);
        Mockito.verify(this.resourcesPolicy, Mockito.never()).updateUserState(user2);
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
}
