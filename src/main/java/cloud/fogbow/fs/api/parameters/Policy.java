package cloud.fogbow.fs.api.parameters;

import cloud.fogbow.fs.constants.ApiDocumentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Policy {
    @ApiModelProperty(example = ApiDocumentation.Model.POLICY, notes = ApiDocumentation.Model.POLICY_NOTE)
    private String policy;
    
    public String getPolicy() {
        return policy;
    }
}
