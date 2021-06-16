package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InvalidParameterException;

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
    
    private Map<ResourceItem, Double> invoiceItems;
    private Double invoiceTotal;
    private Set<ResourceItem> itemsSet;

    @Test
    public void testToStringWithNoInvoiceItems() {
        invoiceItems = new HashMap<ResourceItem, Double>();
        invoiceTotal = 0.0;
        
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        
        String expected = String.format("{\"id\":\"%s\", \"userId\":\"%s\", \"providerId\":\"%s\", \"state\":" +
        "\"WAITING\", \"invoiceItems\":{}, \"invoiceTotal\":%.3f, \"startTime\":1, \"endTime\":10}", INVOICE_ID, 
                USER_ID, PROVIDER_ID, invoiceTotal);
        
        assertEquals(expected, invoice.toString());
    }

    @Test
    public void testToString() {
        setUpInvoiceData();
        
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        
        String expected = String.format("{\"id\":\"%s\", \"userId\":\"%s\", \"providerId\":\"%s\", \"state\":\"WAITING\"," + 
        " \"invoiceItems\":{%s:%.3f,%s:%.3f}, \"invoiceTotal\":%.3f, \"startTime\":1, \"endTime\":10}", INVOICE_ID, 
                USER_ID, PROVIDER_ID, RESOURCE_ITEM_1_STRING, RESOURCE_ITEM_1_VALUE, 
                RESOURCE_ITEM_2_STRING, RESOURCE_ITEM_2_VALUE, invoiceTotal);
        
        assertEquals(expected, invoice.toString());
    }
    
    @Test
    public void testSetPaid() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        assertEquals(InvoiceState.PAID, invoice.getState());
    }
    
    @Test
    public void testSetDefaulting() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        assertEquals(InvoiceState.DEFAULTING, invoice.getState());
    }
    
    @Test
    public void testSetDefaultingAndThenPaid() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        assertEquals(InvoiceState.DEFAULTING, invoice.getState());
        
        invoice.setState(InvoiceState.PAID);
        assertEquals(InvoiceState.PAID, invoice.getState());
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStateDefaultingAndThenWaiting() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.DEFAULTING);
        invoice.setState(InvoiceState.WAITING);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStatePaidAndThenWaiting() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        invoice.setState(InvoiceState.WAITING);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testCannotSetStatePaidAndThenDefaulting() throws InvalidParameterException {
        setUpInvoiceData();
        
        Invoice invoice = new Invoice(INVOICE_ID, USER_ID, PROVIDER_ID, 
                START_TIME, END_TIME, invoiceItems, invoiceTotal);
        
        invoice.setState(InvoiceState.PAID);
        invoice.setState(InvoiceState.DEFAULTING);
    }

    private void setUpInvoiceData() {
        ResourceItem resourceItem1 = Mockito.mock(ResourceItem.class);
        Mockito.when(resourceItem1.toString()).thenReturn(RESOURCE_ITEM_1_STRING);
        ResourceItem resourceItem2 = Mockito.mock(ResourceItem.class);
        Mockito.when(resourceItem2.toString()).thenReturn(RESOURCE_ITEM_2_STRING);
        
        // This code assures a certain order of resource items is used in the string generation
        List<ResourceItem> resourceItemsList = 
                new ArrayList<ResourceItem>(Arrays.asList(resourceItem1, resourceItem2));
        Iterator<ResourceItem> iterator = new TestIterator<ResourceItem>(resourceItemsList);
        
        itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        invoiceItems = Mockito.mock(HashMap.class);
        Mockito.when(invoiceItems.keySet()).thenReturn(itemsSet);
        Mockito.when(invoiceItems.get(resourceItem1)).thenReturn(RESOURCE_ITEM_1_VALUE);
        Mockito.when(invoiceItems.get(resourceItem2)).thenReturn(RESOURCE_ITEM_2_VALUE);

        invoiceTotal = RESOURCE_ITEM_1_VALUE + RESOURCE_ITEM_2_VALUE;
    }
    
    private class TestIterator<T> implements Iterator<T> {

        private int currentIndex;
        private List<T> objs;
          
        public TestIterator(List<T> objs) {
            this.currentIndex = 0;
            this.objs = objs;
        }
        
        @Override
        public boolean hasNext() {
            return currentIndex < this.objs.size();
        }

        @Override
        public T next() {
            return objs.get(currentIndex++);
        }
    }
}
