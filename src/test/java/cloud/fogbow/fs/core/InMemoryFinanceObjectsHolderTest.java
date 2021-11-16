package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListFactory;

// TODO update this test to use mocked list iterators
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class InMemoryFinanceObjectsHolderTest {

    private static final String PLAN_NAME_1 = "plan1";
    private static final String PLAN_NAME_2 = "plan2";
    private static final String NEW_PLAN_NAME_1 = "newplan1";
    
    private DatabaseManager databaseManager;
    private MultiConsumerSynchronizedListFactory listFactory;
    private MultiConsumerSynchronizedList<PersistablePlanPlugin> planSynchronizedList;
    private MultiConsumerSynchronizedList<FinanceUser> plan1Users;
    private MultiConsumerSynchronizedList<FinanceUser> plan2Users;

    private List<PersistablePlanPlugin> plansList;
    private PersistablePlanPlugin plan1;
    private PersistablePlanPlugin plan2;
    private Map<String, String> rulesPlan1;
    private InMemoryUsersHolder usersHolder;
    private InMemoryFinanceObjectsHolder objectHolder;
    
    @Before
    public void setUp() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpPlans();
        setUpDatabase();
        setUpLists();
        setUpUsersHolder();
    }
    
    // test case: When creating a new InMemoryFinanceObjectsHolder instance, the constructor
    // must acquire the data from FinancePlans using the DatabaseManager and
    // prepare its internal data holding lists properly.
    @Test
    public void testConstructorSetsUpDataStructuresCorrectly() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory);
        
        Mockito.verify(databaseManager).getRegisteredPlans();
        Mockito.verify(listFactory).getList();
        Mockito.verify(planSynchronizedList).addItem(plan1);
        Mockito.verify(planSynchronizedList).addItem(plan2);
    }
    
    // test case: When calling the reset method, it must reacquire all the data from
    // FinancePlans using the DatabaseManager and set up new data holding lists properly.
    @Test
    public void testResetSetsUpDataStructuresCorrectly() throws InternalServerErrorException, ConfigurationErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.reset();
        
        Mockito.verify(databaseManager).getRegisteredPlans();
        Mockito.verify(listFactory).getList();
        Mockito.verify(planSynchronizedList).addItem(plan1);
        Mockito.verify(planSynchronizedList).addItem(plan2);
    }
    
    // test case: When calling the method registerFinancePlan, it must add the new 
    // FinancePlan to the list of finance plans and then persist the plan using
    // the DatabaseManager.
    @Test
    public void testRegisterFinancePlan() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        PersistablePlanPlugin planPlugin1 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(planPlugin1.getName()).thenReturn(NEW_PLAN_NAME_1);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.registerFinancePlan(planPlugin1);
        
        Mockito.verify(planSynchronizedList).addItem(planPlugin1);
        Mockito.verify(databaseManager).savePlan(planPlugin1);
    }

    // test case: When calling the method registerFinancePlan and the
    // FinancePlan passed as argument uses an already used plan name, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testCannotRegisterFinancePlanWithAlreadyUsedName() throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin planPlugin1 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(planPlugin1.getName()).thenReturn(PLAN_NAME_1);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        

        objectHolder.registerFinancePlan(planPlugin1);
    }
    
    // test case: When calling the method getFinancePlan, it must iterate 
    // correctly over the plans list and return the correct FinancePlan instance.
    @Test
    public void testGetFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        assertEquals(plan1, objectHolder.getFinancePlan(PLAN_NAME_1));
        assertEquals(plan2, objectHolder.getFinancePlan(PLAN_NAME_2));
        
        Mockito.verify(planSynchronizedList, Mockito.times(2)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlan and a concurrent modification on the 
    // plans list occurs, it must restart the iteration and return the correct FinancePlan instance.
    @Test
    public void testGetFinancePlanListChanges() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).
        thenReturn(plan1).
        thenThrow(new ModifiedListException()).
        thenReturn(plan1, plan2);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        assertEquals(plan2, objectHolder.getFinancePlan(PLAN_NAME_2));
        
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlan and the plans list throws an
    // InternalServerErrorException, it must rethrow the exception.
    @Test(expected = InternalServerErrorException.class)
    public void testGetFinancePlanListThrowsException() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).
        thenReturn(plan1).
        thenThrow(new InternalServerErrorException()).
        thenReturn(plan1, plan2);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.getFinancePlan(PLAN_NAME_2);
    }
    
    // test case: When calling the method getFinancePlan passing as argument an unknown
    // plan name, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanUnknownPlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.getFinancePlan("unknownplan");
    }
    
    // test case: When calling the method removeFinancePlan, it must check if the list of users
    // subscribed to the plan is empty, remove the given plan from the plans list and 
    // delete the plan from the database using the DatabaseManager.
    @Test
    public void testRemoveFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.removeFinancePlan(PLAN_NAME_1);
        
        Mockito.verify(databaseManager).removePlan(plan1);
        Mockito.verify(planSynchronizedList).removeItem(plan1);
        Mockito.verify(usersHolder).getRegisteredUsersByPlan(PLAN_NAME_1);
        Mockito.verify(plan1Users).isEmpty();
    }
    
    // test case: When calling the method removeFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.removeFinancePlan("unknownplan");
    }
    
    // test case: When calling the method removeFinancePlan passing as argument
    // a plan with registered users, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveFinancePlanRegisteredUsersListIsNotEmpty() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.removeFinancePlan(PLAN_NAME_2);
    }
    
    // test case: When calling the method updateFinancePlan, it must call the method 
    // update on the correct FinancePlan instance and then persist the plan data
    // using the DatabaseManager.
    @Test
    public void testUpdateFinancePlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updateFinancePlan(PLAN_NAME_1, updatedPlanInfo);
        
        
        Mockito.verify(plan1).setOptions(updatedPlanInfo);
        Mockito.verify(databaseManager).savePlan(plan1);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method updateFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updateFinancePlan("unknownplan", updatedPlanInfo);
    }
    
    // test case: When calling the method getFinancePlanOptions, it must return a 
    // Map containing the plan data related to the correct FinancePlan instance.
    @Test
    public void testGetFinancePlanMap() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        rulesPlan1 = new HashMap<String, String>();
        Mockito.when(plan1.getOptions()).thenReturn(rulesPlan1);
        
        
        Map<String, String> returnedMap = objectHolder.getFinancePlanOptions(PLAN_NAME_1);
        
        
        assertEquals(rulesPlan1, returnedMap);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlanOptions passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanMapUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);

        objectHolder.getFinancePlanOptions("unknownplan");
    }

    private void setUpLists() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        planSynchronizedList = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).thenReturn(plan1, plan2, null);

        plan1Users = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(plan1Users.isEmpty()).thenReturn(true);
        
        plan2Users = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(plan2Users.isEmpty()).thenReturn(false);
        
        listFactory = Mockito.mock(MultiConsumerSynchronizedListFactory.class);
        Mockito.doReturn(planSynchronizedList).when(listFactory).getList();
    }

    private void setUpDatabase() {
        databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.getRegisteredPlans()).thenReturn(plansList);
    }

    private void setUpPlans() {
        plan1 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(plan1.getName()).thenReturn(PLAN_NAME_1);
        
        plan2 = Mockito.mock(PersistablePlanPlugin.class);
        Mockito.when(plan2.getName()).thenReturn(PLAN_NAME_2);
        
        plansList = new ArrayList<PersistablePlanPlugin>();
        plansList.add(plan1);
        plansList.add(plan2);
    }
    
    private void setUpUsersHolder() {
        this.usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(this.usersHolder.getRegisteredUsersByPlan(PLAN_NAME_1)).thenReturn(plan1Users);
        Mockito.when(this.usersHolder.getRegisteredUsersByPlan(PLAN_NAME_2)).thenReturn(plan2Users);
    }
}
