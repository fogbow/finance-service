package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.ResourceItem;

public class InvoiceBuilder {
	private Map<ResourceItem, Double> items;
	private String userId;
	private String providerId;
	private Double invoiceTotal;
	private Long startTime;
	private Long endTime;
	
	public InvoiceBuilder() {
		this.items = new HashMap<ResourceItem, Double>();
		invoiceTotal = 0.0;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	
	public void setStartTime(Long startTime) {
	    this.startTime = startTime;
	}
	
	public void setEndTime(Long endTime) {
	    this.endTime = endTime;
	}
	
	public void addItem(ResourceItem resourceItem, Double valueToPayPerTimeUnit, Double timeUsed) {
		Double itemValue = valueToPayPerTimeUnit * timeUsed;
		items.put(resourceItem, itemValue);
		invoiceTotal += itemValue;
	}

	public Invoice buildInvoice() {
		return new Invoice(UUID.randomUUID().toString(), userId, providerId, startTime, endTime, 
		        items, invoiceTotal);
	}

	public void reset() {
		this.userId = null;
		this.providerId = null;
		this.items = new HashMap<ResourceItem, Double>();
		this.invoiceTotal = 0.0;
	}
}
