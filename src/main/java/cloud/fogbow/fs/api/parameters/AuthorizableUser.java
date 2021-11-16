package cloud.fogbow.fs.api.parameters;

import com.google.gson.Gson;

import cloud.fogbow.fs.constants.ApiDocumentation;
import cloud.fogbow.ras.core.models.RasOperation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AuthorizableUser {
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.USER_TOKEN)
	private String userToken;
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.OPERATION)
	private String operation;
	
	public AuthorizableUser() {
		
	}
	
	public AuthorizableUser(String userToken, String operation) {
		this.userToken = userToken;
		this.operation = operation;
	}
	
	public String getUserToken() {
		return userToken;
	}
	
	public RasOperation getOperation() {
		return new Gson().fromJson(operation, RasOperation.class);
	}
}
