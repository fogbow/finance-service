package cloud.fogbow.fs.api.http.response;

import java.util.Map;

public class FinancePlan {
    private String planName;
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
