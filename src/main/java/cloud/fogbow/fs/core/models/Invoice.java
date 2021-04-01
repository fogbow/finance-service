package cloud.fogbow.fs.core.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import cloud.fogbow.fs.core.plugins.payment.ResourceItem;

public class Invoice {

	private String invoiceId;
	private String userId;
	private String providerId;
	private InvoiceState state;
	private Map<ResourceItem, Double> invoiceItems;
	private Double invoiceTotal;
	
	public Invoice(String invoiceId, String userId, String providerId, 
			InvoiceState state, Map<ResourceItem, Double> invoiceItems, 
			Double invoiceTotal) {
		this.invoiceId = invoiceId;
		this.userId = userId;
		this.providerId = providerId;
		this.state = state;
		this.invoiceItems = invoiceItems;
		this.invoiceTotal = invoiceTotal;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Map<ResourceItem, Double> getInvoiceItems() {
		return invoiceItems;
	}

	public void setInvoiceItems(Map<ResourceItem, Double> invoiceItems) {
		this.invoiceItems = invoiceItems;
	}

	public Double getInvoiceTotal() {
		return invoiceTotal;
	}

	public void setInvoiceTotal(Double invoiceTotal) {
		this.invoiceTotal = invoiceTotal;
	}

	public InvoiceState getState() {
		return state;
	}
	
	public void setState(InvoiceState state) {
		this.state = state;
	}
	
	public String jsonRepr() {
		return new Gson().toJson(this);
	}
	
	@Override
	public String toString() {
		String invoiceItemsString = "{";
		List<String> invoiceItemsStringList = new ArrayList<String>();
		
		for (ResourceItem item : invoiceItems.keySet()) {
			String itemString = item.toString();
			String valueString = String.valueOf(invoiceItems.get(item));
			String itemValuePairString = itemString + ":" + valueString;
			invoiceItemsStringList.add(itemValuePairString);
		}
		
		invoiceItemsString += String.join(",", invoiceItemsStringList);
		invoiceItemsString += "}";
		
		return "{\"id\":" + invoiceId + ", \"userId\":" + userId + ", \"providerId\":" + providerId + ", \"state\":"
				+ state + ", \"invoiceItems\":" + invoiceItemsString + ", \"invoiceTotal\":" + invoiceTotal + "}";
	}
}
