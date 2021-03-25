package cloud.fogbow.fs.core.plugins.payment;

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
}
