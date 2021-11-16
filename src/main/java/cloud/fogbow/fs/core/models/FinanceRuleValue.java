package cloud.fogbow.fs.core.models;

import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class FinanceRuleValue {
    private static final String TIME_UNIT_COLUMN_NAME = "time_unit";

    @Column
    private Double value;
    
    @Column(name = TIME_UNIT_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    private TimeUnit timeUnit;

    public FinanceRuleValue() {
        
    }
    
    public FinanceRuleValue(Double value, TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((timeUnit == null) ? 0 : timeUnit.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        FinanceRuleValue other = (FinanceRuleValue) obj;
        if (timeUnit != other.timeUnit)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
