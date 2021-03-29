package cloud.fogbow.fs.core.models;

import java.util.HashMap;
import java.util.Map;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.plugins.payment.ComputeItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.VolumeItem;

public class FinancePlan {

	private static final String ITEM_FIELDS_SEPARATOR = ",";
	private static final int RESOURCE_TYPE_FIELD_INDEX = 0;
	private static final String COMPUTE_RESOURCE_TYPE = "compute";
	private static final int COMPUTE_VCPU_FIELD_INDEX = 1;
	private static final int COMPUTE_RAM_FIELD_INDEX = 2;
	private static final int COMPUTE_VALUE_FIELD_INDEX = 3;
	private static final String VOLUME_RESOURCE_TYPE = "volume";
	private static final int VOLUME_SIZE_FIELD_INDEX = 1;
	private static final int VOLUME_VALUE_FIELD_INDEX = 2;
	
	private String name;
	private Map<ResourceItem, Double> plan;
	private Map<String, String> basePlan;
	
	public FinancePlan(String planName, Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.basePlan = planInfo;
		this.plan = plan;
	}

	public String getName() {
		return name;
	}

	// TODO test
	private Map<ResourceItem, Double> validatePlanInfo(Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> plan = new HashMap<ResourceItem, Double>();
		
		for (String itemString : planInfo.keySet()) {
			String[] fields = itemString.split(ITEM_FIELDS_SEPARATOR);
			String resourceType = fields[RESOURCE_TYPE_FIELD_INDEX];
			
			ResourceItem newItem;
			double value;
			
			if (resourceType.equals(COMPUTE_RESOURCE_TYPE)) {
				int vCPU = Integer.parseInt(fields[COMPUTE_VCPU_FIELD_INDEX]);
				int ram = Integer.parseInt(fields[COMPUTE_RAM_FIELD_INDEX]);
				value = Double.parseDouble(fields[COMPUTE_VALUE_FIELD_INDEX]);
				
				newItem = new ComputeItem(vCPU, ram);
			} else if (resourceType.equals(VOLUME_RESOURCE_TYPE)) {
				int size = Integer.parseInt(fields[VOLUME_SIZE_FIELD_INDEX]);
				value = Double.parseDouble(fields[VOLUME_VALUE_FIELD_INDEX]);
				
				newItem = new VolumeItem(size);
			} else {
				throw new InvalidParameterException(
						String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM_TYPE, resourceType));
			}
			
			plan.put(newItem, value);
		}
		
		return plan;
	}
	
	public Map<String, String> getRulesAsMap() {
		return basePlan;
	}

	// TODO test
	public void update(Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> newPlan = validatePlanInfo(planInfo);
		this.plan = newPlan;
	}

	// TODO test
	public Double getItemFinancialValue(ResourceItem resourceItem) {
		return plan.get(resourceItem);
	}
}
