package cloud.fogbow.fs.api.parameters;

import cloud.fogbow.common.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class PublicKey {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.PUBLIC_KEY)
    private String publicKey;
    
    public String getPublicKey() {
        return publicKey;
    }
}
