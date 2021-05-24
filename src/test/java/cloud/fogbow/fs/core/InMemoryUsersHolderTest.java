package cloud.fogbow.fs.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.plugins.plan.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

// TODO review documentation
public class InMemoryUsersHolderTest {
    private static final String USER_ID_1 = "userId1";
    private static final String PROVIDER_ID_1 = "providerId1";
    private static final String USER_ID_2 = "userId2";
    private static final String PROVIDER_ID_2 = "providerId2";
    private static final String USER_ID_TO_ADD = "userIdToAdd";
    private static final String PROVIDER_ID_TO_ADD = "providerIdToAdd";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
    private static final String INVOICE_ID_3 = "invoiceId3";
    private static final String PLAN_NAME_1 = "plan1";
    private static final String PLAN_NAME_2 = "plan2";
    private InMemoryUsersHolder objectHolder;
    private DatabaseManager databaseManager;
    private ArrayList<PlanPlugin> plansList;
    private UserCreditsFactory userCreditsFactory;
    private UserCredits userCredits1;
    private ArrayList<UserCredits> creditsList;
    private UserCredits userCredits2;
    private Invoice invoice1;
    private Invoice invoice2;
    private Invoice invoice3;
    private ArrayList<Invoice> invoicesList;
    private FinanceUser user1;
    private FinanceUser user2;
    private ArrayList<FinanceUser> usersList;
    private MultiConsumerSynchronizedList<FinanceUser> userSynchronizedList1;
    private MultiConsumerSynchronizedList<FinanceUser> userSynchronizedList2;
    private MultiConsumerSynchronizedListFactory listFactory;

    @Before
    public void setUp() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpUsers();
        setUpInvoices();
        setUpCredits();
        setUpDatabase();
        setUpLists();
    }
    
    // TODO documentation
    @Test
    public void testConstructorLoadsUserDataCorrectlyManyUsers() throws InternalServerErrorException, ConfigurationErrorException {
        new InMemoryUsersHolder(databaseManager, listFactory, userCreditsFactory);
        
        Mockito.verify(databaseManager).getRegisteredUsers();
        Mockito.verify(listFactory, Mockito.times(2)).getList();
        Mockito.verify(userSynchronizedList1).addItem(user1);
        Mockito.verify(userSynchronizedList2).addItem(user2);
    }
    
    // TODO documentation
    @Test
    public void testConstructorLoadsUserDataCorrectlyNoUser() throws InternalServerErrorException, ConfigurationErrorException {
        Mockito.when(databaseManager.getRegisteredUsers()).thenReturn(new ArrayList<FinanceUser>());
        
        new InMemoryUsersHolder(databaseManager, listFactory, userCreditsFactory);
        
        Mockito.verify(databaseManager).getRegisteredUsers();
        Mockito.verify(listFactory, Mockito.never()).getList();
    }
    
    // test case: When calling the method registerUser, it must add a new FinanceUser instance
    // to the list of FinanceUsers and persist the new user by calling the DatabaseManager.
    @Test
    public void testRegisterUser() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.registerUser(USER_ID_TO_ADD, PROVIDER_ID_TO_ADD, PLAN_NAME_1);
        
        Mockito.verify(userSynchronizedList1, Mockito.times(1)).addItem(Mockito.any(FinanceUser.class));
        Mockito.verify(databaseManager).saveUser(Mockito.any(FinanceUser.class));
    }

    // test case: When calling the method registerUser passing UserId and ProviderId
    // used by an already registered user, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void cannotRegisterUserWithAlreadyUsedUserIdAndProvider() throws InternalServerErrorException, 
            InvalidParameterException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);

        objectHolder.registerUser(USER_ID_1, PROVIDER_ID_1, PLAN_NAME_1);
    }
    
    // test case: When calling the method removeUser, it must remove the given user from
    // the list of FinanceUsers and delete the user from the database using the DatabaseManager.
    @Test
    public void testRemoveUser() 
            throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException, ModifiedListException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);        
        objectHolder.removeUser(USER_ID_1, PROVIDER_ID_1);
        
        Mockito.verify(userSynchronizedList1).removeItem(Mockito.any(FinanceUser.class));
        Mockito.verify(databaseManager).removeUser(USER_ID_1, PROVIDER_ID_1);
    }
    
    // test case: When calling the method removeUser passing unknown UserId and ProviderId, it 
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveUnknownUser() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.removeUser("unknownuser", "unknownprovider");
    }
    
    // test case: When calling the method getUserById, it must iterate over the correct list of 
    // users and return the correct user.
    @Test
    public void testGetUserById() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        usersByPlugin.put(PLAN_NAME_2, userSynchronizedList2);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        assertEquals(user1, objectHolder.getUserById(USER_ID_1, PROVIDER_ID_1));
        assertEquals(user2, objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2));
        
        Mockito.verify(userSynchronizedList1, Mockito.times(2)).stopIterating(Mockito.anyInt());
        Mockito.verify(userSynchronizedList2, Mockito.times(1)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getUserById and a concurrent modification on the list
    // occurs while the method is iterating over it, it must restart the iteration and return 
    // the correct user.
    @Test
    public void testGetUserByIdListChanges() throws InternalServerErrorException, ModifiedListException, InvalidParameterException, ConfigurationErrorException {
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).
        thenReturn(user1).
        thenThrow(new ModifiedListException()).
        thenReturn(user1, user2);

        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        assertEquals(user2, objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2));
        
        Mockito.verify(userSynchronizedList1, Mockito.times(1)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getUserById and the internal user list throws an
    // InternalServerErrorException while iterating over it, it must rethrow the exception.
    @Test(expected = InternalServerErrorException.class)
    public void testGetUserByIdListThrowsException() throws InternalServerErrorException, ModifiedListException, InvalidParameterException, ConfigurationErrorException {
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).
        thenReturn(user1).
        thenThrow(new InternalServerErrorException());

        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2);
    }
    
    // test case: When calling the method getUserById and the UserId passed as parameter is 
    // not known, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetUserByIdUnknownUser() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.getUserById("unknownuser", PROVIDER_ID_1);
    }
    
    // test case: When calling the method getUserById and the ProviderId passed as parameter 
    // is not known, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetUserByIdUnknownProvider() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        
        objectHolder.getUserById(USER_ID_1, "unknownprovider");
    }
    
    // test case: When calling the method changeOptions, it must change the options
    // used by the given user using the data contained in the given options map and
    // then persist the changes using the DatabaseManager.
    @Test
    public void testChangeOptions() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        HashMap<String, String> newOptions = new HashMap<String, String>();
        newOptions.put("option1", "optionvalue1");
        newOptions.put("option2", "optionvalue2");
         
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.changeOptions(USER_ID_1, PROVIDER_ID_1, newOptions);
        
        Mockito.verify(user1).setProperty("option1", "optionvalue1");
        Mockito.verify(user1).setProperty("option2", "optionvalue2");
        Mockito.verify(databaseManager).saveUser(user1);
    }
    
    // test case: When calling the method saveUser, it must persist the user
    // data using the DatabaseManager.
    @Test
    public void testSaveUser() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.saveUser(user1);
        
        Mockito.verify(databaseManager).saveUser(user1);
    }
    
    // test case: When calling the method saveUser and the user is not known, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSaveUserUnknownUser() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        
        FinanceUser unknownUser = Mockito.mock(FinanceUser.class);
        Mockito.when(unknownUser.getId()).thenReturn("unknownuser");
        Mockito.when(unknownUser.getProvider()).thenReturn("unknownprovider");
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        objectHolder.saveUser(unknownUser);
    }
    
    // TODO documentation
    @Test
    public void testGetRegisteredUsersByPlan() {
        Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin = 
                new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        usersByPlugin.put(PLAN_NAME_1, userSynchronizedList1);
        usersByPlugin.put(PLAN_NAME_2, userSynchronizedList2);
        
        MultiConsumerSynchronizedList<FinanceUser> emptySynchronizedList = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.doReturn(emptySynchronizedList).when(listFactory).getList();
        
        objectHolder = new InMemoryUsersHolder(databaseManager, userCreditsFactory, listFactory, usersByPlugin);
        
        assertEquals(userSynchronizedList1, objectHolder.getRegisteredUsersByPlan(PLAN_NAME_1));
        assertEquals(userSynchronizedList2, objectHolder.getRegisteredUsersByPlan(PLAN_NAME_2));
        assertEquals(emptySynchronizedList, objectHolder.getRegisteredUsersByPlan("unknownplan"));
    }
    
    private void setUpLists() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        userSynchronizedList1 = Mockito.mock(MultiConsumerSynchronizedList.class);
        userSynchronizedList2 = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).thenReturn(user1, null);
        Mockito.when(userSynchronizedList2.getNext(Mockito.anyInt())).thenReturn(user2, null);
        
        listFactory = Mockito.mock(MultiConsumerSynchronizedListFactory.class);
        Mockito.doReturn(userSynchronizedList1).doReturn(userSynchronizedList2).when(listFactory).getList();
    }

    private void setUpDatabase() {
        databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.getRegisteredUsers()).thenReturn(usersList);
        Mockito.when(databaseManager.getRegisteredPlanPlugins()).thenReturn(plansList);
    }
    
    private void setUpCredits() {
        userCreditsFactory = Mockito.mock(UserCreditsFactory.class);
        
        userCredits1 = Mockito.mock(UserCredits.class);
        Mockito.when(userCredits1.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(userCredits1.getProvider()).thenReturn(PROVIDER_ID_1);
        
        userCredits2 = Mockito.mock(UserCredits.class);
        Mockito.when(userCredits2.getUserId()).thenReturn(USER_ID_2);
        Mockito.when(userCredits2.getProvider()).thenReturn(PROVIDER_ID_2);
        
        creditsList = new ArrayList<UserCredits>();
        creditsList.add(userCredits1);
        creditsList.add(userCredits2);
    }

    private void setUpInvoices() {
        invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        Mockito.when(invoice1.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(invoice1.getProviderId()).thenReturn(PROVIDER_ID_1);
        
        invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        Mockito.when(invoice2.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(invoice2.getProviderId()).thenReturn(PROVIDER_ID_1);        
        
        invoice3 = Mockito.mock(Invoice.class);
        Mockito.when(invoice3.getInvoiceId()).thenReturn(INVOICE_ID_3);
        Mockito.when(invoice3.getUserId()).thenReturn(USER_ID_2);
        Mockito.when(invoice3.getProviderId()).thenReturn(PROVIDER_ID_2);
        
        invoicesList = new ArrayList<Invoice>();
        invoicesList.add(invoice1);
        invoicesList.add(invoice2);
        invoicesList.add(invoice3);
    }

    private void setUpUsers() {
        user1 = Mockito.mock(FinanceUser.class);
        Mockito.when(user1.getFinancePluginName()).thenReturn(PLAN_NAME_1);
        Mockito.when(user1.getId()).thenReturn(USER_ID_1);
        Mockito.when(user1.getProvider()).thenReturn(PROVIDER_ID_1);
        user2 = Mockito.mock(FinanceUser.class);
        Mockito.when(user2.getFinancePluginName()).thenReturn(PLAN_NAME_2);
        Mockito.when(user2.getId()).thenReturn(USER_ID_2);
        Mockito.when(user2.getProvider()).thenReturn(PROVIDER_ID_2);
        
        usersList = new ArrayList<FinanceUser>();
        usersList.add(user1);
        usersList.add(user2);
    }
}
