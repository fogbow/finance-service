package cloud.fogbow.fs.core.util;

import java.util.Map;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinancePolicy;

public class FinancePolicyFactory {
    
    public FinancePolicy createFinancePolicy(String planName, Map<String, String> planInfo) 
            throws InvalidParameterException {
        return new FinancePolicy(planName, planInfo); 
    }
    
    public FinancePolicy createFinancePolicy(String planName, String planInfoFilePath) 
            throws InvalidParameterException {
        return new FinancePolicy(planName, planInfoFilePath); 
    }
}
