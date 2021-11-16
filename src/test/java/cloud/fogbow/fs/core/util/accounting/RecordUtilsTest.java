package cloud.fogbow.fs.core.util.accounting;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.OrderStateHistory;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.VolumeItem;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class RecordUtilsTest {

    private Record record;
    private Long paymentStartTime;
    private Long paymentEndTime;
    private static final int COMPUTE_VCPU = 1;
    private static final int COMPUTE_RAM = 2;
    private static final int VOLUME_SIZE = 50;
    private Timestamp startTimeTimestamp;
    private Timestamp endTimeTimestamp;
    private RecordUtils recordUtils;
	private OrderStateHistory stateHistory;
	private Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> history;
    
    // test case: When calling the getItemFromRecord method passing a ComputeRecord, it must
    // extract the compute spec and build a ComputeItem correctly.
    @Test
    public void testGetComputeItemFromRecord() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        ComputeSpec computeSpec = new ComputeSpec(COMPUTE_VCPU, COMPUTE_RAM);
        
        Mockito.when(this.record.getResourceType()).thenReturn(ComputeItem.ITEM_TYPE_NAME);
        Mockito.when(this.record.getSpec()).thenReturn(computeSpec);

        
        ComputeItem item = (ComputeItem) this.recordUtils.getItemFromRecord(this.record);
        
        
        assertEquals(COMPUTE_VCPU, item.getvCPU());
        assertEquals(COMPUTE_RAM, item.getRam());
    }
    
    // test case: When calling the getItemFromRecord method passing a VolumeRecord, it must
    // extract the volume spec and build a VolumeItem correctly.
    @Test
    public void testGetVolumeItemFromRecord() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        VolumeSpec volumeSpec = new VolumeSpec(VOLUME_SIZE);
        
        Mockito.when(this.record.getResourceType()).thenReturn(VolumeItem.ITEM_TYPE_NAME);
        Mockito.when(this.record.getSpec()).thenReturn(volumeSpec);

        
        VolumeItem item = (VolumeItem) this.recordUtils.getItemFromRecord(this.record);
        
        
        assertEquals(VOLUME_SIZE, item.getSize());
    }
    
    // test case: When calling the getItemFromRecord method passing a Record of unknown
    // type, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetItemFromRecordUnknownType() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        Mockito.when(this.record.getResourceType()).thenReturn("unknowntype");

        this.recordUtils.getItemFromRecord(this.record);
    }
    
    // test case: When calling the getRecordStateHistoryOnPeriod method, it must
    // return only the state changes in the period passed as argument. The returned map must
    // have at least two entries. The entry with key timestamp == paymentStartTime must contain the 
    // state where the Record was in the beginning of the period. The entry with 
    // timestamp == paymentEndTime must contain the state where the Record was in the end of the period.
    @Test
    public void testGetTimeFromRecordPerState() throws InvalidParameterException {
    	this.paymentStartTime = 100L;
        this.paymentEndTime = 200L;

        this.history = getHistory(getTimestamps(this.paymentStartTime - 10, 
        										this.paymentStartTime + 20, 
        										this.paymentStartTime + 50, 
        										this.paymentEndTime + 10), 
        						  getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.PAUSED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.STOPPED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.HIBERNATED));
    	setUpRecords();
    	
        this.recordUtils = new RecordUtils();
        
    	Map<Timestamp, OrderState> response = this.recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
    	
    	assertEquals(4, response.size());
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentStartTime)));
    	assertEquals(OrderState.PAUSED, response.get(new Timestamp(this.paymentStartTime + 20)));
    	assertEquals(OrderState.STOPPED, response.get(new Timestamp(this.paymentStartTime + 50)));
    	assertEquals(OrderState.STOPPED, response.get(new Timestamp(this.paymentEndTime)));
    }

    @Test
    public void testGetTimeFromRecordPerStateNoStateChangeBeforeStartTime() throws InvalidParameterException {
    	this.paymentStartTime = 100L;
        this.paymentEndTime = 200L;

        this.history = getHistory(getTimestamps(this.paymentStartTime, 
        										this.paymentEndTime), 
        						  getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.PAUSED));
    	setUpRecords();
    	
        this.recordUtils = new RecordUtils();
        
        Map<Timestamp, OrderState> response = this.recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
    	
    	assertEquals(2, response.size());
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentStartTime)));
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentEndTime)));
    }

    @Test
    public void testGetTimeFromRecordPerStateNoStateChangeInThePeriod() throws InvalidParameterException {
    	this.paymentStartTime = 100L;
        this.paymentEndTime = 200L;

        this.history = getHistory(getTimestamps(this.paymentStartTime - 10, 
        										this.paymentEndTime + 10), 
        						  getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.PAUSED));
    	setUpRecords();
    	
        this.recordUtils = new RecordUtils();
        
        Map<Timestamp, OrderState> response = this.recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
    	
    	assertEquals(2, response.size());
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentStartTime)));
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentEndTime)));
    }

    // test case: When calling the getRecordStateHistoryOnPeriod method, it must
    // return only the state changes in the period passed as argument. The returned map must
    // have at least two entries. The entry with key timestamp == paymentStartTime must contain the 
    // state where the Record was in the beginning of the period. The entry with 
    // timestamp == paymentEndTime must contain the state where the Record was in the end of the period.
    // If a state appears more than once in the period passed as argument, then the returned map
    // must contain at least the same number of entries. 
    @Test
    public void testGetTimeFromRecordPerStateRepeatedStates() throws InvalidParameterException {
    	this.paymentStartTime = 100L;
        this.paymentEndTime = 200L;

        this.history = getHistory(getTimestamps(this.paymentStartTime - 10, 
												this.paymentStartTime + 20, 
												this.paymentStartTime + 50, 
												this.paymentEndTime + 10), 
        						  getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.PAUSED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
        								  		 cloud.fogbow.accs.core.models.orders.OrderState.HIBERNATED));
        
    	setUpRecords();
    	
        this.recordUtils = new RecordUtils();
        
        Map<Timestamp, OrderState> response = this.recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
    	
    	assertEquals(4, response.size());
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentStartTime)));
    	assertEquals(OrderState.PAUSED, response.get(new Timestamp(this.paymentStartTime + 20)));
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentStartTime + 50)));
    	assertEquals(OrderState.FULFILLED, response.get(new Timestamp(this.paymentEndTime)));
    }
    
    // test case: When calling the getRecordStateHistoryOnPeriod method, if the state history of 
    // the Record passed as parameter contains no state change before or on the paymentStartTime, then
    // it is impossible to determine the Record state in the beginning of the payment period. Thus, 
    // the method must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetRecordStateHistoryOnPeriodInvalidStateHistory() throws InvalidParameterException {
        this.paymentStartTime = 100L;
        this.paymentEndTime = 200L;

        this.history = getHistory(getTimestamps(this.paymentStartTime + 20, 
                                                this.paymentStartTime + 50, 
                                                this.paymentEndTime + 10), 
                                  getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState.PAUSED, 
                                                 cloud.fogbow.accs.core.models.orders.OrderState.FULFILLED, 
                                                 cloud.fogbow.accs.core.models.orders.OrderState.HIBERNATED));
        
        setUpRecords();
        
        this.recordUtils = new RecordUtils();
        this.recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
    }
    
    private void setUpRecords() {
    	this.stateHistory = Mockito.mock(OrderStateHistory.class);
    	Mockito.when(this.stateHistory.getHistory()).thenReturn(history);
    	
    	this.record = Mockito.mock(Record.class);
    	Mockito.when(this.record.getStartTime()).thenReturn(startTimeTimestamp);
        Mockito.when(this.record.getEndTime()).thenReturn(endTimeTimestamp);
        Mockito.when(this.record.getStateHistory()).thenReturn(stateHistory);
    }

    private Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> getHistory(List<Timestamp> timestamps, 
    		List<cloud.fogbow.accs.core.models.orders.OrderState> states) {
    	Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> history = new HashMap<>();
    	
    	for (int i = 0; i < timestamps.size(); i++) {
    		history.put(timestamps.get(i), states.get(i));
    	}
    	
    	return history;
    }

    private List<Timestamp> getTimestamps(Long ... timeValues) {
    	List<Timestamp> timestampList = new ArrayList<Timestamp>();

    	for (Long timeValue : timeValues) {
    		timestampList.add(new Timestamp(timeValue));
    	}
    	
    	return timestampList;
    }
    
    private List<cloud.fogbow.accs.core.models.orders.OrderState>
    getOrderStates(cloud.fogbow.accs.core.models.orders.OrderState ... states) {
    	List<cloud.fogbow.accs.core.models.orders.OrderState> stateList = 
    			new ArrayList<>();
    	
    	for (cloud.fogbow.accs.core.models.orders.OrderState state : states) {
    		stateList.add(state);
    	}
    	
    	return stateList;
    }
}
