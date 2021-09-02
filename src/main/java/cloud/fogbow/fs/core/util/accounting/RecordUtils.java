package cloud.fogbow.fs.core.util.accounting;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.OrderStateHistory;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.VolumeItem;
import cloud.fogbow.ras.core.models.orders.OrderState;


public class RecordUtils {

	public RecordUtils() {
	}
	
	@Deprecated
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

    public NavigableMap<Timestamp, OrderState> getRecordStateHistoryOnPeriod(Record record, Long paymentStartTime,
            Long paymentEndTime) throws InvalidParameterException {
        OrderStateHistory orderHistory = record.getStateHistory();
        Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> accsHistory = orderHistory.getHistory();

        Map<Timestamp, OrderState> history = convertAccountingStateToRasState(accsHistory);
        return filterStatesByPeriod(history, paymentStartTime, paymentEndTime);        
    }
    
    // TODO documentation
	public Map<OrderState, Double> getTimeFromRecordPerState(Record record, Long paymentStartTime,
			Long paymentEndTime) throws InvalidParameterException {
		OrderStateHistory orderHistory = record.getStateHistory();
		Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> accsHistory = orderHistory.getHistory();

		Map<Timestamp, OrderState> history = convertAccountingStateToRasState(accsHistory);
		NavigableMap<Timestamp, OrderState> filteredHistory = filterStatesByPeriod(history, paymentStartTime, paymentEndTime);
		Iterator<Timestamp> timestampsIterator = filteredHistory.navigableKeySet().iterator();
		
		Timestamp periodLowerLimit = null;
		Timestamp periodHigherLimit = null;
		periodHigherLimit = timestampsIterator.next();
		
		Map<OrderState, Double> timePerState = new HashMap<OrderState, Double>();
		
		do {
			periodLowerLimit = periodHigherLimit;
			periodHigherLimit = timestampsIterator.next();
			processPeriod(filteredHistory, timePerState, periodLowerLimit, periodHigherLimit);
		} while (timestampsIterator.hasNext());
		
		return timePerState;
	}
	
	private void processPeriod(NavigableMap<Timestamp, OrderState> filteredHistory, 
			Map<OrderState, Double> timePerState, Timestamp periodLowerLimit, Timestamp periodHigherLimit) {
		OrderState state = filteredHistory.get(periodLowerLimit);
		Long periodLength = periodHigherLimit.getTime() - periodLowerLimit.getTime();
		
		if (!timePerState.containsKey(state)) {
			timePerState.put(state, 0.0);
		}
		
		Double currentTotalTime = timePerState.get(state);
		timePerState.put(state, currentTotalTime + periodLength);
	}

	private Map<Timestamp, OrderState> convertAccountingStateToRasState(
			Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> accsHistory) throws InvalidParameterException {
		Map<Timestamp, OrderState> convertedMap = new HashMap<Timestamp, OrderState>();
		
		for (Timestamp key : accsHistory.keySet()) {
			cloud.fogbow.accs.core.models.orders.OrderState accsState = accsHistory.get(key);
			convertedMap.put(key, OrderState.fromValue(accsState.getRepr()));
		}
		
		return convertedMap;
	}	

	private NavigableMap<Timestamp, OrderState> filterStatesByPeriod(Map<Timestamp, OrderState> history, Long paymentStartTime,
			Long paymentEndTime) throws InvalidParameterException {
		Timestamp lowerLimit = getLowerLimit(history, paymentStartTime);
		Timestamp higherLimit = getHighestTimestampBeforeTime(history, paymentEndTime);
		
		if (lowerLimit == null) {
			// TODO test
			// TODO add message
			throw new InvalidParameterException();
		} else {
			TreeMap<Timestamp, OrderState> filteredState = new TreeMap<Timestamp, OrderState>();
			OrderState startState = history.get(lowerLimit);
			OrderState endState = history.get(higherLimit);
			
			filteredState.put(new Timestamp(paymentStartTime), startState);
			filteredState.put(new Timestamp(paymentEndTime), endState);
			
			for (Timestamp timestamp : history.keySet()) {
				if (timestamp.getTime() >= paymentStartTime 
						&& timestamp.getTime() < paymentEndTime) {
					filteredState.put(timestamp, history.get(timestamp));
				}
			}
			
			return filteredState;
		}
	}

	// if the first state of a resource is mapped to a timestamp exactly equal to the payment start time, 
	// then it is the lower limit time of the payment.
    private Timestamp getLowerLimit(Map<Timestamp, OrderState> history, Long paymentStartTime) {
		if (history.containsKey(new Timestamp(paymentStartTime))) {
		    return new Timestamp(paymentStartTime);
		} else {
		    return getHighestTimestampBeforeTime(history, paymentStartTime);
		}
    }

	private Timestamp getHighestTimestampBeforeTime(Map<Timestamp, OrderState> history, Long time) {
		Timestamp highestTimestamp = null;
		
		for (Timestamp timestamp : history.keySet()) {
			if (timestamp.getTime() < time) {
				if (highestTimestamp == null) {
					highestTimestamp = timestamp;
				} else if (timestamp.getTime() > highestTimestamp.getTime()) {
				    highestTimestamp = timestamp;
				}
			}
		}
		
		return highestTimestamp;
	}
}
