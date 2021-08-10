package cloud.fogbow.fs.core.plugins.plan.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.models.VolumeItem;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class InvoiceManagerTest {
    private static final double ITEM_1_TIME = 5.0;
    private static final double ITEM_2_TIME = 1.1;
    private static final double ITEM_1_VALUE = 10.0;
    private static final double ITEM_2_VALUE = 5.0;
    private static final int VOLUME_ITEM_SIZE = 100;
    private static final int COMPUTE_ITEM_VCPU = 2;
    private static final int COMPUTE_ITEM_MEM = 4;
    private static final String USER_ID_1 = "userId1";
    private static final String PROVIDER_ID_1 = "provider1";
    private static final String USER_ID_2 = "userId2";
    private static final String PROVIDER_ID_2 = "provider2";
    private static final String USER_ID_3 = "userId3";
    private static final String PROVIDER_ID_3 = "provider3";
    private static final String INVOICE_1_JSON_REPR = "invoice1json";
    private static final String INVOICE_2_JSON_REPR = "invoice2json";
    private static final String INVOICE_3_JSON_REPR = "invoice3json";
    private static final String INVOICE_4_JSON_REPR = "invoice4json";
	private static final Double ITEM_1_TIME_ON_STATE_1 = 20.0;
	private static final Double ITEM_1_TIME_ON_STATE_2 = 30.0;
	private static final Double ITEM_2_TIME_ON_STATE_1 = 15.0;
	private static final Double ITEM_2_TIME_ON_STATE_2 = 35.0;
    private Long invoiceStartTime = 0L;
    private Long invoiceEndTime = 100L;
    private InMemoryUsersHolder objectHolder;
    private RecordUtils resourceItemFactory;
    private InvoiceBuilder invoiceBuilder;
    private FinanceUser user1;
    private FinanceUser user2;
    private FinanceUser user3;
    private Record record1;
    private Record record2;
    private List<Record> records;
    private VolumeItem item1;
    private ComputeItem item2;
    private FinancePolicy financePlan;
    private Invoice invoiceToAdd;
	private OrderState state1 = OrderState.FULFILLED;
	private OrderState state2 = OrderState.CLOSED;
	private Map<OrderState, Double> timePerStateItem1;
	private HashMap<OrderState, Double> timePerStateItem2;
    
    // test case: When calling the generateInvoiceForUser method, it must
    // collect the user records and generate an Invoice using a 
    // InvoiceBuilder properly.
    @Test
    public void testGenerateInvoiceForUser() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoiceData();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, 
                invoiceBuilder, financePlan);
        
        invoiceManager.generateInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
        
        Mockito.verify(this.invoiceBuilder).setUserId(USER_ID_1);
        Mockito.verify(this.invoiceBuilder).setProviderId(PROVIDER_ID_1);
        Mockito.verify(this.invoiceBuilder).addItem(item1, state1, ITEM_1_VALUE, ITEM_1_TIME_ON_STATE_1);
        Mockito.verify(this.invoiceBuilder).addItem(item1, state2, ITEM_1_VALUE, ITEM_1_TIME_ON_STATE_2);
        Mockito.verify(this.invoiceBuilder).addItem(item2, state1, ITEM_2_VALUE, ITEM_2_TIME_ON_STATE_1);
        Mockito.verify(this.invoiceBuilder).addItem(item2, state2, ITEM_2_VALUE, ITEM_2_TIME_ON_STATE_2);
        Mockito.verify(this.invoiceBuilder).setStartTime(invoiceStartTime);
        Mockito.verify(this.invoiceBuilder).setEndTime(invoiceEndTime);
        Mockito.verify(this.objectHolder).getUserById(USER_ID_1, PROVIDER_ID_1);
        Mockito.verify(this.user1).addInvoice(invoiceToAdd);
        Mockito.verify(this.user1).setLastBillingTime(invoiceEndTime);
        Mockito.verify(this.objectHolder).saveUser(user1);
    }
    
    // test case: When calling the generateInvoiceForUser method, 
    // if the ResourceItemFactory throws an exception when generating
    // a ResourceItem from a Record, the method must throw an
    // InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testGenerateInvoiceForUserErrorOnGettingItemFromRecord() throws InvalidParameterException, 
    InternalServerErrorException {
        setUpDataStructures();
        setUpErrorResourceItemFactory();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, invoiceBuilder, financePlan);
        
        invoiceManager.generateInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
    }
    
    // test case: When calling the generateInvoiceForUser method, 
    // if the FinancePlan throws an exception when getting the 
    // financial value for an item, the method must throw an
    // InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testGenerateInvoiceForUserErrorOnGettingValueFromPlan() throws InvalidParameterException, 
    InternalServerErrorException {
        setUpDataStructures();
        setUpErrorFinancePlan();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, invoiceBuilder, financePlan);
        
        invoiceManager.generateInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
    }
    
    // test case: When calling the generateLastInvoiceForUser method, it must
    // collect the user records and generate an Invoice using a 
    // InvoiceBuilder properly.
    @Test
    public void testGenerateLastInvoiceForUser() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoiceData();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, 
                invoiceBuilder, financePlan);
        
        invoiceManager.generateLastInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
        
        Mockito.verify(this.invoiceBuilder).setUserId(USER_ID_1);
        Mockito.verify(this.invoiceBuilder).setProviderId(PROVIDER_ID_1);
        Mockito.verify(this.invoiceBuilder).addItem(item1, state1, ITEM_1_VALUE, ITEM_1_TIME_ON_STATE_1);
        Mockito.verify(this.invoiceBuilder).addItem(item1, state2, ITEM_1_VALUE, ITEM_1_TIME_ON_STATE_2);
        Mockito.verify(this.invoiceBuilder).addItem(item2, state1, ITEM_2_VALUE, ITEM_2_TIME_ON_STATE_1);
        Mockito.verify(this.invoiceBuilder).addItem(item2, state2, ITEM_2_VALUE, ITEM_2_TIME_ON_STATE_2);
        Mockito.verify(this.invoiceBuilder).setStartTime(invoiceStartTime);
        Mockito.verify(this.invoiceBuilder).setEndTime(invoiceEndTime);
        Mockito.verify(this.objectHolder).getUserById(USER_ID_1, PROVIDER_ID_1);
        Mockito.verify(this.user1).addInvoiceAsDebt(invoiceToAdd);
        Mockito.verify(this.user1).setLastBillingTime(invoiceEndTime);
        Mockito.verify(this.objectHolder).saveUser(user1);
    }
    
    // test case: When calling the generateLastInvoiceForUser method, 
    // if the ResourceItemFactory throws an exception when generating
    // a ResourceItem from a Record, the method must throw an
    // InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testGenerateLastInvoiceForUserErrorOnGettingItemFromRecord() throws InvalidParameterException, 
    InternalServerErrorException {
        setUpDataStructures();
        setUpErrorResourceItemFactory();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, invoiceBuilder, financePlan);
        
        invoiceManager.generateLastInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
    }
    
    // test case: When calling the generateLastInvoiceForUser method, 
    // if the FinancePlan throws an exception when getting the 
    // financial value for an item, the method must throw an
    // InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testGenerateLastInvoiceForUserErrorOnGettingValueFromPlan() throws InvalidParameterException, 
    InternalServerErrorException {
        setUpDataStructures();
        setUpErrorFinancePlan();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, invoiceBuilder, financePlan);
        
        invoiceManager.generateLastInvoiceForUser(USER_ID_1, PROVIDER_ID_1, invoiceStartTime, invoiceEndTime, this.records);
    }

    // test case: When calling the hasPaid method, it must check 
    // the user invoices states. If any of the states is DEFAULTING,
    // it must return false, or return true otherwise.
    @Test
    public void testHasPaid() throws InvalidParameterException, InternalServerErrorException {
        setUpInvoiceData();
        
        InvoiceManager invoiceManager = new InvoiceManager(objectHolder, resourceItemFactory, invoiceBuilder, financePlan);
        
        // user1's invoices are: invoice1 (WAITING) and invoice2 (PAID)
        // user2's invoices are: invoice3 (PAID) and invoice4 (DEFAULTING)
        // user3 has no invoices
        assertTrue(invoiceManager.hasPaid(USER_ID_1, PROVIDER_ID_1));
        assertFalse(invoiceManager.hasPaid(USER_ID_2, PROVIDER_ID_2));
        assertTrue(invoiceManager.hasPaid(USER_ID_3, PROVIDER_ID_3));
    }
    
    private void setUpInvoiceData() throws InvalidParameterException, InternalServerErrorException {
        setUpDataStructures();
        setUpUtilClasses();
    }

    private void setUpUtilClasses() throws InvalidParameterException, InternalServerErrorException {
        setUpFinancePlan();
        setUpResourceItemFactory();
        setUpDatabaseManager();
        setUpInvoiceBuilder();
    }
    
    private void setUpErrorResourceItemFactory() throws InvalidParameterException, InternalServerErrorException {
        setUpFinancePlan();

        this.resourceItemFactory = Mockito.mock(RecordUtils.class);
        Mockito.when(this.resourceItemFactory.getItemFromRecord(record1)).thenThrow(new InvalidParameterException());
        Mockito.when(this.resourceItemFactory.getTimeFromRecord(record1, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_1_TIME);
        Mockito.when(this.resourceItemFactory.getItemFromRecord(record2)).thenReturn(item2);
        Mockito.when(this.resourceItemFactory.getTimeFromRecord(record2, invoiceStartTime, invoiceEndTime)).thenReturn(ITEM_2_TIME);
        
        setUpDatabaseManager();
        setUpInvoiceBuilder();
    }
    
    private void setUpErrorFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        this.financePlan = Mockito.mock(FinancePolicy.class);
        Mockito.when(this.financePlan.getItemFinancialValue(item1, state1)).thenThrow(new InvalidParameterException());
        Mockito.when(this.financePlan.getItemFinancialValue(item2, state2)).thenReturn(ITEM_2_VALUE);

        setUpResourceItemFactory();
        setUpDatabaseManager();
        setUpInvoiceBuilder();
    }
    
    private void setUpInvoiceBuilder() {
        this.invoiceBuilder = Mockito.mock(InvoiceBuilder.class);
        Mockito.when(this.invoiceBuilder.buildInvoice()).thenReturn(invoiceToAdd);
    }

    private void setUpDatabaseManager() throws InvalidParameterException, InternalServerErrorException {
        Invoice invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getState()).thenReturn(InvoiceState.WAITING);
        Mockito.when(invoice1.toString()).thenReturn(INVOICE_1_JSON_REPR);
        Invoice invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getState()).thenReturn(InvoiceState.PAID);
        Mockito.when(invoice2.toString()).thenReturn(INVOICE_2_JSON_REPR);

        Invoice invoice3 = Mockito.mock(Invoice.class);
        Mockito.when(invoice3.getState()).thenReturn(InvoiceState.PAID);
        Mockito.when(invoice3.toString()).thenReturn(INVOICE_3_JSON_REPR);
        Invoice invoice4 = Mockito.mock(Invoice.class);
        Mockito.when(invoice4.getState()).thenReturn(InvoiceState.DEFAULTING);
        Mockito.when(invoice4.toString()).thenReturn(INVOICE_4_JSON_REPR);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        Mockito.when(user1.getInvoices()).thenReturn(Arrays.asList(new Invoice[] {invoice1, invoice2}));
        Mockito.when(user2.getInvoices()).thenReturn(Arrays.asList(new Invoice[] {invoice3, invoice4}));
        Mockito.when(user3.getInvoices()).thenReturn(Arrays.asList(new Invoice[] {}));
        
        Mockito.when(this.objectHolder.getUserById(USER_ID_1, PROVIDER_ID_1)).thenReturn(user1);
        Mockito.when(this.objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2)).thenReturn(user2);
        Mockito.when(this.objectHolder.getUserById(USER_ID_3, PROVIDER_ID_3)).thenReturn(user3);
    }

    private void setUpResourceItemFactory() throws InvalidParameterException {
        this.resourceItemFactory = Mockito.mock(RecordUtils.class);
        Mockito.when(this.resourceItemFactory.getItemFromRecord(record1)).thenReturn(item1);
        Mockito.when(this.resourceItemFactory.getItemFromRecord(record2)).thenReturn(item2);
        
        this.timePerStateItem1 = new HashMap<OrderState, Double>();
        timePerStateItem1.put(state1, ITEM_1_TIME_ON_STATE_1);
        timePerStateItem1.put(state2, ITEM_1_TIME_ON_STATE_2);
        
        this.timePerStateItem2 = new HashMap<OrderState, Double>();
        timePerStateItem2.put(state1, ITEM_2_TIME_ON_STATE_1);
        timePerStateItem2.put(state2, ITEM_2_TIME_ON_STATE_2);
        
        Mockito.when(this.resourceItemFactory.getTimeFromRecordPerState(record1, invoiceStartTime, invoiceEndTime)).thenReturn(timePerStateItem1);
        Mockito.when(this.resourceItemFactory.getTimeFromRecordPerState(record2, invoiceStartTime, invoiceEndTime)).thenReturn(timePerStateItem2);
    }

    private void setUpFinancePlan() throws InvalidParameterException {
        this.financePlan = Mockito.mock(FinancePolicy.class);
        Mockito.when(this.financePlan.getItemFinancialValue(item1, state1)).thenReturn(ITEM_1_VALUE);
        Mockito.when(this.financePlan.getItemFinancialValue(item1, state2)).thenReturn(ITEM_1_VALUE);
        Mockito.when(this.financePlan.getItemFinancialValue(item2, state1)).thenReturn(ITEM_2_VALUE);
        Mockito.when(this.financePlan.getItemFinancialValue(item2, state2)).thenReturn(ITEM_2_VALUE);
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
        this.user2 = Mockito.mock(FinanceUser.class);
        this.user3 = Mockito.mock(FinanceUser.class);
        
        this.invoiceToAdd = Mockito.mock(Invoice.class);
    }
}
