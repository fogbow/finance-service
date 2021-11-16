package cloud.fogbow.fs.api.parameters;

import cloud.fogbow.fs.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class User {
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.USER_ID)
	private String userId;
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PROVIDER_ID)
	private String provider;
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PLAN_NAME)
	private String financePlanName;
	
	public User() {
		
	}
	
    public User(String userId, String provider, String financePlanName) {
		this.userId = userId;
		this.provider = provider;
		this.financePlanName = financePlanName;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public String getFinancePlanName() {
		return financePlanName;
	}
}
