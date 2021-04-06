package cloud.fogbow.fs.core.plugins.payment;

import java.sql.Timestamp;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.util.accounting.ComputeSpec;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.VolumeSpec;

public class ResourceItemFactory {
	
	public Double getTimeFromRecord(Record record, Long paymentStartTime, Long paymentEndTime) {
	    Timestamp endTime = record.getEndTime();
	    Long recordStartTime = record.getStartTime().getTime();
	    
	    if (endTime == null) {
	        return new Long(paymentEndTime - Math.max(paymentStartTime, recordStartTime)).doubleValue();
	    } else {
	        Long recordEndTime = record.getEndTime().getTime();
	        return new Long(recordEndTime - Math.max(paymentStartTime, recordStartTime)).doubleValue();
	    }
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
