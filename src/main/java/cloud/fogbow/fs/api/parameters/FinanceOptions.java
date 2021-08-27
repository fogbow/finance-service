package cloud.fogbow.fs.api.parameters;

import java.util.HashMap;

import cloud.fogbow.fs.constants.ApiDocumentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FinanceOptions {
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.PLAN_INFO) 
    private HashMap<String, String> financeOptions;

    public FinanceOptions() {
        
    }
    
    public FinanceOptions(HashMap<String, String> financeOptions) {
        this.financeOptions = financeOptions;
    }

    public HashMap<String, String> getFinanceOptions() {
        return financeOptions;
    }
}
