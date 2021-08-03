package cloud.fogbow.fs.core.plugins.plan.prepaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.models.VolumeItem;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class CreditsManagerTest {
    private static final String USER_ID1 = "userId1";
    private static final String PROVIDER1 = "provider1";
    private static final String USER_ID2 = "userId2";
    private static final String PROVIDER2 = "provider2";
    private static final String USER_ID3 = "userId3";
    private static final String PROVIDER3 = "provider3";
    private static final Long PAYMENT_START_TIME = 1L;
    private static final Long PAYMENT_END_TIME = 100L;
    private static final Double VALUE_ITEM_1 = 10.0;
    private static final Double VALUE_ITEM_2 = 5.0;
    private InMemoryUsersHolder objectHolder;
    private RecordUtils recordUtils;
    private UserCredits userCredits1;
    private UserCredits userCredits2;
    private UserCredits userCredits3;
    private FinancePolicy policy;
    private FinanceUser financeUser1;
    private FinanceUser financeUser2;
    private FinanceUser financeUser3;
    private List<Record> records;
    private Record record1;
    private Record record2;
    private ResourceItem resourceItem1;
    private ResourceItem resourceItem2;
    
    // test case: When calling the hasPaid method, if the given 
    // user has a credits value equal to zero or positive, it 
    // must return true. Otherwise, it must return false.
    @Test
    public void testHasPaid() throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        this.userCredits1 = Mockito.mock(UserCredits.class);
        this.userCredits2 = Mockito.mock(UserCredits.class);
        this.userCredits3 = Mockito.mock(UserCredits.class);

        Mockito.when(userCredits1.getCreditsValue()).thenReturn(10.0);
        Mockito.when(userCredits2.getCreditsValue()).thenReturn(0.0);
        Mockito.when(userCredits3.getCreditsValue()).thenReturn(-1.0);
        
        setUpRecords();
        setUpPlan();
        setUpDatabase();
        
        CreditsManager creditsManager = new CreditsManager(objectHolder, policy, recordUtils);
        
        assertTrue(creditsManager.hasPaid(USER_ID1, PROVIDER1));
        assertTrue(creditsManager.hasPaid(USER_ID2, PROVIDER2));
        assertFalse(creditsManager.hasPaid(USER_ID3, PROVIDER3));
    }
    
    // test case: When calling the startPaymentProcess method, it must get
    // the user records and the finance plan from the database and deduct 
    // the resource items from the UserCredits according to the user records.
    // Also, it must save the credits in the database.
    @Test
    public void testStartPaymentProcess() throws InternalServerErrorException, InvalidParameterException {
        this.userCredits1 = Mockito.mock(UserCredits.class);
        
        setUpRecords();
        setUpPlan();
        setUpDatabase();
        
        CreditsManager creditsManager = new CreditsManager(objectHolder, policy, recordUtils);
        
        creditsManager.startPaymentProcess(USER_ID1, PROVIDER1, PAYMENT_START_TIME, PAYMENT_END_TIME, records);
        
        Mockito.verify(userCredits1).deduct(resourceItem1, VALUE_ITEM_1, new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
        Mockito.verify(userCredits1).deduct(resourceItem2, VALUE_ITEM_2, new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
        Mockito.verify(financeUser1).setLastBillingTime(PAYMENT_END_TIME);
        Mockito.verify(objectHolder).saveUser(financeUser1);
    }

    // test case: When calling the startPaymentProcess method and the 
    // user has no records for the period, it must not deduct credits.
    @Test
    public void testStartPaymentProcessNoRecords() throws InternalServerErrorException, InvalidParameterException {
        this.userCredits1 = Mockito.mock(UserCredits.class);
        records = new ArrayList<Record>();
        
        setUpPlan();
        setUpDatabase();
        
        CreditsManager creditsManager = new CreditsManager(objectHolder, policy, recordUtils);
        
        creditsManager.startPaymentProcess(USER_ID1, PROVIDER1, PAYMENT_START_TIME, PAYMENT_END_TIME, records);
        
        Mockito.verify(userCredits1, Mockito.never()).deduct(Mockito.any(), Mockito.any(), Mockito.any());
    }
    
    // test case: When calling the startPaymentProcess method and 
    // the RecordUtils throws an exception when getting the resource items
    // from the records, it must catch the exception and throw an
    // InternalServerErrorException.
    @Test
    public void testStartPaymentProcessErrorOnGettingItemFromRecord() throws InternalServerErrorException, InvalidParameterException {
        this.userCredits1 = Mockito.mock(UserCredits.class);
        
        record1 = Mockito.mock(Record.class);
        record2 = Mockito.mock(Record.class);
        
        records = new ArrayList<Record>();
        records.add(record1);
        records.add(record2);
        
        this.recordUtils = Mockito.mock(RecordUtils.class);
        Mockito.when(recordUtils.getItemFromRecord(record1)).thenThrow(new InvalidParameterException());
        Mockito.when(recordUtils.getItemFromRecord(record2)).thenThrow(new InvalidParameterException());
        Mockito.when(recordUtils.getTimeFromRecord(record1, PAYMENT_START_TIME, PAYMENT_END_TIME)).
        thenReturn(new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
        Mockito.when(recordUtils.getTimeFromRecord(record2, PAYMENT_START_TIME, PAYMENT_END_TIME)).
        thenReturn(new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
         
        setUpPlan();
        setUpDatabase();
        
        CreditsManager creditsManager = new CreditsManager(objectHolder, policy, recordUtils);
        
        try {
            creditsManager.startPaymentProcess(USER_ID1, PROVIDER1, PAYMENT_START_TIME, PAYMENT_END_TIME, records);
            Assert.fail("startPaymentProcess is expected to throw exception.");
        } catch (InternalServerErrorException e) {
        }
        
        Mockito.verify(userCredits1, Mockito.never()).deduct(Mockito.any(), Mockito.any(), Mockito.any());
    }
    
    // test case: When calling the startPaymentProcess method and 
    // the FinancePlan throws an exception when getting the financial value 
    // of a resource item, it must catch the exception and throw an
    // InternalServerErrorException.
    @Test
    public void testStartPaymentProcessErrorOnGettingItemFinancialValue() throws InvalidParameterException, InternalServerErrorException {
        this.userCredits1 = Mockito.mock(UserCredits.class);
        
        setUpRecords();

        this.policy = Mockito.mock(FinancePolicy.class);
        Mockito.when(policy.getItemFinancialValue(resourceItem1)).thenThrow(new InvalidParameterException());
        Mockito.when(policy.getItemFinancialValue(resourceItem2)).thenThrow(new InvalidParameterException());
        
        setUpDatabase();
        
        CreditsManager creditsManager = new CreditsManager(objectHolder, policy, recordUtils);
        
        try {
            creditsManager.startPaymentProcess(USER_ID1, PROVIDER1, PAYMENT_START_TIME, PAYMENT_END_TIME, records);            
            Assert.fail("startPaymentProcess is expected to throw exception.");
        } catch (InternalServerErrorException e) {
        }
        
        Mockito.verify(userCredits1, Mockito.never()).deduct(Mockito.any(), Mockito.any(), Mockito.any());
    }
    
    private void setUpRecords() throws InvalidParameterException {
        record1 = Mockito.mock(Record.class);
        record2 = Mockito.mock(Record.class);
        
        records = new ArrayList<Record>();
        records.add(record1);
        records.add(record2);
        
        this.resourceItem1 = new ComputeItem(1, 2);
        this.resourceItem2 = new VolumeItem(100);
        
        this.recordUtils = Mockito.mock(RecordUtils.class);
        Mockito.when(recordUtils.getItemFromRecord(record1)).thenReturn(resourceItem1);
        Mockito.when(recordUtils.getItemFromRecord(record2)).thenReturn(resourceItem2);
        Mockito.when(recordUtils.getTimeFromRecord(record1, PAYMENT_START_TIME, PAYMENT_END_TIME)).
        thenReturn(new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
        Mockito.when(recordUtils.getTimeFromRecord(record2, PAYMENT_START_TIME, PAYMENT_END_TIME)).
        thenReturn(new Double(PAYMENT_END_TIME - PAYMENT_START_TIME));
    }
    
    private void setUpPlan() throws InvalidParameterException {
        this.policy = Mockito.mock(FinancePolicy.class);
        Mockito.when(policy.getItemFinancialValue(resourceItem1)).thenReturn(VALUE_ITEM_1);
        Mockito.when(policy.getItemFinancialValue(resourceItem2)).thenReturn(VALUE_ITEM_2);
    }
    
    private void setUpDatabase() throws InvalidParameterException, InternalServerErrorException {
        this.financeUser1 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser1.getCredits()).thenReturn(this.userCredits1);
       
        this.financeUser2 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser2.getCredits()).thenReturn(this.userCredits2);
        
        this.financeUser3 = Mockito.mock(FinanceUser.class);
        Mockito.when(financeUser3.getCredits()).thenReturn(this.userCredits3);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID1, PROVIDER1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID2, PROVIDER2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_ID3, PROVIDER3)).thenReturn(financeUser3);
//        Mockito.when(objectHolder.getOrDefaultFinancePlan(plan)).thenReturn(financePlan);
    }
}
