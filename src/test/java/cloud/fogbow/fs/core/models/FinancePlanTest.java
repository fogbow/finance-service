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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.TestUtils;

public class FinancePlanTest {

	private static final String ITEM_ID1 = "1";
	private static final String ITEM_ID2 = "2";
	private static final String ITEM_ID3 = "3";
	private static final String ITEM_ID4 = "4";
	private static final String PLAN_NAME = "planName1";
	private static final int COMPUTE_1_VCPU = 2;
	private static final int COMPUTE_1_RAM = 4;
	private static final Double COMPUTE_1_VALUE = 5.0;
	private static final int COMPUTE_2_VCPU = 4;
	private static final int COMPUTE_2_RAM = 8;
	private static final Double COMPUTE_2_VALUE = 9.0;
	private static final int VOLUME_1_SIZE = 50;
	private static final Double VOLUME_1_VALUE = 2.0;
	private static final int VOLUME_2_SIZE = 10;
	private static final Double VOLUME_2_VALUE = 0.3;
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
	private HashMap<String, String> planInfo;
    private ComputeItem computeItem1BeforeUpdate;
    private ComputeItem computeItem2BeforeUpdate;
    private VolumeItem volumeItem1BeforeUpdate;
    private VolumeItem volumeItem2BeforeUpdate;
	
	// test case: When creating a FinancePlan object, the constructor must validate the 
	// plan data passed as argument and set up the FinancePlan object correctly.
	@Test
	public void testConstructorValidPlanInfo() throws InvalidParameterException {
		setUpPlanInfo();
		
		FinancePlan plan = new FinancePlan(PLAN_NAME, planInfo);
		
		ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
		ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
		ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
		ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
		
		assertEquals(COMPUTE_1_VALUE, plan.getItemFinancialValue(computeItem1));
		assertEquals(COMPUTE_2_VALUE, plan.getItemFinancialValue(computeItem2));
		assertEquals(VOLUME_1_VALUE, plan.getItemFinancialValue(volumeItem1));
		assertEquals(VOLUME_2_VALUE, plan.getItemFinancialValue(volumeItem2));
	}
	
	// test case: When creating a FinancePlan object and one of the plan items
	// is of an unknown type, the constructor must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInvalidResourceType() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = "invalidtype";
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);
		
		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the compute items 
	// definitions passed as argument contains an empty compute value, the constructor
	// must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains no compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoMissingComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[3];
		computeItemValues1[0] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[1] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[2] = String.valueOf(COMPUTE_1_RAM);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
    // test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains an unparsable compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = "nonparsablevalue";
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
    // test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains a negative compute value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoNegativeComputeValue() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = "-10.0";
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains an empty compute vCPU, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeVcpu() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
    // test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains an unparsable compute vCPU, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeVcpu() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = "unparsablevcpu";
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains an empty compute ram, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyComputeRam() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
    // test case: When creating a FinancePlan object and one of the compute items 
    // definitions passed as argument contains an unparsable compute ram, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableComputeRam() throws InvalidParameterException {
		String[] computeItemValues1 = new String[4];
		computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
		computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = "unparsableram";
		computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
		String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

		setUpPlanInfo(computeItemString1, getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains an empty volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
    // test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains an unparsable volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf("unparsablesize");
		volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains no volume size, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoMissingVolumeSize() throws InvalidParameterException {
		String[] volumeItemValues = new String[2];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains an empty volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoEmptyVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains an unparsable volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoUnparsableVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("unparsablevalue");
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When creating a FinancePlan object and one of the volume items 
    // definitions passed as argument contains a negative volume value, the constructor
    // must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConstructorInvalidPlanInfoNegativeVolumeValue() throws InvalidParameterException {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
		volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("-5.0");
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				volumeItemString, getValidVolume2String());
		
		new FinancePlan(PLAN_NAME, planInfo);
	}
	
	// test case: When calling the getItemFinanceValue method and the item is not 
	// known by the financial plan, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetItemFinancialValueItemDoesNotExist() throws InvalidParameterException {
		setUpPlanInfo();
		
		FinancePlan plan = new FinancePlan(PLAN_NAME, planInfo);
		ResourceItem unknownItem1 = new ComputeItem(UNKNOWN_ITEM_VCPU, UNKNOWN_ITEM_RAM);
		
		plan.getItemFinancialValue(unknownItem1);
	}
	
	// test case: When creating a FinancePlan object using a file as data source, 
    // the constructor must read the plan data from the file, validate the data and
    // set up the FinancePlan object correctly.
    @Test
    public void testConstructorReadPlanFromFile() throws InvalidParameterException {
        FinancePlan plan = new FinancePlan(PLAN_NAME, "src/test/resources/private/test_plan.txt");
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);

        assertEquals(COMPUTE_1_VALUE, plan.getItemFinancialValue(computeItem1));
        assertEquals(COMPUTE_2_VALUE, plan.getItemFinancialValue(computeItem2));
        assertEquals(VOLUME_1_VALUE, plan.getItemFinancialValue(volumeItem1));
        assertEquals(VOLUME_2_VALUE, plan.getItemFinancialValue(volumeItem2));
    }
    
    // test case: When creating a FinancePlan object using a file as data source and
    // the data source file does not exist, the constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testConstructorDataSourceFileDoesNotExist() throws InvalidParameterException {
        new FinancePlan(PLAN_NAME, "unknown_file.txt");
    }
    
    // test case: When calling the getRulesAsMap method, it must return a Map containing representations 
    // of the resource items considered in the FinancePlan. Each representation of resource item must
    // be mapped to the correct resource item value.
    @Test
    public void testGetRulesAsMap() throws InvalidParameterException {
        setUpPlanInfo();
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
        
        FinancePlan plan = new FinancePlan(PLAN_NAME, planInfo);
        
        
        Map<String, String> returnedMap = plan.getRulesAsMap();

        assertEquals(4, returnedMap.size());
        
        assertTrue(returnedMap.containsKey(computeItem1.toString()));
        assertEquals(String.valueOf(COMPUTE_1_VALUE), returnedMap.get(computeItem1.toString()));
        
        assertTrue(returnedMap.containsKey(computeItem2.toString()));
        assertEquals(String.valueOf(COMPUTE_2_VALUE), returnedMap.get(computeItem2.toString()));
        
        assertTrue(returnedMap.containsKey(volumeItem1.toString()));
        assertEquals(String.valueOf(VOLUME_1_VALUE), returnedMap.get(volumeItem1.toString()));
        
        assertTrue(returnedMap.containsKey(volumeItem2.toString()));
        assertEquals(String.valueOf(VOLUME_2_VALUE), returnedMap.get(volumeItem2.toString()));
    }
    
    // test case: When calling the getPlanFromDatabaseItems method, it must return a Map of 
    // resource item specification to resource item value. The map must contain a 
    // mapping for each element contained in the list passed as argument.
    @Test
    public void testGetPlanFromDatabaseItems() throws InvalidParameterException {
        FinancePlan plan = new FinancePlan();
        
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);
        
        List<FinancePlanItem> items = new ArrayList<FinancePlanItem>();
        items.add(new FinancePlanItem(computeItem1, COMPUTE_1_VALUE));
        items.add(new FinancePlanItem(computeItem2, COMPUTE_2_VALUE));
        items.add(new FinancePlanItem(volumeItem1, VOLUME_1_VALUE));
        items.add(new FinancePlanItem(volumeItem2, VOLUME_2_VALUE));
        
        Map<ResourceItem, Double> returnedPlan = plan.getPlanFromDatabaseItems(items);
        
        assertEquals(4, returnedPlan.size());
        
        assertEquals(COMPUTE_1_VALUE, returnedPlan.get(computeItem1));
        assertEquals(COMPUTE_2_VALUE, returnedPlan.get(computeItem2));
        assertEquals(VOLUME_1_VALUE, returnedPlan.get(volumeItem1));
        assertEquals(VOLUME_2_VALUE, returnedPlan.get(volumeItem2));
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

        Iterator<ResourceItem> iterator = new TestUtils().getIterator(
                Arrays.asList(computeItem1, computeItem2, volumeItem1, volumeItem2));
        
        HashSet<ResourceItem> itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        HashMap<ResourceItem, Double> planItems = Mockito.mock(HashMap.class);
        Mockito.when(planItems.keySet()).thenReturn(itemsSet);
        Mockito.when(planItems.get(computeItem1)).thenReturn(COMPUTE_1_VALUE);
        Mockito.when(planItems.get(computeItem2)).thenReturn(COMPUTE_2_VALUE);
        Mockito.when(planItems.get(volumeItem1)).thenReturn(VOLUME_1_VALUE);
        Mockito.when(planItems.get(volumeItem2)).thenReturn(VOLUME_2_VALUE);
        
        FinancePlan plan = new FinancePlan(planItems);
        
        String result = plan.toString();
        
        String expectedString = String.format("{0:[%s,%s],1:[%s,%s],2:[%s,%s],3:[%s,%s]}", 
                COMPUTE_ITEM_1_TO_STRING, String.valueOf(COMPUTE_1_VALUE), 
                COMPUTE_ITEM_2_TO_STRING, String.valueOf(COMPUTE_2_VALUE), 
                VOLUME_ITEM_1_TO_STRING, String.valueOf(VOLUME_1_VALUE), 
                VOLUME_ITEM_2_TO_STRING, String.valueOf(VOLUME_2_VALUE));
        
        assertEquals(expectedString, result);
    }
    
    @Test
    public void testUpdateValidPlanInfo() throws InvalidParameterException {
        setUpPlanInfo();
        // get items before update
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        // set up items after update
        ResourceItem computeItem1 = new ComputeItem(COMPUTE_1_VCPU, COMPUTE_1_RAM);
        ResourceItem computeItem2 = new ComputeItem(COMPUTE_2_VCPU, COMPUTE_2_RAM);
        ResourceItem volumeItem1 = new VolumeItem(VOLUME_1_SIZE);
        ResourceItem volumeItem2 = new VolumeItem(VOLUME_2_SIZE);

        FinancePlan plan = new FinancePlan(planItems);
        
        // check plan state before update
        assertPlanDoesNotContainItem(plan, computeItem1);
        assertPlanDoesNotContainItem(plan, computeItem2);
        assertPlanDoesNotContainItem(plan, volumeItem1);
        assertPlanDoesNotContainItem(plan, volumeItem2);
        
        // exercise
        plan.update(planInfo);
        
        // check plan state after update
        assertPlanDoesNotContainItem(plan, computeItem1BeforeUpdate);
        assertPlanDoesNotContainItem(plan, computeItem2BeforeUpdate);
        assertPlanDoesNotContainItem(plan, volumeItem1BeforeUpdate);
        assertPlanDoesNotContainItem(plan, volumeItem2BeforeUpdate);
        
        assertEquals(COMPUTE_1_VALUE, plan.getItemFinancialValue(computeItem1));
        assertEquals(COMPUTE_2_VALUE, plan.getItemFinancialValue(computeItem2));
        assertEquals(VOLUME_1_VALUE, plan.getItemFinancialValue(volumeItem1));
        assertEquals(VOLUME_2_VALUE, plan.getItemFinancialValue(volumeItem2));
    }
    
    // test case: When calling the update method and one of the plan items
    // is of an unknown type, the constructor must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInvalidResourceType() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = "invalidtype";
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);
        
        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains no compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoMissingComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[3];
        computeItemValues1[0] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[1] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[2] = String.valueOf(COMPUTE_1_RAM);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = "nonparsablevalue";
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains a negative compute value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoNegativeComputeValue() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = "-10.0";
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute vCPU, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeVcpu() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute vCPU, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeVcpu() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = "unparsablevcpu";
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(COMPUTE_1_RAM);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an empty compute ram, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyComputeRam() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the compute items 
    // definitions passed as argument contains an unparsable compute ram, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableComputeRam() throws InvalidParameterException {
        String[] computeItemValues1 = new String[4];
        computeItemValues1[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
        computeItemValues1[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(COMPUTE_1_VCPU);
        computeItemValues1[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = "unparsableram";
        computeItemValues1[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(COMPUTE_1_VALUE);
        String computeItemString1 = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues1);

        setUpPlanInfo(computeItemString1, getValidCompute2String(),
                getValidVolume1String(), getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an empty volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an unparsable volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf("unparsablesize");
        volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(VOLUME_1_VALUE);
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains no volume size, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoMissingVolumeSize() throws InvalidParameterException {
        String[] volumeItemValues = new String[2];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an empty volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoEmptyVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains an unparsable volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoUnparsableVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("unparsablevalue");
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        HashMap<ResourceItem, Double> planItems = setUpItemsBeforeUpdate();
        
        FinancePlan plan = new FinancePlan(planItems);
        
        plan.update(planInfo);
    }
    
    // test case: When calling the update method and one of the volume items 
    // definitions passed as argument contains a negative volume value, the constructor
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateInvalidPlanInfoNegativeVolumeValue() throws InvalidParameterException {
        String[] volumeItemValues = new String[3];
        volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
        volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(VOLUME_1_SIZE);
        volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf("-5.0");
        
        String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);

        setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
                volumeItemString, getValidVolume2String());
        
        new FinancePlan(PLAN_NAME, planInfo);
    }

    private HashMap<ResourceItem, Double> setUpItemsBeforeUpdate() {
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
        Iterator<ResourceItem> iterator = new TestUtils().getIterator(
                Arrays.asList(computeItem1BeforeUpdate, computeItem2BeforeUpdate, 
                        volumeItem1BeforeUpdate, volumeItem2BeforeUpdate));
        
        HashSet<ResourceItem> itemsSet = Mockito.mock(HashSet.class);
        Mockito.when(itemsSet.iterator()).thenReturn(iterator);
        
        HashMap<ResourceItem, Double> planItems = Mockito.mock(HashMap.class);
        Mockito.when(planItems.keySet()).thenReturn(itemsSet);
        Mockito.when(planItems.get(computeItem1BeforeUpdate)).thenReturn(COMPUTE_1_VALUE_BEFORE_UPDATE);
        Mockito.when(planItems.get(computeItem2BeforeUpdate)).thenReturn(COMPUTE_2_VALUE_BEFORE_UPDATE);
        Mockito.when(planItems.get(volumeItem1BeforeUpdate)).thenReturn(VOLUME_1_VALUE_BEFORE_UPDATE);
        Mockito.when(planItems.get(volumeItem2BeforeUpdate)).thenReturn(VOLUME_2_VALUE_BEFORE_UPDATE);
        return planItems;
    }

    private void assertPlanDoesNotContainItem(FinancePlan plan, ResourceItem item) {
        try {
            plan.getItemFinancialValue(item);
            Assert.fail("Expected to throw InvalidParameterException.");
        } catch (InvalidParameterException e) {
            
        }
    }
    
	private void setUpPlanInfo() {
		setUpPlanInfo(getValidCompute1String(), getValidCompute2String(),
				getValidVolume1String(), getValidVolume2String());
	}
	
	private void setUpPlanInfo(String computeItemString1, String computeItemString2, String volumeItemString1, 
			String volumeItemString2) {
		this.planInfo = new HashMap<String, String>();
		
		planInfo.put(ITEM_ID1, computeItemString1);
		planInfo.put(ITEM_ID2, computeItemString2);
		planInfo.put(ITEM_ID3, volumeItemString1);
		planInfo.put(ITEM_ID4, volumeItemString2);
	}

	private String getValidCompute2String() {
		return getComputeString(COMPUTE_2_VCPU, COMPUTE_2_RAM, COMPUTE_2_VALUE);
	}

	private String getValidCompute1String() {
		return getComputeString(COMPUTE_1_VCPU, COMPUTE_1_RAM, COMPUTE_1_VALUE);
	}
	
	private String getComputeString(int vcpu, int ram, double value) {
		String[] computeItemValues = new String[4];
		computeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.COMPUTE_RESOURCE_TYPE;
		computeItemValues[FinancePlan.COMPUTE_VCPU_FIELD_INDEX] = String.valueOf(vcpu);
		computeItemValues[FinancePlan.COMPUTE_RAM_FIELD_INDEX] = String.valueOf(ram);
		computeItemValues[FinancePlan.COMPUTE_VALUE_FIELD_INDEX] = String.valueOf(value);
		
		String computeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, computeItemValues);
		return computeItemString;
	}
	
	private String getValidVolume1String() {
		return getValidVolumeString(VOLUME_1_SIZE, VOLUME_1_VALUE);
	}
	
	private String getValidVolume2String() {
		return getValidVolumeString(VOLUME_2_SIZE, VOLUME_2_VALUE);
	}
	
	private String getValidVolumeString(int size, double value) {
		String[] volumeItemValues = new String[3];
		volumeItemValues[FinancePlan.RESOURCE_TYPE_FIELD_INDEX] = FinancePlan.VOLUME_RESOURCE_TYPE;
		volumeItemValues[FinancePlan.VOLUME_SIZE_FIELD_INDEX] = String.valueOf(size);
		volumeItemValues[FinancePlan.VOLUME_VALUE_FIELD_INDEX] = String.valueOf(value);
		
		String volumeItemString = String.join(FinancePlan.ITEM_FIELDS_SEPARATOR, volumeItemValues);
		return volumeItemString;
	}
}
