package cloud.fogbow.fs.core.plugins.payment;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public class ResourceItemFactory {
	
	public Double getTimeFromRecord(Record record) {
		Long recordStartTime = record.getEndTime().getTime();
		Long recordEndTime = record.getStartTime().getTime();
		
		return recordStartTime.doubleValue() - 
				recordEndTime.doubleValue();
	}

	public ResourceItem getItemFromRecord(Record record) throws InvalidParameterException {
		String resourceType = record.getResourceType();
		ResourceItem item;
		
		if (resourceType.equals(ComputeItem.ITEM_TYPE_NAME)) {
			ComputeSpec spec = (ComputeSpec) record.getSpec();
			item = new ComputeItem(spec.getvCpu(), spec.getRam());
		} else if (resourceType.equals(VolumeItem.ITEM_TYPE_NAME)) {
			VolumeSpec spec = (VolumeSpec) record.getSpec();
			item = new VolumeItem(spec.getSize());
		} else {
			throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM_TYPE, 
					resourceType));
		}
		
		return item;
	}
}
