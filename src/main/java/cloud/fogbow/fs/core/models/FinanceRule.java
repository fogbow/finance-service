package cloud.fogbow.fs.core.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import cloud.fogbow.ras.core.models.orders.OrderState;

@Entity
@Table(name = "finance_rules_table")
public class FinanceRule {
    private static final String ORDER_STATE_COLUMN_NAME = "order_state";

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, unique = true)
    private Long id;

    @Column(name = ORDER_STATE_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    private OrderState orderState;
    
    @OneToOne(cascade={CascadeType.ALL})
    private ResourceItem item;
    
    @Column
    private Double value;
    
    public FinanceRule() {
        
    }
    
    public FinanceRule(ResourceItem item, OrderState orderState, Double value) {
        this.item = item;
        this.orderState = orderState;
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
