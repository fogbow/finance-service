package cloud.fogbow.fs.api.parameters;

import java.util.Map;

public class RequestFinancePlan {
	private String pluginClassName;
	private Map<String, String> planInfo;
	
	public RequestFinancePlan() {
		
	}
	
	public RequestFinancePlan(String pluginClassName, Map<String, String> planInfo) {
		this.pluginClassName = pluginClassName;
		this.planInfo = planInfo;
	}

	public String getPluginClassName() {
		return pluginClassName;
	}
	
	public Map<String, String> getPlanInfo() {
		return planInfo;
	}
}
