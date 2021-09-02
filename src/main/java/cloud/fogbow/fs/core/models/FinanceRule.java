package cloud.fogbow.fs.core.models;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "finance_rules_table")
public class FinanceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, unique = true)
    private Long id;

	@Embedded
	private FinanceRuleContext financeRuleContext;
	
	@Embedded
	private FinanceRuleValue financeRuleValue;

    public FinanceRule() {
        
    }

    public FinanceRule(FinanceRuleContext financeRuleContext, FinanceRuleValue financeRuleValue) {
        this.financeRuleContext = financeRuleContext;
        this.financeRuleValue = financeRuleValue;
    }

    public FinanceRuleContext getFinanceRuleContext() {
        return financeRuleContext;
    }

    public void setFinanceRuleContext(FinanceRuleContext financeRuleContext) {
        this.financeRuleContext = financeRuleContext;
    }

    public FinanceRuleValue getFinanceRuleValue() {
        return financeRuleValue;
    }

    public void setFinanceRuleValue(FinanceRuleValue financeRuleValue) {
        this.financeRuleValue = financeRuleValue;
    }
}
