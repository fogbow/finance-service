package cloud.fogbow.fs.core.models;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.TestUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class InvoiceTest {

    private static final String INVOICE_ID = "invoiceId";
    private static final String USER_ID = "userId";
    private static final String PROVIDER_ID = "providerId";
    private static final String RESOURCE_ITEM_1_STRING = "resourceItem1";
    private static final Double RESOURCE_ITEM_1_VALUE = 5.1;
    private static final String RESOURCE_ITEM_2_STRING = "resourceItem2";
    private static final Double RESOURCE_ITEM_2_VALUE = 10.3;
    private static final Long START_TIME = 1L;
    private static final Long END_TIME = 10L;
	private static final OrderState STATE_1 = OrderState.FULFILLED;
	private static final OrderState STATE_2 = OrderState.PAUSED;
    
    private Map<Pair<ResourceItem, OrderState>, Double> invoiceItems;
    private Double invoiceTotal;
    private Set<Pair<ResourceItem, OrderState>> itemsSet;

    @Before
    public void setUp() {
        setUpInvoiceData();
    }
    
    @Test
    public void testToStringWithNoInvoiceItems() {
        invoiceItems = new HashMap<Pair<ResourceItem, OrderState>, Double>();
        invoiceTotal = 0.0;
        
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        
        String expected = String.format("{\"id\":\"%s\", \"userId\":\"%s\", \"providerId\":\"%s\", \"state\":" +
        "\"WAITING\", \"invoiceItems\":{}, \"invoiceTotal\":%.3f, \"startTime\":1, \"endTime\":10}", INVOICE_ID, 
                USER_ID, PROVIDER_ID, invoiceTotal);
        
        assertEquals(expected, invoice.toString());
    }

    @Test
    public void testToString() {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        
        String expected = String.format("{\"id\":\"%s\", \"userId\":\"%s\", \"providerId\":\"%s\", \"state\":\"WAITING\"," + 
        " \"invoiceItems\":{%s-%s:%.3f,%s-%s:%.3f}, \"invoiceTotal\":%.3f, \"startTime\":1, \"endTime\":10}", INVOICE_ID, 
                USER_ID, PROVIDER_ID, RESOURCE_ITEM_1_STRING, STATE_1.getValue(), RESOURCE_ITEM_1_VALUE, 
                RESOURCE_ITEM_2_STRING, STATE_2.getValue(), RESOURCE_ITEM_2_VALUE, invoiceTotal);
        
        assertEquals(expected, invoice.toString());
    }
    
    @Test
    public void testSetPaid() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        assertEquals(InvoiceState.PAID, invoice.getState());
    }
    
    @Test
    public void testSetDefaulting() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        assertEquals(InvoiceState.DEFAULTING, invoice.getState());
    }
    
    @Test
    public void testSetDefaultingAndThenPaid() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        assertEquals(InvoiceState.DEFAULTING, invoice.getState());
        
        invoice.setState(InvoiceState.PAID);
        assertEquals(InvoiceState.PAID, invoice.getState());
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStateDefaultingAndThenWaiting() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        invoice.setState(InvoiceState.WAITING);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStatePaidAndThenWaiting() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        invoice.setState(InvoiceState.WAITING);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStatePaidAndThenDefaulting() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        invoice.setState(InvoiceState.DEFAULTING);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStatePaidAlreadyPaidInvoice() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        try {
            invoice.setState(InvoiceState.PAID);
        } catch (InvalidParameterException e) {
            Assert.fail("Unexpected exception.");
        }
        
        invoice.setState(InvoiceState.PAID);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStateDefaultingAlreadyDefaultingInvoice() throws InvalidParameterException {
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        try {
            invoice.setState(InvoiceState.DEFAULTING);
        } catch (InvalidParameterException e) {
            Assert.fail("Unexpected exception.");
        }
        
        invoice.setState(InvoiceState.DEFAULTING);
    }

    private void setUpInvoiceData() {
        ResourceItem resourceItem1 = Mockito.mock(ResourceItem.class);
        Mockito.when(resourceItem1.toString()).thenReturn(RESOURCE_ITEM_1_STRING);
        ResourceItem resourceItem2 = Mockito.mock(ResourceItem.class);
        Mockito.when(resourceItem2.toString()).thenReturn(RESOURCE_ITEM_2_STRING);
        
        // This code assures a certain order of resource items is used in the string generation
        Iterator<Pair<ResourceItem, OrderState>> iterator = 
        		new TestUtils().getIterator(Arrays.asList(
        				Pair.of(resourceItem1, STATE_1),
        				Pair.of(resourceItem2, STATE_2)));
        
        itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        invoiceItems = Mockito.mock(HashMap.class);
        Mockito.when(invoiceItems.keySet()).thenReturn(itemsSet);
        Mockito.when(invoiceItems.get(Pair.of(resourceItem1, STATE_1))).thenReturn(RESOURCE_ITEM_1_VALUE);
        Mockito.when(invoiceItems.get(Pair.of(resourceItem2, STATE_2))).thenReturn(RESOURCE_ITEM_2_VALUE);

        invoiceTotal = RESOURCE_ITEM_1_VALUE + RESOURCE_ITEM_2_VALUE;
    }
}
