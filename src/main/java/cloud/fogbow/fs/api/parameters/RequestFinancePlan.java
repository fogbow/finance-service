package cloud.fogbow.fs.api.parameters;

import java.util.Map;

// FIXME change class name to FinancePlan
public class RequestFinancePlan {
	private String pluginClassName;
	private String financePlanName;
	private Map<String, String> planInfo;
	
	public RequestFinancePlan() {
		
	}
	
	public RequestFinancePlan(String pluginClassName, String financePlanName, 
	        Map<String, String> planInfo) {
		this.pluginClassName = pluginClassName;
		this.planInfo = planInfo;
	}

    public String getPluginClassName() {
		return pluginClassName;
	}
    
    public String getFinancePlanName() {
        return financePlanName;
    }
	
	public Map<String, String> getPlanInfo() {
		return planInfo;
	}
}
