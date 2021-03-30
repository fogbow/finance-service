package cloud.fogbow.fs.core.plugins.payment.postpaid;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.payment.ComputeItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.VolumeItem;

public class InvoiceBuilderTest {

	private static final String USER_ID_1 = "userId1";
	private static final String PROVIDER_ID_1 = "provider1";
	private static final int VCPU_ITEM_1 = 2;
	private static final int RAM_ITEM_1 = 4;
	private static final Double VALUE_ITEM_1 = 3.0;
	private static final Double TIME_USED_ITEM_1 = 2.0;
	private static final int VCPU_ITEM_2 = 1;
	private static final int RAM_ITEM_2 = 1;
	private static final Double VALUE_ITEM_2 = 0.5;
	private static final Double TIME_USED_ITEM_2 = 0.1;
	private static final int SIZE_ITEM_3 = 130;
	private static final Double VALUE_ITEM_3 = 5.0;
	private static final Double TIME_USED_ITEM_3 = 4.0;

	// test case: When calling the buildInvoice method, 
	// it must create a new Invoice object, considering the
	// items previously added using the method addItem.
	@Test
	public void testBuildInvoice() throws InvalidParameterException {
		ResourceItem resourceItem1 = new ComputeItem(VCPU_ITEM_1, RAM_ITEM_1);
		ResourceItem resourceItem2 = new ComputeItem(VCPU_ITEM_2, RAM_ITEM_2);
		ResourceItem resourceItem3 = new VolumeItem(SIZE_ITEM_3);
		
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder();
		
		invoiceBuilder.setUserId(USER_ID_1);
		invoiceBuilder.setProviderId(PROVIDER_ID_1);
		invoiceBuilder.addItem(resourceItem1, VALUE_ITEM_1, TIME_USED_ITEM_1);
		invoiceBuilder.addItem(resourceItem2, VALUE_ITEM_2, TIME_USED_ITEM_2);
		invoiceBuilder.addItem(resourceItem3, VALUE_ITEM_3, TIME_USED_ITEM_3);
		Invoice invoice = invoiceBuilder.buildInvoice();
		
		assertEquals(USER_ID_1, invoice.getUserId());
		assertEquals(PROVIDER_ID_1, invoice.getProviderId());
		assertEquals(InvoiceState.WAITING, invoice.getState());
		assertEquals(3, invoice.getInvoiceItems().keySet().size());
		assertEquals(new Double(VALUE_ITEM_1*TIME_USED_ITEM_1), 
				invoice.getInvoiceItems().get(resourceItem1));
		assertEquals(new Double(VALUE_ITEM_2*TIME_USED_ITEM_2), 
				invoice.getInvoiceItems().get(resourceItem2));
		assertEquals(new Double(VALUE_ITEM_3*TIME_USED_ITEM_3),
				invoice.getInvoiceItems().get(resourceItem3));
		assertEquals(new Double(VALUE_ITEM_1*TIME_USED_ITEM_1 + VALUE_ITEM_2*TIME_USED_ITEM_2 
				+ VALUE_ITEM_3*TIME_USED_ITEM_3), invoice.getInvoiceTotal());
	}
	
	// test case: When calling the buildInvoice method and
	// no item has been added through the method addItem, 
	// it must create an empty Invoice.
	@Test
	public void testBuildEmptyInvoice() {
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder();
		invoiceBuilder.setUserId(USER_ID_1);
		invoiceBuilder.setProviderId(PROVIDER_ID_1);
		
		Invoice invoice = invoiceBuilder.buildInvoice();
		
		assertEquals(USER_ID_1, invoice.getUserId());
		assertEquals(PROVIDER_ID_1, invoice.getProviderId());
		assertEquals(InvoiceState.WAITING, invoice.getState());
		assertEquals(0, invoice.getInvoiceItems().keySet().size());
		assertEquals(new Double(0.0), invoice.getInvoiceTotal());
	}
}
