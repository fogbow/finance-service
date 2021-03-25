package cloud.fogbow.fs.core.models;

import java.util.HashMap;
import java.util.Map;

import cloud.fogbow.fs.core.plugins.payment.ComputeItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.VolumeItem;

public class FinancePlan {

	private String name;
	private Map<ResourceItem, Double> plan;
	private Map<String, String> basePlan;
	
	public FinancePlan(String planName, Map<String, String> planInfo) {
		Map<ResourceItem, Double> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.basePlan = planInfo;
		this.plan = plan;
	}

	public String getName() {
		return name;
	}

	private Map<ResourceItem, Double> validatePlanInfo(Map<String, String> planInfo) {
		Map<ResourceItem, Double> plan = new HashMap<ResourceItem, Double>();
		
		for (String itemString : planInfo.keySet()) {
			// FIXME constant
			String[] fields = itemString.split(",");
			// FIXME constant
			String resourceType = fields[0];
			
			ResourceItem newItem;
			double value;
			
			// FIXME constant
			if (resourceType.equals("compute")) {
				// FIXME constant
				int vCPU = Integer.parseInt(fields[1]);
				// FIXME constant
				int ram = Integer.parseInt(fields[2]);
				// FIXME constant
				value = Double.parseDouble(fields[3]);
				
				newItem = new ComputeItem(vCPU, ram);
			// FIXME constant
			} else if (resourceType.equals("volume")) {
				// FIXME constant
				int size = Integer.parseInt(fields[1]);
				// FIXME constant
				value = Double.parseDouble(fields[2]);
				
				newItem = new VolumeItem(size);
			} else {
				// FIXME treat this
				newItem = null;
				value = 0;
			}
			
			plan.put(newItem, value);
		}
		
		return plan;
	}
	
	public Map<String, String> getRulesAsMap() {
		return basePlan;
	}

	public void update(Map<String, String> planInfo) {
		Map<ResourceItem, Double> newPlan = validatePlanInfo(planInfo);
		this.plan = newPlan;
	}

	public Double getItemFinancialValue(ResourceItem resourceItem) {
		return plan.get(resourceItem);
	}
}
