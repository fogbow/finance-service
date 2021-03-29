package cloud.fogbow.fs.core.plugins.payment.postpaid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;

public class InvoiceBuilder {

	private Map<ResourceItem, Double> items;
	private String userId;
	private String providerId;
	private Double invoiceTotal;
	
	public InvoiceBuilder(String userId, String providerId) {
		this.userId = userId;
		this.providerId = providerId;
		this.items = new HashMap<ResourceItem, Double>();
		invoiceTotal = 0.0;
	}

	// TODO test
	public void addItem(ResourceItem resourceItem, Double valueToPayPerTimeUnit, Double timeUsed) {
		Double itemValue = valueToPayPerTimeUnit * timeUsed;
		items.put(resourceItem, itemValue);
		invoiceTotal += itemValue;
	}

	// TODO test
	public Invoice buildInvoice() {
		return new Invoice(UUID.randomUUID().toString(), userId, providerId, InvoiceState.WAITING, items, invoiceTotal);
	}
}
