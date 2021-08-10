package cloud.fogbow.fs.core.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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

import org.springframework.data.util.Pair;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.ras.core.models.orders.OrderState;

@Entity
@Table(name = "finance_policy_table")
public class FinancePolicy {

	public static final String PLAN_FIELDS_SEPARATOR = "-";
	public static final String ITEM_FIELDS_SEPARATOR = ",";
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

    @Column(name = FINANCE_PLAN_ID_COLUMN_NAME)
	@Id
	private String name;
	
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
	private Map<Pair<ResourceItem, OrderState>, Double> policy;
	
	public FinancePolicy() {
	    
	}
	
	// This method is used to repopulate the FinancePlan internal map
	// with the data loaded from the database.
    @PostLoad
    private void startUp() {
        policy = getPlanFromDatabaseItems(items);
    }
    
    @VisibleForTesting
    Map<Pair<ResourceItem, OrderState>, Double> getPlanFromDatabaseItems(List<FinanceRule> databaseItems) {
        Map<Pair<ResourceItem, OrderState>, Double> plan = new HashMap<Pair<ResourceItem, OrderState>, Double>();
        
        for (FinanceRule item : databaseItems) {
            plan.put(Pair.of(item.getItem(), item.getOrderState()), item.getValue());
        }
        
        return plan;
    }
	
    public FinancePolicy(String planName, String planPath) throws InvalidParameterException {
    	Map<String, String> planInfo = getPlanFromFile(planPath);
    	Map<Pair<ResourceItem, OrderState>, Double> plan = validatePlanInfo(planInfo);
    	this.items = getDatabaseItems(plan);
    	
		this.name = planName;
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
    
	private List<FinanceRule> getDatabaseItems(Map<Pair<ResourceItem, OrderState>, Double> inMemoryPlan) {
	    List<FinanceRule> databasePlanItems = new ArrayList<FinanceRule>();
	    
	    for (Pair<ResourceItem, OrderState> item : inMemoryPlan.keySet()) {
	        databasePlanItems.add(new FinanceRule(item.getFirst(), item.getSecond(), inMemoryPlan.get(item)));
	    }
	    
        return databasePlanItems;
    }
	
    public FinancePolicy(String planName, Map<String, String> planInfo) throws InvalidParameterException {
    	Map<Pair<ResourceItem, OrderState>, Double> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.policy = plan;
		this.items = getDatabaseItems(plan);
	}
    
    FinancePolicy(Map<Pair<ResourceItem, OrderState>, Double> plan) {
        this.policy = plan;
    }
    
	public String getName() {
		return name;
	}

	private Map<Pair<ResourceItem, OrderState>, Double> validatePlanInfo(Map<String, String> planInfo) throws InvalidParameterException {
		Map<Pair<ResourceItem, OrderState>, Double> plan = new HashMap<Pair<ResourceItem, OrderState>, Double>();

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

	private void extractComputeItem(Map<Pair<ResourceItem, OrderState>, Double> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateComputeFieldsLength(fields);
			
			int vCPU = Integer.parseInt(fields[COMPUTE_VCPU_FIELD_INDEX]);
			int ram = Integer.parseInt(fields[COMPUTE_RAM_FIELD_INDEX]);
			double value = Double.parseDouble(fields[COMPUTE_VALUE_FIELD_INDEX]);
			ResourceItem newItem = new ComputeItem(vCPU, ram);
			
			validateItemValue(value);
			
			OrderState state = OrderState.fromValue(fields[ORDER_STATE_FIELD_INDEX]);
			
			plan.put(Pair.of(newItem, state), value);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_COMPUTE_ITEM_FIELD);
		}
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
	
	private void extractVolumeItem(Map<Pair<ResourceItem, OrderState>, Double> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateVolumeFieldsLength(fields);
			
			int size = Integer.parseInt(fields[VOLUME_SIZE_FIELD_INDEX]);
			double value = Double.parseDouble(fields[VOLUME_VALUE_FIELD_INDEX]);
			ResourceItem newItem = new VolumeItem(size);
			
			validateItemValue(value);
			
			OrderState state = OrderState.fromValue(fields[ORDER_STATE_FIELD_INDEX]);

			plan.put(Pair.of(newItem, state), value);
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
        
        for (Pair<ResourceItem, OrderState> item : this.policy.keySet()) {
            financePlanItemsStrings.add(
                    String.format("%s:[%s,%s,%s]", String.valueOf(ruleIndex), 
                    		item.getFirst().toString(),
                    		item.getSecond().getValue(),
                    		String.valueOf(this.policy.get(item))));
            ruleIndex++;
        }
        
        return String.format("{%s}", String.join(",", financePlanItemsStrings));
    }

	private Map<String, String> generateRulesRepr() {
        Map<String, String> rulesRepr = new HashMap<String, String>();

        for (Pair<ResourceItem, OrderState> item : this.policy.keySet()) {
        	String rulesReprKey = String.format("%s-%s", item.getFirst().toString(),
        			item.getSecond().getValue());
            rulesRepr.put(rulesReprKey, String.valueOf(this.policy.get(item)));
        }
        
        return rulesRepr;
    }

	public void update(Map<String, String> planInfo) throws InvalidParameterException {
		Map<Pair<ResourceItem, OrderState>, Double> newPlan = validatePlanInfo(planInfo);
		this.policy = newPlan;
		this.items = getDatabaseItems(newPlan);
	}

	public Double getItemFinancialValue(ResourceItem resourceItem, OrderState orderState) throws InvalidParameterException {
		if (policy.containsKey(Pair.of(resourceItem, orderState))) { 
			return policy.get(Pair.of(resourceItem, orderState));	
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM, 
				resourceItem.toString(), orderState.getValue()));
	}
}
