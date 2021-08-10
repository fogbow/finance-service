package cloud.fogbow.fs.core.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import cloud.fogbow.ras.core.models.orders.OrderState;

@Entity
@Table(name = "invoice_items_table")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, unique = true)
    private Long id;
    
    @OneToOne(cascade={CascadeType.ALL})
    private ResourceItem item;
    
    @Column
    private Double value;
    
    @Column
    private OrderState orderState;
    
    public InvoiceItem() {
        
    }
    
    public InvoiceItem(ResourceItem item, Double value) {
        this.item = item;
        this.value = value;
    }
    
    public InvoiceItem(ResourceItem item, OrderState state, Double value) {
    	this.item = item;
    	this.setOrderState(state);
        this.value = value;
	}

	public ResourceItem getItem() {
        return item;
    }

    public void setItem(ResourceItem item) {
        this.item = item;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

	public OrderState getOrderState() {
		return orderState;
	}

	public void setOrderState(OrderState orderState) {
		this.orderState = orderState;
	}
}
