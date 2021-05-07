package cloud.fogbow.fs.core.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public class FinancePlan {

	public static final String PLAN_FIELDS_SEPARATOR = "-";
	public static final String ITEM_FIELDS_SEPARATOR = ",";
	public static final int RESOURCE_TYPE_FIELD_INDEX = 0;
	public static final String COMPUTE_RESOURCE_TYPE = "compute";
	public static final int COMPUTE_VCPU_FIELD_INDEX = 1;
	public static final int COMPUTE_RAM_FIELD_INDEX = 2;
	public static final int COMPUTE_VALUE_FIELD_INDEX = 3;
	public static final String VOLUME_RESOURCE_TYPE = "volume";
	public static final int VOLUME_SIZE_FIELD_INDEX = 1;
	public static final int VOLUME_VALUE_FIELD_INDEX = 2;
	
	private String name;
	private Map<ResourceItem, Double> plan;
	private Map<String, String> basePlan;
	
	// TODO test
    public FinancePlan(String planName, String planPath) throws InvalidParameterException {
    	Map<String, String> planInfo = getPlanFromFile(planPath);
    	Map<ResourceItem, Double> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.basePlan = planInfo;
		this.plan = plan;
    }
	
	public FinancePlan(String planName, Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> plan = validatePlanInfo(planInfo);
		this.name = planName;
		this.basePlan = planInfo;
		this.plan = plan;
	}
	
    private Map<String, String> getPlanFromFile(String planPath) {
        try {
        	Map<String, String> planInfo = new HashMap<String, String>();
        	File file = new File(planPath);
        	Scanner input = new Scanner(file);
            
            while (input.hasNextLine()) {
                String nextLine = input.nextLine().trim();
                if (!nextLine.isEmpty()) {
                	String[] planFields = nextLine.split(PLAN_FIELDS_SEPARATOR);
                	String itemName = planFields[0];
                	String itemInfo = planFields[1]; 
                	
                    planInfo.put(itemName, itemInfo);
                }
            }
            
            input.close();
            
            return planInfo;
        } catch (FileNotFoundException e) {
            throw new FatalErrorException(String.format(
                    Messages.Exception.UNABLE_TO_READ_CONFIGURATION_FILE_S, planPath));
        }
    }

	public String getName() {
		return name;
	}

	private Map<ResourceItem, Double> validatePlanInfo(Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> plan = new HashMap<ResourceItem, Double>();

			for (String itemKey : planInfo.keySet()) {
				String[] fields = planInfo.get(itemKey).split(ITEM_FIELDS_SEPARATOR);
				String resourceType = fields[RESOURCE_TYPE_FIELD_INDEX];

				switch(resourceType) {
					case COMPUTE_RESOURCE_TYPE: extractComputeItem(plan, fields); break;
					case VOLUME_RESOURCE_TYPE: extractVolumeItem(plan, fields); break;
					default: throw new InvalidParameterException(
							String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM_TYPE, resourceType));
				}
			}

		return plan;
	}

	private void extractComputeItem(Map<ResourceItem, Double> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateComputeFieldsLength(fields);
			
			int vCPU = Integer.parseInt(fields[COMPUTE_VCPU_FIELD_INDEX]);
			int ram = Integer.parseInt(fields[COMPUTE_RAM_FIELD_INDEX]);
			double value = Double.parseDouble(fields[COMPUTE_VALUE_FIELD_INDEX]);
			ResourceItem newItem = new ComputeItem(vCPU, ram);
			
			validateItemValue(value);
			
			plan.put(newItem, value);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_COMPUTE_ITEM_FIELD);
		}
	}

	private void validateItemValue(double value) throws InvalidParameterException {
		if (value < 0) {
			throw new InvalidParameterException(Messages.Exception.NEGATIVE_RESOURCE_ITEM_VALUE);
		}
	}

	private void validateComputeFieldsLength(String[] fields) throws InvalidParameterException {
		if (fields.length != 4) {
			throw new InvalidParameterException(Messages.Exception.INVALID_NUMBER_OF_COMPUTE_ITEM_FIELDS);
		}
	}
	
	private void extractVolumeItem(Map<ResourceItem, Double> plan, String[] fields) 
			throws InvalidParameterException {
		try {
			validateVolumeFieldsLength(fields);
			
			int size = Integer.parseInt(fields[VOLUME_SIZE_FIELD_INDEX]);
			double value = Double.parseDouble(fields[VOLUME_VALUE_FIELD_INDEX]);
			ResourceItem newItem = new VolumeItem(size);
			
			validateItemValue(value);
			
			plan.put(newItem, value);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_VOLUME_ITEM_FIELD);
		}
	}

	private void validateVolumeFieldsLength(String[] fields) throws InvalidParameterException {
		if (fields.length != 3) {
			throw new InvalidParameterException(Messages.Exception.INVALID_NUMBER_OF_VOLUME_ITEM_FIELDS);
		}
	}
	
	public Map<String, String> getRulesAsMap() {
		return basePlan;
	}

	// TODO discuss how this operation should be performed
	public void update(Map<String, String> planInfo) throws InvalidParameterException {
		Map<ResourceItem, Double> newPlan = validatePlanInfo(planInfo);
		this.plan = newPlan;
	}

	public Double getItemFinancialValue(ResourceItem resourceItem) throws InvalidParameterException {
		if (plan.containsKey(resourceItem)) { 
			return plan.get(resourceItem);	
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_RESOURCE_ITEM, 
				resourceItem.toString()));
	}
}
