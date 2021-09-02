package cloud.fogbow.fs.core.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;

import cloud.fogbow.ras.core.models.orders.OrderState;

@Embeddable
public class FinanceRuleContext {
    private static final String ORDER_STATE_COLUMN_NAME = "order_state";
    
    @Column(name = ORDER_STATE_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    private OrderState orderState;
    
    @OneToOne(cascade={CascadeType.ALL})
    private ResourceItem item;

    public FinanceRuleContext() {
        
    }
    
    public FinanceRuleContext(ResourceItem item, OrderState orderState) {
        this.orderState = orderState;
        this.item = item;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }

    public ResourceItem getItem() {
        return item;
    }

    public void setItem(ResourceItem item) {
        this.item = item;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((item == null) ? 0 : item.hashCode());
        result = prime * result + ((orderState == null) ? 0 : orderState.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FinanceRuleContext other = (FinanceRuleContext) obj;
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        if (orderState != other.orderState)
            return false;
        return true;
    }
}
