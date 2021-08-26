package cloud.fogbow.fs.api.parameters;

import java.util.Map;

// TODO documentation
public class FinancePlan {
	private String pluginClassName;
	private String financePlanName;
	private Map<String, String> planInfo;
	
	public FinancePlan() {
		
	}
	
	public FinancePlan(String pluginClassName, String financePlanName, 
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
