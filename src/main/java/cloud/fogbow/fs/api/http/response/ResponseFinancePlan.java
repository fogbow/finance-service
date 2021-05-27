package cloud.fogbow.fs.api.http.response;

import java.util.Map;

// FIXME change name to FinancePlan
public class ResponseFinancePlan {
    private String planName;
    private Map<String, String> planInfo;
    
    public ResponseFinancePlan(String planName, Map<String, String> planInfo) {
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
