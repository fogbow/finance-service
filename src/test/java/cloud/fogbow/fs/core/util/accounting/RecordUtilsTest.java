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
    private static final long RECORD_START_TIME = 100;
    private static final long RECORD_END_TIME = 200;
    private static final int COMPUTE_VCPU = 1;
    private static final int COMPUTE_RAM = 2;
    private static final int VOLUME_SIZE = 50;
    private Timestamp startTimeTimestamp;
    private Timestamp endTimeTimestamp;
    private RecordUtils recordUtils;
	private OrderStateHistory stateHistory;
	private Map<Timestamp, cloud.fogbow.accs.core.models.orders.OrderState> history;
    
    // test case 1: record start = payment start and record end = payment end
    
    // In this case, the record time is the difference between the 
    // payment end and start times.
    @Test
    public void testGetTimeFromRecordCase1() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 100L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(paymentEndTime - paymentStartTime), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }

    // test case 2: record start < payment start and record end = payment end
    
    // In this case, the record time is the difference between the 
    // payment end and start times.
    @Test
    public void testGetTimeFromRecordCase2() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 150L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 3: record start > payment start and record end = payment end 
    
    // In this case, the record time is the difference between the 
    // payment end time and the record start time.
    @Test
    public void testGetTimeFromRecordCase3() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 50L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 4: record start = payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time
    // and the payment start time.
    @Test
    public void testGetTimeFromRecordCase4() {
        setUpNormalRecordTimes();

        paymentStartTime = 100L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 5: record start < payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time and 
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase5() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 150L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 6: record start > payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time
    // and the record start time.
    @Test
    public void testGetTimeFromRecordCase6() {
        setUpNormalRecordTimes();

        paymentStartTime = 50L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 7 (NORMAL): record start = payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase7() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 100L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }

    // test case 8 (NORMAL): record start < payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase8() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 150L;
        paymentEndTime = 180L;
        
        assertEquals(new Double(30.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 9 (NORMAL): record start > payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the record start time.
    @Test
    public void testGetTimeFromRecordCase9() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 50L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 10 (RARE): record start = payment start and record end > payment end

    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase10() {
        setUpNormalRecordTimes();

        paymentStartTime = 100L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 11 (RARE): record start < payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase11() {
        setUpNormalRecordTimes();

        paymentStartTime = 150L;
        paymentEndTime = 180L;
        
        assertEquals(new Double(30.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 12 (RARE): record start > payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the record start time.
    @Test
    public void testGetTimeFromRecordCase12() {
        setUpNormalRecordTimes();

        paymentStartTime = 50L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
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
    
    // TODO documentation
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
    
    // TODO documentation
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
    
    // TODO documentation
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
    
    // TODO documentation
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
    	
    private void setUpNormalRecordTimes() {
        this.startTimeTimestamp = new Timestamp(RECORD_START_TIME);
        this.endTimeTimestamp = new Timestamp(RECORD_END_TIME);
        
        this.record = Mockito.mock(Record.class);
        Mockito.when(this.record.getStartTime()).thenReturn(startTimeTimestamp);
        Mockito.when(this.record.getEndTime()).thenReturn(endTimeTimestamp);
        
        this.recordUtils = new RecordUtils();
    }
    
    private void setUpNullRecordEndTime() {
        this.startTimeTimestamp = new Timestamp(RECORD_START_TIME);
        this.endTimeTimestamp = new Timestamp(RECORD_END_TIME);
        
        this.record = Mockito.mock(Record.class);
        Mockito.when(this.record.getStartTime()).thenReturn(startTimeTimestamp);
        Mockito.when(this.record.getEndTime()).thenReturn(null);
        
        this.recordUtils = new RecordUtils();
    }
}
