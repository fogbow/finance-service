package cloud.fogbow.fs.core.plugins.payment.postpaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.payment.ComputeItem;
import cloud.fogbow.fs.core.plugins.payment.VolumeItem;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class DefaultInvoiceManagerTest {

	private static final double ITEM_1_TIME = 5.0;
	private static final double ITEM_2_TIME = 1.1;
	private static final double ITEM_1_VALUE = 10.0;
	private static final double ITEM_2_VALUE = 5.0;
	private static final int VOLUME_ITEM_SIZE = 100;
	private static final int COMPUTE_ITEM_VCPU = 2;
	private static final int COMPUTE_ITEM_MEM = 4;
	private static final String USER_ID_1 = "userId1";
	private static final String PROVIDER_ID_1 = "provider1";
	private static final String PLAN_NAME_1 = "plan1";
	private static final String USER_ID_2 = "userId2";
	private static final String PROVIDER_ID_2 = "provider2";
	private static final String USER_ID_3 = "userId3";
	private static final String PROVIDER_ID_3 = "provider3";
	private static final String INVOICE_1_JSON_REPR = "invoice1json";
	private static final String INVOICE_2_JSON_REPR = "invoice2json";
	private static final String INVOICE_3_JSON_REPR = "invoice3json";
	private static final String INVOICE_4_JSON_REPR = "invoice4json";
	private Long invoiceStartTime = 0L;
	private Long invoiceEndTime = 100L;
	private DatabaseManager databaseManager;
	private RecordUtils resourceItemFactory;
	private InvoiceBuilder invoiceBuilder;
	private FinanceUser user1;
	private Record record1;
	private Record record2;
	private List<Record> records;
	private VolumeItem item1;
	private ComputeItem item2;
	private FinancePlan financePlan;
	private Invoice invoiceToAdd;
	
	// test case: When calling the startPaymentProcess method, it must
	// collect the user records and generate an Invoice using a 
	// InvoiceBuilder properly.
	@Test
	public void testStartPaymentProcess() throws InternalServerErrorException, InvalidParameterException {
		setUpInvoiceData();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		invoiceManager.startPaymentProcess(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime);
		
		Mockito.verify(this.invoiceBuilder).setUserId(USER_ID_1);
		Mockito.verify(this.invoiceBuilder).setProviderId(PROVIDER_ID_1);
		Mockito.verify(this.invoiceBuilder).addItem(item1, ITEM_1_VALUE, ITEM_1_TIME);
		Mockito.verify(this.invoiceBuilder).addItem(item2, ITEM_2_VALUE, ITEM_2_TIME);
		Mockito.verify(this.databaseManager).saveInvoice(invoiceToAdd);
	}
	
	// test case: When calling the startPaymentProcess method, 
	// if the ResourceItemFactory throws an exception when generating
	// a ResourceItem from a Record, the method must throw an
	// InternalServerErrorException.
	@Test(expected = InternalServerErrorException.class)
	public void testStartPaymentProcessErrorOnGettingItemFromRecord() throws InvalidParameterException, 
	InternalServerErrorException {
		setUpDataStructures();
		setUpErrorResourceItemFactory();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		invoiceManager.startPaymentProcess(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime);
	}
	
	// test case: When calling the startPaymentProcess method, 
	// if the FinancePlan throws an exception when getting the 
	// financial value for an item, the method must throw an
	// InternalServerErrorException.
	@Test(expected = InternalServerErrorException.class)
	public void testStartPaymentProcessErrorOnGettingValueFromPlan() throws InvalidParameterException, 
	InternalServerErrorException {
		setUpDataStructures();
		setUpErrorFinancePlan();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		invoiceManager.startPaymentProcess(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime);
	}

	// test case: When calling the hasPaid method, it must check 
	// the user invoices states. If any of the states is DEFAULTING,
	// it must return false, or return true otherwise.
	@Test
	public void testHasPaid() throws InvalidParameterException, InternalServerErrorException {
		setUpInvoiceData();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		// user1's invoices are: invoice1 (WAITING) and invoice2 (PAID)
		// user2's invoices are: invoice3 (PAID) and invoice4 (DEFAULTING)
		// user3 has no invoices
		assertTrue(invoiceManager.hasPaid(USER_ID_1, PROVIDER_ID_1));
		assertFalse(invoiceManager.hasPaid(USER_ID_2, PROVIDER_ID_2));
		assertTrue(invoiceManager.hasPaid(USER_ID_3, PROVIDER_ID_3));
	}
	
	// test case: When calling the getUserFinanceState method using the 
	// property ALL_USER_INVOICES, it must get all the user's invoices and,
	// for each invoice, generate a representing string. Then, return a 
	// concatenation of the strings.
	@Test
	public void testGetUserFinanceStateAllInvoices() throws InvalidParameterException {
		setUpInvoiceData();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		String user1State = invoiceManager.getUserFinanceState(USER_ID_1, PROVIDER_ID_1, 
				DefaultInvoiceManager.ALL_USER_INVOICES_PROPERTY_NAME);
		String user2State = invoiceManager.getUserFinanceState(USER_ID_2, PROVIDER_ID_2, 
				DefaultInvoiceManager.ALL_USER_INVOICES_PROPERTY_NAME);
		String user3State = invoiceManager.getUserFinanceState(USER_ID_3, PROVIDER_ID_3, 
				DefaultInvoiceManager.ALL_USER_INVOICES_PROPERTY_NAME);
		
		assertEquals("[" + String.join(DefaultInvoiceManager.PROPERTY_VALUES_SEPARATOR, 
				INVOICE_1_JSON_REPR, INVOICE_2_JSON_REPR) + "]", user1State);
		assertEquals("[" + String.join(DefaultInvoiceManager.PROPERTY_VALUES_SEPARATOR, 
				INVOICE_3_JSON_REPR, INVOICE_4_JSON_REPR)+ "]", user2State);
		assertEquals("[]", user3State);
	}
	
	// test case: When calling the getUserFinanceState method using an
	// unknown property, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetUserFinanceStateUnknownProperty() throws InvalidParameterException {
		setUpInvoiceData();
		
		DefaultInvoiceManager invoiceManager = new DefaultInvoiceManager(databaseManager, 
				PLAN_NAME_1, resourceItemFactory, invoiceBuilder);
		
		invoiceManager.getUserFinanceState(USER_ID_1, PROVIDER_ID_1, "unknownProperty");
	}
	
	private void setUpInvoiceData() throws InvalidParameterException {
		setUpDataStructures();
		setUpUtilClasses();
	}

	private void setUpUtilClasses() throws InvalidParameterException {
		setUpFinancePlan();
		setUpResourceItemFactory();
		setUpDatabaseManager();
		setUpInvoiceBuilder();
	}
	
	private void setUpErrorResourceItemFactory() throws InvalidParameterException {
		setUpFinancePlan();

		this.resourceItemFactory = Mockito.mock(RecordUtils.class);
		Mockito.when(this.resourceItemFactory.getItemFromRecord(record1)).thenThrow(new InvalidParameterException());
		Mockito.when(this.resourceItemFactory.getTimeFromRecord(record1, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_1_TIME);
		Mockito.when(this.resourceItemFactory.getItemFromRecord(record2)).thenReturn(item2);
		Mockito.when(this.resourceItemFactory.getTimeFromRecord(record2, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_2_TIME);
		
		setUpDatabaseManager();
		setUpInvoiceBuilder();
	}
	
	private void setUpErrorFinancePlan() throws InvalidParameterException {
		this.financePlan = Mockito.mock(FinancePlan.class);
		Mockito.when(this.financePlan.getItemFinancialValue(item1)).thenThrow(new InvalidParameterException());
		Mockito.when(this.financePlan.getItemFinancialValue(item2)).thenReturn(ITEM_2_VALUE);

		setUpResourceItemFactory();
		setUpDatabaseManager();
		setUpInvoiceBuilder();
	}
	
	private void setUpInvoiceBuilder() {
		this.invoiceBuilder = Mockito.mock(InvoiceBuilder.class);
		Mockito.when(this.invoiceBuilder.buildInvoice()).thenReturn(invoiceToAdd);
	}

	private void setUpDatabaseManager() {
		this.databaseManager = Mockito.mock(DatabaseManager.class);
		
		List<Invoice> invoiceListUser1 = new ArrayList<Invoice>();
		Invoice invoice1 = Mockito.mock(Invoice.class);
		Mockito.when(invoice1.getState()).thenReturn(InvoiceState.WAITING);
		Mockito.when(invoice1.toString()).thenReturn(INVOICE_1_JSON_REPR);
		Invoice invoice2 = Mockito.mock(Invoice.class);
		Mockito.when(invoice2.getState()).thenReturn(InvoiceState.PAID);
		Mockito.when(invoice2.toString()).thenReturn(INVOICE_2_JSON_REPR);
		
		invoiceListUser1.add(invoice1);
		invoiceListUser1.add(invoice2);
		
		List<Invoice> invoiceListUser2 = new ArrayList<Invoice>();
		Invoice invoice3 = Mockito.mock(Invoice.class);
		Mockito.when(invoice3.getState()).thenReturn(InvoiceState.PAID);
		Mockito.when(invoice3.toString()).thenReturn(INVOICE_3_JSON_REPR);
		Invoice invoice4 = Mockito.mock(Invoice.class);
		Mockito.when(invoice4.getState()).thenReturn(InvoiceState.DEFAULTING);
		Mockito.when(invoice4.toString()).thenReturn(INVOICE_4_JSON_REPR);
		
		invoiceListUser2.add(invoice3);
		invoiceListUser2.add(invoice4);

		Mockito.when(this.databaseManager.getUserById(USER_ID_1, PROVIDER_ID_1)).thenReturn(user1);
		Mockito.when(this.databaseManager.getInvoiceByUserId(USER_ID_1, PROVIDER_ID_1)).thenReturn(invoiceListUser1);
		Mockito.when(this.databaseManager.getInvoiceByUserId(USER_ID_2, PROVIDER_ID_2)).thenReturn(invoiceListUser2);
		Mockito.when(this.databaseManager.getInvoiceByUserId(USER_ID_3, PROVIDER_ID_3)).thenReturn(new ArrayList<Invoice>());
		Mockito.when(this.databaseManager.getFinancePlan(PLAN_NAME_1)).thenReturn(financePlan);
	}

	private void setUpResourceItemFactory() throws InvalidParameterException {
		this.resourceItemFactory = Mockito.mock(RecordUtils.class);
		Mockito.when(this.resourceItemFactory.getItemFromRecord(record1)).thenReturn(item1);
		Mockito.when(this.resourceItemFactory.getTimeFromRecord(record1, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_1_TIME);
		Mockito.when(this.resourceItemFactory.getItemFromRecord(record2)).thenReturn(item2);
		Mockito.when(this.resourceItemFactory.getTimeFromRecord(record2, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_2_TIME);
	}

	private void setUpFinancePlan() throws InvalidParameterException {
		this.financePlan = Mockito.mock(FinancePlan.class);
		Mockito.when(this.financePlan.getItemFinancialValue(item1)).thenReturn(ITEM_1_VALUE);
		Mockito.when(this.financePlan.getItemFinancialValue(item2)).thenReturn(ITEM_2_VALUE);
	}

	private void setUpDataStructures() throws InvalidParameterException {
		this.record1 = Mockito.mock(Record.class);
		this.record2 = Mockito.mock(Record.class);

		this.records = new ArrayList<Record>();
		this.records.add(record1);
		this.records.add(record2);
		
		this.item1 = new VolumeItem(VOLUME_ITEM_SIZE);
		this.item2 = new ComputeItem(COMPUTE_ITEM_VCPU, COMPUTE_ITEM_MEM);
		
		this.user1 = Mockito.mock(FinanceUser.class);
		Mockito.when(user1.getPeriodRecords()).thenReturn(this.records);
		
		this.invoiceToAdd = Mockito.mock(Invoice.class);
	}
}
