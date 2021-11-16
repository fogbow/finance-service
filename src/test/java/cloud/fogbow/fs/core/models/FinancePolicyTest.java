package cloud.fogbow.fs.core.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.TestUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

// TODO review empty fields tests
public class FinancePolicyTest {
	private static final String ITEM_ID1 = "1";
	private static final String ITEM_ID2 = "2";
	private static final String ITEM_ID3 = "3";
	private static final String ITEM_ID4 = "4";
	private static final String PLAN_NAME = "planName1";
	private static final int COMPUTE_1_VCPU = 2;
	private static final int COMPUTE_1_RAM = 4;
	private static final Double COMPUTE_1_VALUE = 5.0;
	private static final String COMPUTE_1_TIME_UNIT = FinancePolicy.SECONDS;
	private static final int COMPUTE_2_VCPU = 4;
	private static final int COMPUTE_2_RAM = 8;
	private static final Double COMPUTE_2_VALUE = 9.0;
	private static final String COMPUTE_2_TIME_UNIT = FinancePolicy.MINUTES;
	private static final int VOLUME_1_SIZE = 50;
	private static final Double VOLUME_1_VALUE = 2.0;
	private static final String VOLUME_1_TIME_UNIT = FinancePolicy.HOURS;
	private static final int VOLUME_2_SIZE = 10;
	private static final Double VOLUME_2_VALUE = 0.3;
	private static final String VOLUME_2_TIME_UNIT = FinancePolicy.MILLISECONDS;
	private static final int UNKNOWN_ITEM_VCPU = 1;
	private static final int UNKNOWN_ITEM_RAM = 1;
	private static final Double COMPUTE_1_VALUE_BEFORE_UPDATE = COMPUTE_1_VALUE + 1;
	private static final Double COMPUTE_2_VALUE_BEFORE_UPDATE = COMPUTE_2_VALUE + 1;
	private static final Double VOLUME_1_VALUE_BEFORE_UPDATE = VOLUME_1_VALUE + 1;
	private static final Double VOLUME_2_VALUE_BEFORE_UPDATE = VOLUME_2_VALUE + 2;
    private static final String COMPUTE_ITEM_1_TO_STRING = "compute1ToString";
    private static final String COMPUTE_ITEM_2_TO_STRING = "compute2ToString";
    private static final String VOLUME_ITEM_1_TO_STRING = "volume1ToString";
    private static final String VOLUME_ITEM_2_TO_STRING = "volume2ToString";
    private static final OrderState STATE_1 = OrderState.FULFILLED;
    private static final OrderState STATE_2 = OrderState.CLOSED;
    private static final Double DEFAULT_RESOURCE_VALUE = 0.0001;
	private HashMap<String, String> policyInfo;
    private ComputeItem computeItem1BeforeUpdate;
    private ComputeItem computeItem2BeforeUpdate;
    private VolumeItem volumeItem1BeforeUpdate;
    private VolumeItem volumeItem2BeforeUpdate;
	
	// test case: When creating a FinancePolicy object, the constructor must validate the 
	// policy data passed as argument and set up the FinancePolicy object correctly.
	@Test
	public void testConstructorValidPlanInfo() throws InvalidParameterException {
		setUpPolicyInfo();
		
		FinancePolicy policy = new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
		
		ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
		ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
		ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
		ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
		
		assertEquals(COMPUTE_1_VALUE, policy.getItemFinancialValue(computeItem1, STATE_1));
		assertEquals(COMPUTE_2_VALUE, policy.getItemFinancialValue(computeItem2, STATE_2));
		assertEquals(VOLUME_1_VALUE, policy.getItemFinancialValue(volumeItem1, STATE_1));
		assertEquals(VOLUME_2_VALUE, policy.getItemFinancialValue(volumeItem2, STATE_2));
	}
	
	// test case: When creating a FinancePolicy object and one of the plan items
	// is of an unknown type, the constructor must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInvalidResourceType() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = "invalidtype";
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
		        FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + COMPUTE_1_TIME_UNIT;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);
		
		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the compute items 
	// definitions passed as argument contains an empty compute value, the constructor
	// must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains no compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoMissingComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[3];
		computeItemValues1[0] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[1] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[2] = String.valueOf(COMPUTE_1_RAM);
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains an unparsable compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = "nonparsablevalue";
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains a negative compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoNegativeComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] =  "-10.0" + "/" + COMPUTE_1_TIME_UNIT ;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the compute items
	// definitions passed as argument contains an invalid time unit for the compute value, the
	// constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testConstructorInvalidPlanInfoInvalidComputeValueTimeUnit() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + "invalidtimeunit";
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(), getValidVolume1String(), getValidVolume2String());

        new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
    }
	
	// test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains an empty compute vCPU, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeVcpu() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + COMPUTE_1_TIME_UNIT;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains an unparsable compute vCPU, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeVcpu() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = "unparsablevcpu";
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + COMPUTE_1_TIME_UNIT;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains an empty compute ram, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeRam() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + COMPUTE_1_TIME_UNIT;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the compute items 
    // definitions passed as argument contains an unparsable compute ram, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeRam() throws InvalidParameterException {
		String[] computeItemValues1 = new String[5];
		computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = "unparsableram";
		computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + COMPUTE_1_TIME_UNIT;
		String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPolicyInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains an empty volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[4];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + VOLUME_1_TIME_UNIT;
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains an unparsable volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[4];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf("unparsablesize");
		volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + VOLUME_1_TIME_UNIT;
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains no volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoMissingVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains an empty volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains an unparsable volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[4];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("unparsablevalue");
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
	// test case: When creating a FinancePolicy object and one of the volume items 
    // definitions passed as argument contains a negative volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoNegativeVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[4];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("-5.0");
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
	}
	
    // test case: When creating a FinancePolicy object and one of the volume items
    // definitions passed as argument contains an invalid time unit for the volume value, the
    // constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testConstructorInvalidPlanInfoInvalidVolumeValueTimeUnit() throws InvalidParameterException {
        String[] volumeItemValues = new String[4];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE) + 
                FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + "invalidtimeunit";
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
    }
	
	// test case: When calling the getItemFinanceValue method and the item is not 
	// known by the financial plan, it must return the default resource value.
	@Test
	public void testGetItemFinancialValueItemDoesNotExist() throws InvalidParameterException {
		setUpPolicyInfo();
		
		FinancePolicy plan = new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
		ResourceItem unknownItem1 = new ComputeItem(UNKNOWN_ITEM_VCPU, UNKNOWN_ITEM_RAM);
		
		assertEquals(DEFAULT_RESOURCE_VALUE, plan.getItemFinancialValue(unknownItem1, STATE_1));
	}
	
	// test case: When creating a FinancePolicy object using a file as data source, 
    // the constructor must read the plan data from the file, validate the data and
    // set up the FinancePolicy object correctly.
    @Test
    public void testConstructorReadPlanFromFile() throws InvalidParameterException {
        FinancePolicy plan = new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, 
                "src/test/resources/private/test_plan.txt");
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);

        assertEquals(COMPUTE_1_VALUE, plan.getItemFinancialValue(computeItem1, STATE_1));
        assertEquals(COMPUTE_2_VALUE, plan.getItemFinancialValue(computeItem2, STATE_2));
        assertEquals(VOLUME_1_VALUE, plan.getItemFinancialValue(volumeItem1, STATE_1));
        assertEquals(VOLUME_2_VALUE, plan.getItemFinancialValue(volumeItem2, STATE_2));
    }
    
    // test case: When creating a FinancePolicy object using a file as data source and
    // the data source file does not exist, the constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testConstructorDataSourceFileDoesNotExist() throws InvalidParameterException {
        new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, "unknown_file.txt");
    }
    
    // test case: When calling the getRulesAsMap method, it must return a Map containing representations 
    // of the resource items considered in the FinancePolicy. Each representation of resource item must
    // be mapped to the correct resource item value.
    @Test
    public void testGetRulesAsMap() throws InvalidParameterException {
        setUpPolicyInfo();
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
        
        FinancePolicy plan = new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
        
        Map<String, String> returnedMap = plan.getRulesAsMap();

        assertEquals(4, returnedMap.size());

        String keyCompute1 = String.format("%s-%s", computeItem1.toString(), STATE_1.getValue());
        String valueCompute1 = String.format("%s-%s", String.valueOf(COMPUTE_1_VALUE), String.valueOf(TimeUnit.SECONDS)); 
        assertTrue(returnedMap.containsKey(keyCompute1));
        assertEquals(valueCompute1, returnedMap.get(keyCompute1));
        
        String keyCompute2 = String.format("%s-%s", computeItem2.toString(), STATE_2.getValue());
        String valueCompute2 = String.format("%s-%s", String.valueOf(COMPUTE_2_VALUE), String.valueOf(TimeUnit.MINUTES));
        assertTrue(returnedMap.containsKey(keyCompute2));
        assertEquals(valueCompute2, returnedMap.get(keyCompute2));

        String keyVolume1 = String.format("%s-%s", volumeItem1.toString(), STATE_1.getValue());
        String valueVolume1 = String.format("%s-%s", String.valueOf(VOLUME_1_VALUE), String.valueOf(TimeUnit.HOURS));
        assertTrue(returnedMap.containsKey(keyVolume1));
        assertEquals(valueVolume1, returnedMap.get(keyVolume1));
        
        String keyVolume2 = String.format("%s-%s", volumeItem2.toString(), STATE_2.getValue());
        String valueVolume2 = String.format("%s-%s", String.valueOf(VOLUME_2_VALUE), String.valueOf(TimeUnit.MILLISECONDS));
        assertTrue(returnedMap.containsKey(keyVolume2));
        assertEquals(valueVolume2, returnedMap.get(keyVolume2));
    }
    
    // test case: When calling the getPlanFromDatabaseItems method, it must return a Map of 
    // finance rule context to finance rule value. The map must contain a 
    // mapping for each element contained in the list passed as argument.
    @Test
    public void testGetPlanFromDatabaseItems() throws InvalidParameterException {
        FinancePolicy plan = new FinancePolicy();
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
        
        List<FinanceRule> items = new ArrayList<FinanceRule>();
        FinanceRuleContext contextComputeItem1 = new FinanceRuleContext(computeItem1, STATE_1);
        FinanceRuleContext contextComputeItem2 = new FinanceRuleContext(computeItem2, STATE_2);
        FinanceRuleContext contextVolumeItem1 = new FinanceRuleContext(volumeItem1, STATE_1);
        FinanceRuleContext contextVolumeItem2 = new FinanceRuleContext(volumeItem2, STATE_2);
        
        FinanceRuleValue valueComputeItem1 = new FinanceRuleValue(COMPUTE_1_VALUE, TimeUnit.SECONDS);
        FinanceRuleValue valueComputeItem2 = new FinanceRuleValue(COMPUTE_2_VALUE, TimeUnit.MINUTES);
        FinanceRuleValue valueVolumeItem1 = new FinanceRuleValue(VOLUME_1_VALUE, TimeUnit.HOURS);
        FinanceRuleValue valueVolumeItem2 = new FinanceRuleValue(VOLUME_2_VALUE, TimeUnit.MILLISECONDS);
                
        items.add(new FinanceRule(contextComputeItem1, valueComputeItem1));
        items.add(new FinanceRule(contextComputeItem2, valueComputeItem2));
        items.add(new FinanceRule(contextVolumeItem1, valueVolumeItem1));
        items.add(new FinanceRule(contextVolumeItem2, valueVolumeItem2));
        
        Map<FinanceRuleContext, FinanceRuleValue> returnedPlan = plan.getPolicyFromDatabaseItems(items);
        
        assertEquals(4, returnedPlan.size());
        
        assertEquals(COMPUTE_1_VALUE, returnedPlan.get(new FinanceRuleContext(computeItem1, STATE_1)).getValue());
        assertEquals(TimeUnit.SECONDS, returnedPlan.get(new FinanceRuleContext(computeItem1, STATE_1)).getTimeUnit());
        assertEquals(COMPUTE_2_VALUE, returnedPlan.get(new FinanceRuleContext(computeItem2, STATE_2)).getValue());
        assertEquals(TimeUnit.MINUTES, returnedPlan.get(new FinanceRuleContext(computeItem2, STATE_2)).getTimeUnit());
        assertEquals(VOLUME_1_VALUE, returnedPlan.get(new FinanceRuleContext(volumeItem1, STATE_1)).getValue());
        assertEquals(TimeUnit.HOURS, returnedPlan.get(new FinanceRuleContext(volumeItem1, STATE_1)).getTimeUnit());
        assertEquals(VOLUME_2_VALUE, returnedPlan.get(new FinanceRuleContext(volumeItem2, STATE_2)).getValue());
        assertEquals(TimeUnit.MILLISECONDS, returnedPlan.get(new FinanceRuleContext(volumeItem2, STATE_2)).getTimeUnit());
    }
    
    // test case: When calling the toString method, it must generate and 
    // return a String containing representations of the plan's resource items
    // and resource items values.
    @Test
    public void testToString() throws InvalidParameterException {
        ResourceItem computeItem1 = Mockito.mock(ComputeItem.class);
        ResourceItem computeItem2 = Mockito.mock(ComputeItem.class);
        ResourceItem volumeItem1 = Mockito.mock(VolumeItem.class);
        ResourceItem volumeItem2 = Mockito.mock(VolumeItem.class);
        
        Mockito.when(computeItem1.toString()).thenReturn(COMPUTE_ITEM_1_TO_STRING);
        Mockito.when(computeItem2.toString()).thenReturn(COMPUTE_ITEM_2_TO_STRING);
        Mockito.when(volumeItem1.toString()).thenReturn(VOLUME_ITEM_1_TO_STRING);
        Mockito.when(volumeItem2.toString()).thenReturn(VOLUME_ITEM_2_TO_STRING);

        Iterator<FinanceRuleContext> iterator = new TestUtils().getIterator(
                Arrays.asList(new FinanceRuleContext(computeItem1, STATE_1), 
                              new FinanceRuleContext(computeItem2, STATE_2),
                              new FinanceRuleContext(volumeItem1, STATE_1), 
                              new FinanceRuleContext(volumeItem2, STATE_2)));
        
        HashSet<FinanceRuleContext> itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = Mockito.mock(HashMap.class);
        Mockito.when(planItems.keySet()).thenReturn(itemsSet);
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem1, STATE_1))).thenReturn(new FinanceRuleValue(COMPUTE_1_VALUE, TimeUnit.SECONDS));
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem2, STATE_2))).thenReturn(new FinanceRuleValue(COMPUTE_2_VALUE, TimeUnit.MINUTES));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem1, STATE_1))).thenReturn(new FinanceRuleValue(VOLUME_1_VALUE, TimeUnit.HOURS));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem2, STATE_2))).thenReturn(new FinanceRuleValue(VOLUME_2_VALUE, TimeUnit.MILLISECONDS));
        
        Mockito.when(planItems.keySet()).thenReturn(itemsSet);
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem1, STATE_1))).
            thenReturn(new FinanceRuleValue(COMPUTE_1_VALUE, TimeUnit.SECONDS));
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem2, STATE_2))).
            thenReturn(new FinanceRuleValue(COMPUTE_2_VALUE, TimeUnit.MINUTES));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem1, STATE_1))).
            thenReturn(new FinanceRuleValue(VOLUME_1_VALUE, TimeUnit.HOURS));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem2, STATE_2))).
            thenReturn(new FinanceRuleValue(VOLUME_2_VALUE, TimeUnit.MILLISECONDS));
        
        
        FinancePolicy plan = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        String result = plan.toString();
        
        String expectedString = String.format("{0:[%s,%s,%s,%s],1:[%s,%s,%s,%s],2:[%s,%s,%s,%s],3:[%s,%s,%s,%s]}", 
                COMPUTE_ITEM_1_TO_STRING, STATE_1.getValue(), String.valueOf(COMPUTE_1_VALUE), String.valueOf(TimeUnit.SECONDS), 
                COMPUTE_ITEM_2_TO_STRING, STATE_2.getValue(), String.valueOf(COMPUTE_2_VALUE), String.valueOf(TimeUnit.MINUTES),
                VOLUME_ITEM_1_TO_STRING, STATE_1.getValue(), String.valueOf(VOLUME_1_VALUE), String.valueOf(TimeUnit.HOURS),
                VOLUME_ITEM_2_TO_STRING, STATE_2.getValue(), String.valueOf(VOLUME_2_VALUE), String.valueOf(TimeUnit.MILLISECONDS));
        
        assertEquals(expectedString, result);
    }
    
    @Test
    public void testUpdateValidPlanInfo() throws InvalidParameterException {
        setUpPolicyInfo();
        // get items before update
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        // set up items after update
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);

        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        // check plan state before update
        assertPlanDoesNotContainItem(policy, computeItem1, STATE_1);
        assertPlanDoesNotContainItem(policy, computeItem2, STATE_2);
        assertPlanDoesNotContainItem(policy, volumeItem1, STATE_1);
        assertPlanDoesNotContainItem(policy, volumeItem2, STATE_2);
        
        // exercise
        policy.update(policyInfo);
        
        // check plan state after update
        assertPlanDoesNotContainItem(policy, computeItem1BeforeUpdate, STATE_1);
        assertPlanDoesNotContainItem(policy, computeItem2BeforeUpdate, STATE_2);
        assertPlanDoesNotContainItem(policy, volumeItem1BeforeUpdate, STATE_1);
        assertPlanDoesNotContainItem(policy, volumeItem2BeforeUpdate, STATE_2);
        
        assertEquals(COMPUTE_1_VALUE, policy.getItemFinancialValue(computeItem1, STATE_1));
        assertEquals(COMPUTE_2_VALUE, policy.getItemFinancialValue(computeItem2, STATE_2));
        assertEquals(VOLUME_1_VALUE, policy.getItemFinancialValue(volumeItem1, STATE_1));
        assertEquals(VOLUME_2_VALUE, policy.getItemFinancialValue(volumeItem2, STATE_2));
    }
    
    // TODO update the policy.update tests
    
    // test case: When calling the update method and one of the plan items
    // is of an unknown type, the constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInvalidResourceType() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = "invalidtype";
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);
        
        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains no compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoMissingComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[3];
        computeItemValues1[0] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[1] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[2] = String.valueOf(COMPUTE_1_RAM);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = "nonparsablevalue";
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains a negative compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoNegativeComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = "-10.0";
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute vCPU, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeVcpu() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute vCPU, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeVcpu() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = "unparsablevcpu";
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute ram, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeRam() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute ram, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeRam() throws InvalidParameterException {
        String[] computeItemValues1 = new String[5];
        computeItemValues1[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        computeItemValues1[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = "unparsableram";
        computeItemValues1[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPolicyInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an empty volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[4];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an unparsable volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[4];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf("unparsablesize");
        volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains no volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoMissingVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an empty volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an unparsable volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[4];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("unparsablevalue");
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = setUpItemsBeforeUpdate();
        
        FinancePolicy policy = new FinancePolicy(DEFAULT_RESOURCE_VALUE, planItems);
        
        policy.update(policyInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains a negative volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoNegativeVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[4];
        volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = STATE_1.getValue();
        volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("-5.0");
        
        String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        new FinancePolicy(PLAN_NAME, DEFAULT_RESOURCE_VALUE, policyInfo);
    }

    private Map<FinanceRuleContext, FinanceRuleValue> setUpItemsBeforeUpdate() {
        // set up items before update
        computeItem1BeforeUpdate = Mockito.mock(ComputeItem.class);
        computeItem2BeforeUpdate = Mockito.mock(ComputeItem.class);
        volumeItem1BeforeUpdate = Mockito.mock(VolumeItem.class);
        volumeItem2BeforeUpdate = Mockito.mock(VolumeItem.class);
        
        Mockito.when(computeItem1BeforeUpdate.toString()).thenReturn(COMPUTE_ITEM_1_TO_STRING);
        Mockito.when(computeItem2BeforeUpdate.toString()).thenReturn(COMPUTE_ITEM_2_TO_STRING);
        Mockito.when(volumeItem1BeforeUpdate.toString()).thenReturn(VOLUME_ITEM_1_TO_STRING);
        Mockito.when(volumeItem2BeforeUpdate.toString()).thenReturn(VOLUME_ITEM_2_TO_STRING);

        // This code assures a certain order of resource items is used in the string generation
        Iterator<FinanceRuleContext> iterator = new TestUtils().getIterator(
                Arrays.asList(new FinanceRuleContext(computeItem1BeforeUpdate, STATE_1), 
                              new FinanceRuleContext(computeItem2BeforeUpdate, STATE_2),
                              new FinanceRuleContext(volumeItem1BeforeUpdate, STATE_1), 
                              new FinanceRuleContext(volumeItem2BeforeUpdate, STATE_2)));
        
        HashSet<FinanceRuleContext> itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        Map<FinanceRuleContext, FinanceRuleValue> planItems = Mockito.mock(HashMap.class);
        Mockito.when(planItems.keySet()).thenReturn(itemsSet);
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem1BeforeUpdate, STATE_1))).
            thenReturn(new FinanceRuleValue(COMPUTE_1_VALUE_BEFORE_UPDATE, TimeUnit.SECONDS));
        Mockito.when(planItems.get(new FinanceRuleContext(computeItem2BeforeUpdate, STATE_2))).
            thenReturn(new FinanceRuleValue(COMPUTE_2_VALUE_BEFORE_UPDATE, TimeUnit.MINUTES));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem1BeforeUpdate, STATE_1))).
            thenReturn(new FinanceRuleValue(VOLUME_1_VALUE_BEFORE_UPDATE, TimeUnit.HOURS));
        Mockito.when(planItems.get(new FinanceRuleContext(volumeItem2BeforeUpdate, STATE_2))).
            thenReturn(new FinanceRuleValue(VOLUME_2_VALUE_BEFORE_UPDATE, TimeUnit.MILLISECONDS));
        return planItems;
    }

    private void assertPlanDoesNotContainItem(FinancePolicy plan, ResourceItem item, OrderState state) throws InvalidParameterException {
        Assert.assertEquals(DEFAULT_RESOURCE_VALUE, plan.getItemFinancialValue(item, state));
    }
    
	private void setUpPolicyInfo() {
		setUpPolicyInfo(getValidCompute1String(), getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
	}
	
	private void setUpPolicyInfo(String computeItemString1, String computeItemString2, String volumeItemString1, 
			String volumeItemString2) {
		this.policyInfo = new HashMap<String, String>();
		
		policyInfo.put(ITEM_ID1, computeItemString1);
		policyInfo.put(ITEM_ID2, computeItemString2);
		policyInfo.put(ITEM_ID3, volumeItemString1);
		policyInfo.put(ITEM_ID4, volumeItemString2);
	}

	private String getValidCompute2String() {
		return getComputeString(COMPUTE_2_VCPU, COMPUTE_2_RAM, COMPUTE_2_VALUE, COMPUTE_2_TIME_UNIT, STATE_2);
	}

	private String getValidCompute1String() {
		return getComputeString(COMPUTE_1_VCPU, COMPUTE_1_RAM, COMPUTE_1_VALUE, COMPUTE_1_TIME_UNIT, STATE_1);
	}
	
	private String getComputeString(int vcpu, int ram, double value, String timeUnit, OrderState orderState) {
		String[] computeItemValues = new String[5];
		computeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.COMPUTE_RESOURCE_TYPE;
		computeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = orderState.getValue(); 
		computeItemValues[FinancePolicy.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(vcpu);
		computeItemValues[FinancePolicy.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(ram);
		computeItemValues[FinancePolicy.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(value) + 
		        FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + timeUnit;
		
		String computeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, computeItemValues);
		return computeItemString;
	}
	
	private String getValidVolume1String() {
		return getValidVolumeString(VOLUME_1_SIZE, VOLUME_1_VALUE, VOLUME_1_TIME_UNIT, STATE_1);
	}
	
	private String getValidVolume2String() {
		return getValidVolumeString(VOLUME_2_SIZE, VOLUME_2_VALUE, VOLUME_2_TIME_UNIT, STATE_2);
	}
	
	private String getValidVolumeString(int size, double value, String timeUnit, OrderState orderState) {
		String[] volumeItemValues = new String[4];
		volumeItemValues[FinancePolicy.RESOURCE_TYPE_FIELD_INDEX] = FinancePolicy.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePolicy.ORDER_STATE_FIELD_INDEX] = orderState.getValue();
		volumeItemValues[FinancePolicy.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(size);
		volumeItemValues[FinancePolicy.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(value) + 
		        FinancePolicy.VALUE_TIME_UNIT_SEPARATOR + timeUnit;
		
		String volumeItemString = String.join(FinancePolicy.ITEM_FIELDS_SEPARATOR, volumeItemValues);
		return volumeItemString;
	}
}
