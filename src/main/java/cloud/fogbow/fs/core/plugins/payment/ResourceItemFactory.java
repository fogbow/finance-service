package cloud.fogbow.fs.core.plugins.payment;

import java.util.HashMap;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;

public class ResourceItemFactory {
	
	public Double getTimeFromRecord(Record record) {
		Long recordStartTime = record.getEndTime().getTime();
		Long recordEndTime = record.getStartTime().getTime();
		
		return recordStartTime.doubleValue() - 
				recordEndTime.doubleValue();
	}

	public ResourceItem getItemFromRecord(Record record) {
		String resourceType = record.getResourceType();
		ResourceItem item;
		
		// FIXME constant
		if (resourceType.equals("compute")) {
			ComputeSpec spec = (ComputeSpec) record.getSpec();
			item = new ComputeItem(spec.getvCpu(), spec.getRam());
		// FIXME constant
		} else if (resourceType.equals("volume")) {
			VolumeSpec spec = (VolumeSpec) record.getSpec();
			item = new VolumeItem(spec.getSize());
		} else {
			// FIXME treat this
			item = null;
		}
		
		return item;
	}

	public Map<ResourceItem, Double> getPlan(Map<String, String> basePlan) {
		Map<ResourceItem, Double> plan = new HashMap<ResourceItem, Double>();
		
		for (String itemString : basePlan.keySet()) {
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
}
