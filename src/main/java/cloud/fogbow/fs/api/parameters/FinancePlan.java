package cloud.fogbow.fs.api.parameters;

import java.util.Map;

import cloud.fogbow.fs.constants.ApiDocumentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FinancePlan {
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PLUGIN_CLASS_NAME)
	private String pluginClassName;
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PLAN_NAME)
	private String financePlanName;
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PLAN_INFO)
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
