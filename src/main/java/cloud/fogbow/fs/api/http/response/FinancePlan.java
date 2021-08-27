package cloud.fogbow.fs.api.http.response;

import java.util.Map;

import cloud.fogbow.fs.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class FinancePlan {
    @ApiModelProperty(example = ApiDocumentation.Model.PLAN_NAME)
    private String planName;
    @ApiModelProperty(example = ApiDocumentation.Model.PLAN_INFO)
    private Map<String, String> planInfo;
    
    public FinancePlan(String planName, Map<String, String> planInfo) {
        this.planName = planName;
        this.planInfo = planInfo;
    }

    public String getPlanName() {
        return planName;
    }

    public Map<String, String> getPlanInfo() {
        return planInfo;
    }
}
