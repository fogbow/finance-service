package cloud.fogbow.fs.core.plugins.finance.prepaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.Record;

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
	private long creditsDeductionWaitTime = 10; 
	
	private FinanceUser user1;
	private FinanceUser user2;
	
	private InMemoryFinanceObjectsHolder objectHolder;
	
	private TimeUtils timeUtils;
	
	private List<Record> userRecords;
	
	private Record record1;
	private Record record2;
	
	private AccountingServiceClient accountingServiceClient;
	private PaymentManager paymentManager;
	
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
		this.timeUtils = Mockito.spy(TimeUtils.class);
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
		
		this.paymentManager = Mockito.mock(PaymentManager.class);
		
		PaymentRunner paymentRunner = new PaymentRunner(creditsDeductionWaitTime, 
		        objectHolder, accountingServiceClient, paymentManager, timeUtils);
		
		
		paymentRunner.doRun();
		
		
		//
		// Checking payment state
		//
		
		// PaymentRunner triggered payment correctly
		Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
		        INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0));
		Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2, PROVIDER_USER_2, 
		        INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1));
		
		// PaymentRunner set the last period records
		List<Record> records = user1.getPeriodRecords();
		assertEquals(2, records.size());
		assertEquals(RECORD_ID_1, records.get(0).getId());
		assertEquals(RECORD_ID_2, records.get(1).getId());
		
		List<Record> records2 = user2.getPeriodRecords();
		assertEquals(2, records2.size());
		assertEquals(RECORD_ID_1, records2.get(0).getId());
		assertEquals(RECORD_ID_2, records2.get(1).getId());
		
		// PaymentRunner changed users last billing time
		assertEquals(String.valueOf(timeValues.get(0)), user1.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
		assertEquals(String.valueOf(timeValues.get(1)), user2.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
	}
	
	// test case: When calling the doRun method and an exception
	// is thrown when acquiring user records, it must handle the
	// exception and continue checking the remaining users.
	@Test
	public void testErrorOnAcquiringUserRecords() throws FogbowException, ModifiedListException {
		//
		// Setting up mocks
		//
		this.timeUtils = Mockito.spy(TimeUtils.class);
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
		
		this.paymentManager = Mockito.mock(PaymentManager.class);
		
		PaymentRunner paymentRunner = new PaymentRunner(creditsDeductionWaitTime, 
		        objectHolder, accountingServiceClient, paymentManager, timeUtils);
		
		
		paymentRunner.doRun();
		
		//
		// Checking payment state
		//
		
		// PaymentRunner triggered payment correctly
		Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0));
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2, PROVIDER_USER_2, 
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1));

		// PaymentRunner set the last period records
		
		// An exception is thrown when acquiring user1 records.
		// Therefore, user1 state has no records.
		List<Record> records = user1.getPeriodRecords();
		assertNull(records);

		List<Record> records2 = user2.getPeriodRecords();
		assertEquals(2, records2.size());
		assertEquals(RECORD_ID_1, records2.get(0).getId());
		assertEquals(RECORD_ID_2, records2.get(1).getId());

		// PaymentRunner changed users last billing time for user2 only
		assertEquals(String.valueOf(INITIAL_USER_1_LAST_BILLING_TIME), user1.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
		assertEquals(String.valueOf(timeValues.get(1)), user2.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
	}
	
	// test case: When calling the doRun method and a ModifiedListException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testUserListChanges() throws ModifiedListException, FogbowException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.spy(TimeUtils.class);
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
        
        this.paymentManager = Mockito.mock(PaymentManager.class);
        
        PaymentRunner paymentRunner = new PaymentRunner(creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);
        
        
        paymentRunner.doRun();
        
        
        // Tries to get both users
        
        Mockito.verify(users, Mockito.times(2)).getNext(Mockito.anyInt());
        
        //
        // Checking payment state
        //
        
        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1, 
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0));
        Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_2, PROVIDER_USER_2,
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1));

        // PaymentRunner set the last period records
        
        // An exception is thrown when trying to get user2.
        // Therefore, user2 state has no records.
        List<Record> records = user1.getPeriodRecords();
        assertEquals(2, records.size());
        assertEquals(RECORD_ID_1, records.get(0).getId());
        assertEquals(RECORD_ID_2, records.get(1).getId());

        List<Record> records2 = user2.getPeriodRecords();
        assertNull(records2);

        // PaymentRunner changed users last billing time for user1 only
        assertEquals(String.valueOf(timeValues.get(0)), user1.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
        assertEquals(String.valueOf(INITIAL_USER_2_LAST_BILLING_TIME), user2.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
    }
    
    // test case: When calling the doRun method and an InternalServerErrorException
    // is thrown when acquiring a user, it must handle the 
    // exception and stop the user iteration.
    @Test
    public void testErrorOnGettingItemFromList() throws ModifiedListException, FogbowException {
        //
        // Setting up mocks
        //
        this.timeUtils = Mockito.spy(TimeUtils.class);
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

        this.paymentManager = Mockito.mock(PaymentManager.class);

        PaymentRunner paymentRunner = new PaymentRunner(creditsDeductionWaitTime, 
                objectHolder, accountingServiceClient, paymentManager, timeUtils);

        paymentRunner.doRun();

        // Tries to get both users

        Mockito.verify(users, Mockito.times(2)).getNext(Mockito.anyInt());

        //
        // Checking payment state
        //

        // PaymentRunner triggered payment correctly
        Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1, PROVIDER_USER_1,
                INITIAL_USER_1_LAST_BILLING_TIME, timeValues.get(0));
        Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_2, PROVIDER_USER_2,
                INITIAL_USER_2_LAST_BILLING_TIME, timeValues.get(1));

        // PaymentRunner set the last period records

        // An exception is thrown when trying to get user2.
        // Therefore, user2 state has no records.
        List<Record> records = user1.getPeriodRecords();
        assertEquals(2, records.size());
        assertEquals(RECORD_ID_1, records.get(0).getId());
        assertEquals(RECORD_ID_2, records.get(1).getId());

        List<Record> records2 = user2.getPeriodRecords();
        assertNull(records2);

        // PaymentRunner changed users last billing time for user1 only
        assertEquals(String.valueOf(timeValues.get(0)), user1.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
        assertEquals(String.valueOf(INITIAL_USER_2_LAST_BILLING_TIME),
                user2.getProperty(FinanceUser.USER_LAST_BILLING_TIME));
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
        this.user1 = new FinanceUser(new HashMap<String, String>());
        user1.setId(ID_USER_1);
        user1.setProvider(PROVIDER_USER_1);
        user1.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(INITIAL_USER_1_LAST_BILLING_TIME));

        this.user2 = new FinanceUser(new HashMap<String, String>());
        user2.setId(ID_USER_2);
        user2.setProvider(PROVIDER_USER_2);
        user2.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(INITIAL_USER_2_LAST_BILLING_TIME));
    }
    
    private void setUpObjectHolder() {
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getRegisteredUsersByPaymentType(PrePaidFinancePlugin.PLUGIN_NAME)).thenReturn(users);
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
				Mockito.anyString(), Mockito.anyString());
	}

	private void setUpErrorAccounting() throws FogbowException {
		this.userRecords = new ArrayList<Record>();
		this.record1 = Mockito.mock(Record.class);
		Mockito.when(this.record1.getId()).thenReturn(RECORD_ID_1);
		this.record2 = Mockito.mock(Record.class);
		Mockito.when(this.record2.getId()).thenReturn(RECORD_ID_2);
		
		userRecords.add(record1);
		userRecords.add(record2);
		
		TimeUtils timeUtils = new TimeUtils();
		
		this.accountingServiceClient = Mockito.mock(AccountingServiceClient.class);
		Mockito.doThrow(FogbowException.class).when(accountingServiceClient).getUserRecords(ID_USER_1, 
				PROVIDER_USER_1, timeUtils.toDate(PaymentRunner.SIMPLE_DATE_FORMAT, INITIAL_USER_1_LAST_BILLING_TIME), 
						timeUtils.toDate(PaymentRunner.SIMPLE_DATE_FORMAT, timeValues.get(0)));
		
		Mockito.doReturn(userRecords).when(accountingServiceClient).getUserRecords(ID_USER_2, 
				PROVIDER_USER_2, timeUtils.toDate(PaymentRunner.SIMPLE_DATE_FORMAT, INITIAL_USER_2_LAST_BILLING_TIME), 
				timeUtils.toDate(PaymentRunner.SIMPLE_DATE_FORMAT, timeValues.get(1)));
	}
}
