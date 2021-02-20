package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.TimeUtils;

public class PaymentRunnerTest {
	
	private static final String ID_USER_1 = "userId1";
	private static final String ID_USER_2 = "userId2";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private static final Long BILLING_INTERVAL = 30L;
	private static final Long RECORD_ID_1 = 0L;
	private static final Long RECORD_ID_2 = 1L;
	private static final Long INITIAL_USER_1_LAST_BILLING_TIME = 0L;
	private static final Long INITIAL_USER_2_LAST_BILLING_TIME = 1L;
	private long invoiceWaitTime = 10; 
	
	private FinanceUser user1;
	private FinanceUser user2;
	
	private DatabaseManager databaseManager;
	
	private TimeUtils timeUtils;
	
	private List<FinanceUser> userList;
	private List<Record> userRecords;
	
	private Record record1;
	private Record record2;
	
	private AccountingServiceClient accountingServiceClient;
	private PaymentManager paymentManager;
	
	private List<Long> timeValues;
	
	// TODO documentation
	@Test
	public void testRunIsBillingTime() throws FogbowException {
		//
		// Setting up mocks
		//
		this.timeUtils = Mockito.spy(TimeUtils.class);
		timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL, 
				INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL + 1, 
				INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL + 2);
		Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
				timeValues.get(1), timeValues.get(2));
		
		setUpDatabase();
		setUpAccounting();
		
		this.paymentManager = Mockito.mock(PaymentManager.class);
		
		PaymentRunner paymentRunner = new PaymentRunner(invoiceWaitTime, 
				databaseManager, accountingServiceClient, paymentManager, timeUtils);
		
		
		paymentRunner.doRun();
		
		
		//
		// Checking payment state
		//
		
		// PaymentRunner triggered payment correctly
		Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_1);
		Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2);
		
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
		assertEquals(String.valueOf(timeValues.get(0)), user1.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
		assertEquals(String.valueOf(timeValues.get(1)), user2.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
	}
	
	// TODO documentation
	@Test
	public void testRunNotBillingTime() throws FogbowException {
		//
		// Setting up mocks
		//
		this.timeUtils = Mockito.spy(TimeUtils.class);
		timeValues = Arrays.asList(5L, 10L, 15L);
		
		Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
				timeValues.get(1), timeValues.get(2));
		
		setUpDatabase();
		setUpAccounting();
		
		this.paymentManager = Mockito.mock(PaymentManager.class);
		
		PaymentRunner paymentRunner = new PaymentRunner(invoiceWaitTime, 
				databaseManager, accountingServiceClient, paymentManager, timeUtils);
		
		
		paymentRunner.doRun();
		
		
		//
		// Checking payment state
		//
		
		// payment is not triggered
		Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(Mockito.anyString());
		
		// last period records are not modified
		assertNull(user1.getPeriodRecords());
		assertNull(user2.getPeriodRecords());
		
		// last billing time is not altered
		assertEquals(String.valueOf(0), user1.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
		assertEquals(String.valueOf(1), user2.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
	}

	// TODO documentation
	@Test
	public void testErrorOnAcquiringUserRecords() throws FogbowException {
		//
		// Setting up mocks
		//
		this.timeUtils = Mockito.spy(TimeUtils.class);
		timeValues = Arrays.asList(INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL, 
				INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL + 1, 
				INITIAL_USER_1_LAST_BILLING_TIME + BILLING_INTERVAL + 2);
		Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(timeValues.get(0),
				timeValues.get(1), timeValues.get(2));
		
		setUpDatabase();
		setUpErrorAccounting();
		
		this.paymentManager = Mockito.mock(PaymentManager.class);
		
		PaymentRunner paymentRunner = new PaymentRunner(invoiceWaitTime, 
				databaseManager, accountingServiceClient, paymentManager, timeUtils);
		
		
		paymentRunner.doRun();
		
		//
		// Checking payment state
		//
		
		// PaymentRunner triggered payment correctly
		Mockito.verify(paymentManager, Mockito.never()).startPaymentProcess(ID_USER_1);
		Mockito.verify(paymentManager, Mockito.times(1)).startPaymentProcess(ID_USER_2);

		// PaymentRunner set the last period records
		List<Record> records = user1.getPeriodRecords();
		assertNull(records);

		List<Record> records2 = user2.getPeriodRecords();
		assertEquals(2, records2.size());
		assertEquals(RECORD_ID_1, records2.get(0).getId());
		assertEquals(RECORD_ID_2, records2.get(1).getId());

		// PaymentRunner changed users last billing time for user2 only
		assertEquals(String.valueOf(INITIAL_USER_1_LAST_BILLING_TIME), user1.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
		assertEquals(String.valueOf(timeValues.get(1)), user2.getProperty(PaymentRunner.USER_LAST_BILLING_TIME));
	}
	
	private void setUpDatabase() {
		this.databaseManager = Mockito.mock(DatabaseManager.class);
		this.userList = new ArrayList<FinanceUser>();
		Mockito.doReturn(userList).when(databaseManager).getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
		
		this.user1 = new FinanceUser(new HashMap<String, String>());
		user1.setId(ID_USER_1);
		user1.setProvider(PROVIDER_USER_1);
		user1.setProperty(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(BILLING_INTERVAL));
		user1.setProperty(PaymentRunner.USER_LAST_BILLING_TIME, String.valueOf(INITIAL_USER_1_LAST_BILLING_TIME));
		
		this.user2 = new FinanceUser(new HashMap<String, String>());
		user2.setId(ID_USER_2);
		user2.setProvider(PROVIDER_USER_2);
		user2.setProperty(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(BILLING_INTERVAL));
		user2.setProperty(PaymentRunner.USER_LAST_BILLING_TIME, String.valueOf(INITIAL_USER_2_LAST_BILLING_TIME));
		
		userList.add(user1);
		userList.add(user2);
	}

	private void setUpAccounting() throws FogbowException {
		this.userRecords = new ArrayList<Record>();
		this.record1 = new Record(RECORD_ID_1, null, null, null, null, null, null, null, null, 0, null);
		this.record2 = new Record(RECORD_ID_2, null, null, null, null, null, null, null, null, 0, null);
		userRecords.add(record1);
		userRecords.add(record2);
		
		this.accountingServiceClient = Mockito.mock(AccountingServiceClient.class);
		Mockito.doReturn(userRecords).when(accountingServiceClient).getUserRecords(Mockito.anyString(), Mockito.anyString(), 
				Mockito.anyString(), Mockito.anyString());
	}
	
	private void setUpErrorAccounting() throws FogbowException {
		this.userRecords = new ArrayList<Record>();
		this.record1 = new Record(RECORD_ID_1, null, null, null, null, null, null, null, null, 0, null);
		this.record2 = new Record(RECORD_ID_2, null, null, null, null, null, null, null, null, 0, null);
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
