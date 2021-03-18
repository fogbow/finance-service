package cloud.fogbow.fs.core.models;

import java.util.Map;

public class PaymentPlan {

	private Map<String, String> planMap;
	
	public String getPlanValue(String name) {
		return planMap.get(name);
	}
}
