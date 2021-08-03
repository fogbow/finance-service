package cloud.fogbow.fs.core.util.accounting;

import java.sql.Timestamp;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.VolumeItem;


public class RecordUtils {

	public RecordUtils() {
	}
	
    public Double getTimeFromRecord(Record record, Long paymentStartTime, Long paymentEndTime) {
        Timestamp endTimeTimestamp = record.getEndTime();
        Long recordStartTime = record.getStartTime().getTime();
        Long startTime = Math.max(paymentStartTime, recordStartTime);
        Long endTime = null;
        Long totalTime = null;
        
        // if endTimeTimestamp is null, then the record has not ended yet. Therefore, we use
        // paymentEndTime as end time
        if (endTimeTimestamp == null) {
            endTime = paymentEndTime;
        } else {
            Long recordEndTime = endTimeTimestamp.getTime();
            // if the record end time is before the payment end time, then the record has 
            // already ended when the getRecords request was performed. 
            // Therefore, we use the record end time as the end time.
            if (recordEndTime < paymentEndTime) {
                endTime = recordEndTime;
            // if the record end time is after the payment end time, then the record has ended
            // after the getRecords request. In this case, we use the paymentEndTime as end time.
            } else {
                endTime = paymentEndTime;
            }
        }
        
        totalTime = endTime - startTime;
        return totalTime.doubleValue();
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
