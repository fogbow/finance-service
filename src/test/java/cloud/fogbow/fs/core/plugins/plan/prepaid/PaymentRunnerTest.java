package cloud.fogbow.fs.core.plugins.plan.prepaid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;

public class PaymentRunnerTest {
    private static final String ID_USER_1 = "userId1";
    private static final String ID_USER_2 = "userId2";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final Long RECORD_ID_1 = 0L;
    private static final Long RECORD_ID_2 = 1L;
    private static final Long INITIAL_USER_1_LAST_BILLING_TIME = 0L;
    private static final Long INITIAL_USER_2_LAST_BILLING_TIME = 1L;
    private static final Integer CONSUMER_ID = 0;
    private static final String PLAN_NAME = "planName";
    private long creditsDeductionWaitTime = 10; 
    
    private FinanceUser user1;
    private FinanceUser user2;
    
    private InMemoryUsersHolder objectHolder;
    
    private TimeUtils timeUtils;
    
    private List<Record> userRecords;
    
    private Record record1;
    private Record record2;
    
    private AccountingServiceClient accountingServiceClient;
    private CreditsManager paymentManager;
    
    private List<Long> timeValues;
    
    private MultiConsumerSynchronizedList<FinanceUser> users;
    
    // test case: When calling the doRun method, it must get the
    // list of users from the DatabaseManager. For each user 
    // it must get the user records, set the records in the database and 
    // start payment.
    @Test
    public void testDoRun() throws FogbowException, ModifiedListException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.mock(TimeUtils.class);
        // Set time values used by the PaymentRunner
        // The first value is the billing time for the first user
        // The second value is the billing time for the second user
        timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + 1, 
                INITIAL_USER_1_LAST_BILLING_TIME + 2, 
                INITIAL_USER_1_LAST_BILLING_TIME + 3);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
                timeValues.get(1), timeValues.get(2));
        
        setUpDatabase();
        setUpAccounting();
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        
        PaymentRunner paymentRunner = new PaymentRunner(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);
        
        
        paymentRunner.doRun();
        
        
        //
        // Checking payment state
        //
        
        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0), userRecords);
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2, PROVIDER_USER_2, 
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1), userRecords);
    }
    
    // test case: When calling the doRun method and an exception
    // is thrown when acquiring user records, it must handle the
    // exception and continue checking the remaining users.
    @Test
    public void testErrorOnAcquiringUserRecords() throws FogbowException, ModifiedListException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.mock(TimeUtils.class);
        // Set time values used by the PaymentRunner
        // The first value is the billing time for the first user
        // The second value is the billing time for the second user
        timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + 1, 
                INITIAL_USER_1_LAST_BILLING_TIME + 2, 
                INITIAL_USER_1_LAST_BILLING_TIME + 3);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
                timeValues.get(1), timeValues.get(2));
        
        setUpDatabase();
        setUpErrorAccounting();
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        
        PaymentRunner paymentRunner = new PaymentRunner(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);
        
        
        paymentRunner.doRun();
        
        //
        // Checking payment state
        //
        
        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0), userRecords);
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2, PROVIDER_USER_2, 
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1), userRecords);
    }
    
    // test case: When calling the doRun method and a ModifiedListException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testUserListChanges() throws ModifiedListException, FogbowException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.mock(TimeUtils.class);
        // Set time values used by the PaymentRunner
        // The first value is the billing time for the first user
        // The second value is the billing time for the second user
        timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + 1, 
                INITIAL_USER_1_LAST_BILLING_TIME + 2, 
                INITIAL_USER_1_LAST_BILLING_TIME + 3);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
                timeValues.get(1), timeValues.get(2));
        
        setUpDatabaseUserListChanges();
        setUpAccounting();
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        
        PaymentRunner paymentRunner = new PaymentRunner(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);
        
        
        paymentRunner.doRun();
        
        
        // Tries to get both users
        
        Mockito.verify(users, Mockito.times(2)).getNext(Mockito.anyInt());
        
        //
        // Checking payment state
        //
        
        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0), userRecords);
        Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_2, PROVIDER_USER_2,
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1), userRecords);
    }
    
    // test case: When calling the doRun method and an InternalServerErrorException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testErrorOnGettingItemFromList() throws ModifiedListException, FogbowException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.mock(TimeUtils.class);
        // Set time values used by the PaymentRunner
        // The first value is the billing time for the first user
        // The second value is the billing time for the second user
        timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + 1, 
                INITIAL_USER_1_LAST_BILLING_TIME + 2, 
                INITIAL_USER_1_LAST_BILLING_TIME + 3);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
                timeValues.get(1), timeValues.get(2));

        setUpDatabaseErrorOnGettingItemFromList();
        setUpAccounting();

        this.paymentManager = Mockito.mock(CreditsManager.class);

        PaymentRunner paymentRunner = new PaymentRunner(PLAN_NAME, creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);

        paymentRunner.doRun();

        // Tries to get both users

        Mockito.verify(users, Mockito.times(2)).getNext(Mockito.anyInt());

        //
        // Checking payment state
        //

        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1,
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0), userRecords);
        Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_2, PROVIDER_USER_2,
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1), userRecords);
    }
    
    private void setUpDatabase() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(this.user1, this.user2, null);

        setUpObjectHolder();
    }

    private void setUpDatabaseUserListChanges() throws InternalServerErrorException, ModifiedListException {
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(this.user1).thenThrow(new ModifiedListException());

        setUpObjectHolder();
    }
    
    private void setUpDatabaseErrorOnGettingItemFromList() throws InternalServerErrorException, ModifiedListException {
        setUpUsers();

        users = Mockito.mock(MultiConsumerSynchronizedList.class);

        Mockito.when(users.startIterating()).thenReturn(CONSUMER_ID);
        Mockito.when(users.getNext(CONSUMER_ID)).thenReturn(this.user1).thenThrow(new InternalServerErrorException());

        setUpObjectHolder();
    }

    private void setUpUsers() {
        this.user1 = Mockito.mock(FinanceUser.class);
        Mockito.when(this.user1.getLastBillingTime()).thenReturn(INITIAL_USER_1_LAST_BILLING_TIME);
        Mockito.when(this.user1.getId()).thenReturn(ID_USER_1);
        Mockito.when(this.user1.getProvider()).thenReturn(PROVIDER_USER_1);

        this.user2 = Mockito.mock(FinanceUser.class);
        Mockito.when(this.user2.getLastBillingTime()).thenReturn(INITIAL_USER_2_LAST_BILLING_TIME);
        Mockito.when(this.user2.getId()).thenReturn(ID_USER_2);
        Mockito.when(this.user2.getProvider()).thenReturn(PROVIDER_USER_2);
    }
    
    private void setUpObjectHolder() {
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getRegisteredUsersByPlan(PLAN_NAME)).thenReturn(users);
    }

    private void setUpAccounting() throws FogbowException {
        this.userRecords = new ArrayList<Record>();
        this.record1 = Mockito.mock(Record.class);
        Mockito.when(this.record1.getId()).thenReturn(RECORD_ID_1);
        this.record2 = Mockito.mock(Record.class);
        Mockito.when(this.record2.getId()).thenReturn(RECORD_ID_2);
        
        userRecords.add(record1);
        userRecords.add(record2);
        
        this.accountingServiceClient = Mockito.mock(AccountingServiceClient.class);
        Mockito.doReturn(userRecords).when(accountingServiceClient).getUserRecords(Mockito.anyString(), Mockito.anyString(), 
                Mockito.anyLong(), Mockito.anyLong());
    }

    private void setUpErrorAccounting() throws FogbowException {
        this.userRecords = new ArrayList<Record>();
        this.record1 = Mockito.mock(Record.class);
        Mockito.when(this.record1.getId()).thenReturn(RECORD_ID_1);
        this.record2 = Mockito.mock(Record.class);
        Mockito.when(this.record2.getId()).thenReturn(RECORD_ID_2);
        
        userRecords.add(record1);
        userRecords.add(record2);

        this.accountingServiceClient = Mockito.mock(AccountingServiceClient.class);

        Mockito.doThrow(FogbowException.class).when(accountingServiceClient).getUserRecords(ID_USER_1, PROVIDER_USER_1,
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0));
        Mockito.doReturn(userRecords).when(accountingServiceClient).getUserRecords(ID_USER_2, PROVIDER_USER_2, 
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1));
    }
}
