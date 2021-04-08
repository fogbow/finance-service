package cloud.fogbow.fs.core.util.accounting;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.internal.LinkedTreeMap;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.plugins.payment.ComputeItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.VolumeItem;
import cloud.fogbow.fs.core.util.JsonUtils;


public class RecordUtils {

	private static final String RESOURCE_TYPE_KEY = "resourceType";
	private static final String COMPUTE_RESOURCE = "compute";
	private static final String VOLUME_RESOURCE = "volume";
	
	private JsonUtils jsonUtils;
	
	public RecordUtils() {
		this.jsonUtils = new JsonUtils();
	}
	
	// TODO test
	public List<Record> getRecordsFromString(String recordsString) {
    	ArrayList<Record> recordList = new ArrayList<Record>();
        ArrayList<LinkedTreeMap<String, Object>> rawRecordsList = 
        		this.jsonUtils.fromJson(recordsString, ArrayList.class);

        for (LinkedTreeMap<String, Object> rawRecord : rawRecordsList) {
        	Record record;
        	
        	switch((String) rawRecord.get(RESOURCE_TYPE_KEY)) {
        		case COMPUTE_RESOURCE: record = getComputeRecord(rawRecord); break;
        		case VOLUME_RESOURCE: record = getVolumeRecord(rawRecord); break;
        		// TODO treat this
        		default: record = null;
            }
        	
        	recordList.add(record);
        }
        
        return recordList;
	}

	private ComputeRecord getComputeRecord(LinkedTreeMap<String, Object> rawRecord) {
		String jsonRepr = this.jsonUtils.toJson((LinkedTreeMap<String, Object>) rawRecord);
		return this.jsonUtils.fromJson(jsonRepr, ComputeRecord.class);
	}
	
	private VolumeRecord getVolumeRecord(LinkedTreeMap<String, Object> rawRecord) {
		String jsonRepr = this.jsonUtils.toJson((LinkedTreeMap<String, Object>) rawRecord);
		return this.jsonUtils.fromJson(jsonRepr, VolumeRecord.class);
	}

	public class ComputeRecord extends Record {
		private ComputeSpec spec;
		
		public ComputeRecord(Long id, String orderId, String resourceType, ComputeSpec spec, String requester, Timestamp startTime,
				  Timestamp startDate, Timestamp endTime, Timestamp endDate, long duration, OrderState state) {
			super(id, orderId, resourceType, requester, startTime, startDate, endTime, endDate, duration, state);
		}
		
		@Override
		public ComputeSpec getSpec() {
			return spec;
		}

		@Override
		public void setSpec(OrderSpec orderSpec) {
			this.spec = (ComputeSpec) orderSpec;
		}
	}
	
	public class VolumeRecord extends Record {
		private VolumeSpec spec;
		
		public VolumeRecord(Long id, String orderId, String resourceType, ComputeSpec spec, String requester, Timestamp startTime,
				  Timestamp startDate, Timestamp endTime, Timestamp endDate, long duration, OrderState state) {
			super(id, orderId, resourceType, requester, startTime, startDate, endTime, endDate, duration, state);
		}
		
		@Override
		public VolumeSpec getSpec() {
			return spec;
		}

		@Override
		public void setSpec(OrderSpec orderSpec) {
			this.spec = (VolumeSpec) orderSpec;
		}
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
