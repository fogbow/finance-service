package cloud.fogbow.fs.core.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.data.util.Pair;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.ras.core.models.orders.OrderState;

@Entity
@Table(name = "invoice_table")
public class Invoice {
    private static final String INVOICE_ID_COLUMN_NAME = "invoice_id";
    private static final String USER_ID_COLUMN_NAME = "user_id";
    private static final String PROVIDER_ID_COLUMN_NAME = "provider_id";
    private static final String INVOICE_STATE_COLUMN_NAME = "state";
    private static final String INVOICE_ITEMS_COLUMN_NAME = "invoice_items";
    private static final String INVOICE_TOTAL_COLUMN_NAME = "invoice_total";
    private static final String INVOICE_START_TIME_COLUMN_NAME = "start_time";
    private static final String INVOICE_END_TIME_COLUMN_NAME = "end_time";

    @Column(name = INVOICE_ID_COLUMN_NAME)
    @Id
	private String invoiceId;
    
    @Column(name = USER_ID_COLUMN_NAME)
	private String userId;
    
    @Column(name = PROVIDER_ID_COLUMN_NAME)
	private String providerId;
    
    @Column(name = INVOICE_STATE_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
	private InvoiceState state;

    @Column(name = INVOICE_ITEMS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<InvoiceItem> invoiceItems;
    
    @Column(name = INVOICE_TOTAL_COLUMN_NAME)
	private Double invoiceTotal;
	
    @Column(name = INVOICE_START_TIME_COLUMN_NAME)
    private Long startTime;
    
    @Column(name = INVOICE_END_TIME_COLUMN_NAME)
    private Long endTime;
    
    public Invoice() {
        
    }
    
	public Invoice(String invoiceId, String userId, String providerId, 
			Long startTime, Long endTime, Map<Pair<ResourceItem, OrderState>, Double> items, 
			Double invoiceTotal) {
		this.invoiceId = invoiceId;
		this.userId = userId;
		this.providerId = providerId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.state = InvoiceState.WAITING;
		this.invoiceTotal = invoiceTotal;
		
		this.invoiceItems = new ArrayList<InvoiceItem>();
		
		for (Pair<ResourceItem, OrderState> item : items.keySet()) {
		    invoiceItems.add(new InvoiceItem(item.getFirst(), item.getSecond(), items.get(item)));
		}
	}

	public List<InvoiceItem> getInvoiceItemsList() {
        return invoiceItems;
    }

    public void setInvoiceItemsList(List<InvoiceItem> invoiceItemsList) {
        this.invoiceItems = invoiceItemsList;
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

	public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndtime() {
        return endTime;
    }

    public void setEndtime(Long endtime) {
        this.endTime = endtime;
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
	
	public void setState(InvoiceState state) throws InvalidParameterException {
	    switch (state) {
	        case PAID: setAsPaid(); break;
	        case DEFAULTING: setAsDefaulting(); break;
	        default: throw new InvalidParameterException(
                    String.format(Messages.Exception.CANNOT_CHANGE_INVOICE_STATE, 
                            this.state.getValue(), state.getValue()));
	    }
	}
	
	private void setAsPaid() throws InvalidParameterException {
	    if (this.state.equals(InvoiceState.WAITING) || 
	            this.state.equals(InvoiceState.DEFAULTING)) {
	        this.state = InvoiceState.PAID;
	    } else {
	        throw new InvalidParameterException(
	                String.format(Messages.Exception.CANNOT_CHANGE_INVOICE_STATE, 
	                        this.state.getValue(), InvoiceState.PAID.getValue()));
	    }
	}
	
	private void setAsDefaulting() throws InvalidParameterException {
	    if (this.state.equals(InvoiceState.WAITING)) {
	        this.state = InvoiceState.DEFAULTING;
	    } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.CANNOT_CHANGE_INVOICE_STATE, 
                            this.state.getValue(), InvoiceState.DEFAULTING.getValue()));
	    }
	}

	@Override
	public String toString() {
		List<String> invoiceItemsStringList = generateInvoiceItemsString();

		String invoiceItemsString = String.format("{%s}", String.join(",", invoiceItemsStringList));
		String invoiceTotalString = String.format("%.3f", invoiceTotal);
		
		StringBuilder invoiceStringBuilder = new StringBuilder();
		
		invoiceStringBuilder.
		append("{\"id\":\"").append(invoiceId).
		append("\", \"userId\":\"").append(userId).
		append("\", \"providerId\":\"").append(providerId).
		append("\", \"state\":\"").append(state).
		append("\", \"invoiceItems\":").append(invoiceItemsString).
		append(", \"invoiceTotal\":").append(invoiceTotalString).
		append(", \"startTime\":").append(startTime).
		append(", \"endTime\":").append(endTime).
		append("}");
		
		return invoiceStringBuilder.toString();
	}

    private List<String> generateInvoiceItemsString() {
        List<String> invoiceItemsStringList = new ArrayList<String>();
		
		for (InvoiceItem invoiceItem : invoiceItems) {
            String itemString = invoiceItem.getItem().toString();
            String orderStateString = invoiceItem.getOrderState().getValue();
            String valueString = String.format("%.3f", invoiceItem.getValue());
            String itemValuePairString = itemString + "-" + orderStateString + ":" + valueString;
            invoiceItemsStringList.add(itemValuePairString);
		}
		
        return invoiceItemsStringList;
    }
}
