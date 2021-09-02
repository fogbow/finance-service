package cloud.fogbow.fs.core.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.ras.core.models.orders.OrderState;

@Entity
@Table(name = "finance_policy_table")
public class FinancePolicy {

    public static final String PLAN_FIELDS_SEPARATOR = "-";
	public static final String ITEM_FIELDS_SEPARATOR = ",";
	public static final String VALUE_TIME_UNIT_SEPARATOR = "/";
	
	public static final String MILLISECONDS = "ms";
	public static final String SECONDS = "s";
	public static final String MINUTES = "m";
	public static final String HOURS = "h";

	public static final int RESOURCE_TYPE_FIELD_INDEX = 0;
	public static final int ORDER_STATE_FIELD_INDEX = 1;
	public static final String COMPUTE_RESOURCE_TYPE = "compute";
	public static final int COMPUTE_VCPU_FIELD_INDEX = 2;
	public static final int COMPUTE_RAM_FIELD_INDEX = 3;
	public static final int COMPUTE_VALUE_FIELD_INDEX = 4;
	public static final String VOLUME_RESOURCE_TYPE = "volume";
	public static final int VOLUME_SIZE_FIELD_INDEX = 2;
	public static final int VOLUME_VALUE_FIELD_INDEX = 3;
	
	private static final String FINANCE_PLAN_ID_COLUMN_NAME = "finance_plan_id";
    private static final String FINANCE_PLAN_ITEMS_COLUMN_NAME = "finance_plan_items";
    private static final String DEFAULT_RESOURCE_VALUE_COLUMN_NAME = "default_resource_value_column_name";

    @Column(name = FINANCE_PLAN_ID_COLUMN_NAME)
	@Id
	private String name;
    
    @Column(name = DEFAULT_RESOURCE_VALUE_COLUMN_NAME)
    private Double defaultResourceValue;
	
    // Persisting a Map with complex keys tends
    // to lead to some problems. Thus, we keep the
    // plan data stored as a list of entries.
    @Column(name = FINANCE_PLAN_ITEMS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
    private List<FinanceRule> items;
    
    // Since accessing a resource item value in a Map 
    // is expected to be faster than accessing the value
    // in a List, we keep this copy of the plan data for 
    // access operations.
	@Transient
	private Map<FinanceRuleContext, FinanceRuleValue> policy;
	
	public FinancePolicy() {
	    
	}
	
	// This method is used to repopulate the FinancePolicy internal map
	// with the data loaded from the database.
    @PostLoad
    private void startUp() {
        policy = getPolicyFromDatabaseItems(items);
    }
    
    @VisibleForTesting
    Map<FinanceRuleContext, FinanceRuleValue> getPolicyFromDatabaseItems(List<FinanceRule> databaseItems) {
        Map<FinanceRuleContext, FinanceRuleValue> policy = new HashMap<FinanceRuleContext, FinanceRuleValue>();
        
        for (FinanceRule item : databaseItems) {
            policy.put(item.getFinanceRuleContext(), item.getFinanceRuleValue());
        }
        
        return policy;
    }
	
    public FinancePolicy(String planName, Double defaultResourceValue, String planPath) throws InvalidParameterException {
    	Map<String, String> planInfo = getPlanFromFile(planPath);
    	Map<FinanceRuleContext, FinanceRuleValue> plan = validatePlanInfo(planInfo);
    	this.items = getDatabaseItems(plan);
    	
		this.name = planName;
		this.defaultResourceValue = defaultResourceValue;
		this.policy = plan;
    }
    
    private Map<String, String> getPlanFromFile(String planPath) throws InvalidParameterException {
        try {
            Map<String, String> planInfo = new HashMap<String, String>();
            File file = new File(planPath);
            Scanner input = new Scanner(file);
            
            while (input.hasNextLine()) {
                String nextLine = input.nextLine().trim();
                if (!nextLine.isEmpty()) {
                    String[] planFields = nextLine.split(PLAN_FIELDS_SEPARATOR);
                    String itemName = planFields[0];
                    String itemInfo = planFields[1]; 
                    
                    planInfo.put(itemName, itemInfo);
                }
            }
            
            input.close();
            
            return planInfo;
        } catch (FileNotFoundException e) {
            throw new InvalidParameterException(String.format(
                    Messages.Exception.UNABLE_TO_READ_CONFIGURATION_FILE_S, planPath));
        }
    }
    
	private List<FinanceRule> getDatabaseItems(Map<FinanceRuleContext, FinanceRuleValue> inMemoryPlan) {
	    List<FinanceRule> databasePlanItems = new ArrayList<FinanceRule>();
	    
	    for (FinanceRuleContext context : inMemoryPlan.keySet()) {
	        FinanceRuleValue value = inMemoryPlan.get(context);
	        
	        databasePlanItems.add(new FinanceRule(context, value));
	    }
	    
        return databasePlanItems;
    }
	
    public FinancePolicy(String planName, Double defaultResourceValue, Map<String, String> planInfo) throws InvalidParameterException {
        Map<FinanceRuleContext, FinanceRuleValue> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.defaultResourceValue = defaultResourceValue;
		this.policy = plan;
		this.items = getDatabaseItems(plan);
	}
    
    FinancePolicy(Double defaultResourceValue, Map<FinanceRuleContext, FinanceRuleValue> plan) {
        this.defaultResourceValue = defaultResourceValue;
        this.policy = plan;
    }
    
	public String getName() {
		return name;
	}

	private Map<FinanceRuleContext, FinanceRuleValue> validatePlanInfo(Map<String, String> planInfo) throws InvalidParameterException {
	    Map<FinanceRuleContext, FinanceRuleValue> plan = new HashMap<FinanceRuleContext, FinanceRuleValue>();

			for (String itemKey : planInfo.keySet()) {
				String[] fields = planInfo.get(itemKey).split(ITEM_FIELDS_SEPARATOR);
				String resourceType = fields[RESOURCE_TYPE_FIELD_INDEX];

				switch(resourceType) {
					case COMPUTE_RESOURCE_TYPE: extractComputeItem(plan, fields); break;
					case VOLUME_RESOURCE_TYPE: extractVolumeItem(plan, fields); break;
					default: throw new InvalidParameterException(
							String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM_TYPE, resourceType));
				}
			}

		return plan;
	}

	private void extractComputeItem(Map<FinanceRuleContext, FinanceRuleValue> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateComputeFieldsLength(fields);

			// parse context
			int vCPU = Integer.parseInt(fields[COMPUTE_VCPU_FIELD_INDEX]);
			int ram = Integer.parseInt(fields[COMPUTE_RAM_FIELD_INDEX]);
			OrderState state = OrderState.fromValue(fields[ORDER_STATE_FIELD_INDEX]);
			ResourceItem newItem = new ComputeItem(vCPU, ram);
			FinanceRuleContext context = new FinanceRuleContext(newItem, state);
			
			// parse value
			String valueFieldsString = fields[COMPUTE_VALUE_FIELD_INDEX];
			FinanceRuleValue ruleValue = getFinanceRuleValueFromString(valueFieldsString);
			
			plan.put(context, ruleValue);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_COMPUTE_ITEM_FIELD);
		}
	}

    private FinanceRuleValue getFinanceRuleValueFromString(String valueFieldsString) throws InvalidParameterException {
        String[] valueFields = valueFieldsString.split(VALUE_TIME_UNIT_SEPARATOR);
        validateValueFieldsLength(valueFields);

        // parse value
        String valueString = valueFields[0];
        Double baseValue = Double.parseDouble(valueString);
        validateItemValue(baseValue);
        
        // parse time unit
        String valueTimeUnitString = valueFields[1];
        TimeUnit valueTimeUnit = getTimeUnitFromString(valueTimeUnitString);
        
        return new FinanceRuleValue(baseValue, valueTimeUnit);
    }

    private TimeUnit getTimeUnitFromString(String valueTimeUnitString) throws InvalidParameterException {
        switch(valueTimeUnitString) {
            case MILLISECONDS: return TimeUnit.MILLISECONDS;
            case SECONDS: return TimeUnit.SECONDS;
            case MINUTES: return TimeUnit.MINUTES;
            case HOURS: return TimeUnit.HOURS;
        }
        
        throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE_ITEM_VALUE_TIME_UNIT);
    }

    private void validateItemValue(double value) throws InvalidParameterException {
		if (value < 0) {
			throw new InvalidParameterException(Messages.Exception.NEGATIVE_RESOURCE_ITEM_VALUE);
		}
	}

	private void validateComputeFieldsLength(String[] fields) throws InvalidParameterException {
		if (fields.length != 5) {
			throw new InvalidParameterException(Messages.Exception.INVALID_NUMBER_OF_COMPUTE_ITEM_FIELDS);
		}
	}

    private void validateValueFieldsLength(String[] valueFields) throws InvalidParameterException {
        if (valueFields.length != 2) {
            throw new InvalidParameterException(Messages.Exception.INVALID_NUMBER_OF_RESOURCE_ITEM_VALUE_FIELDS);
        }
    }
	
	private void extractVolumeItem(Map<FinanceRuleContext, FinanceRuleValue> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateVolumeFieldsLength(fields);

			// parse context
			int size = Integer.parseInt(fields[VOLUME_SIZE_FIELD_INDEX]);
            OrderState state = OrderState.fromValue(fields[ORDER_STATE_FIELD_INDEX]);
            ResourceItem newItem = new VolumeItem(size);
            FinanceRuleContext context = new FinanceRuleContext(newItem, state);
            
			// parse value
            String valueFieldsString = fields[VOLUME_VALUE_FIELD_INDEX];
            FinanceRuleValue ruleValue = getFinanceRuleValueFromString(valueFieldsString);
            
			plan.put(context, ruleValue);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_VOLUME_ITEM_FIELD);
		}
	}

	private void validateVolumeFieldsLength(String[] fields) throws InvalidParameterException {
		if (fields.length != 4) {
			throw new InvalidParameterException(Messages.Exception.INVALID_NUMBER_OF_VOLUME_ITEM_FIELDS);
		}
	}
	
	public Map<String, String> getRulesAsMap() {
	    return generateRulesRepr();
	}
	
    @Override
    public String toString() {
        List<String> financePlanItemsStrings = new ArrayList<String>();
        Integer ruleIndex = 0;
        
        for (FinanceRuleContext context : this.policy.keySet()) {
            ResourceItem item = context.getItem();
            OrderState orderState = context.getOrderState();
            FinanceRuleValue ruleValue = this.policy.get(context);
            TimeUnit timeUnit = ruleValue.getTimeUnit();
            Double value = ruleValue.getValue();
            
            String ruleString = String.format("%s:[%s,%s,%s,%s]", 
                    String.valueOf(ruleIndex), 
                    item.toString(),
                    orderState.getValue(),
                    String.valueOf(value),
                    String.valueOf(timeUnit)); 
            
            financePlanItemsStrings.add(ruleString);
            ruleIndex++;
        }
        
        return String.format("{%s}", String.join(",", financePlanItemsStrings));
    }

	private Map<String, String> generateRulesRepr() {
        Map<String, String> rulesRepr = new HashMap<String, String>();

        for (FinanceRuleContext item : this.policy.keySet()) {
            FinanceRuleValue value = this.policy.get(item);
        	String rulesReprKey = String.format("%s-%s", item.getItem().toString(),
        			String.valueOf(item.getOrderState()));
        	String rulesReprValue = String.format("%s-%s", String.valueOf(value.getValue()),
                    String.valueOf(value.getTimeUnit()));
            rulesRepr.put(rulesReprKey, rulesReprValue);
        }
        
        return rulesRepr;
    }

	public void update(Map<String, String> planInfo) throws InvalidParameterException {
		Map<FinanceRuleContext, FinanceRuleValue> newPlan = validatePlanInfo(planInfo);
		this.policy = newPlan;
		this.items = getDatabaseItems(newPlan);
	}

	public Double getItemFinancialValue(ResourceItem resourceItem, OrderState orderState) throws InvalidParameterException {
	    FinanceRuleContext context = new FinanceRuleContext(resourceItem, orderState);
	    
		if (policy.containsKey(context)) { 
			return policy.get(context).getValue();	
		} else {
		    return this.defaultResourceValue;
		}
	}

    public TimeUnit getItemFinancialTimeUnit(ResourceItem resourceItem, OrderState orderState) {
        FinanceRuleContext context = new FinanceRuleContext(resourceItem, orderState);
        
        if (policy.containsKey(context)) { 
            return policy.get(context).getTimeUnit();  
        } else {
            return TimeUnit.MILLISECONDS;
        }
    }
}
