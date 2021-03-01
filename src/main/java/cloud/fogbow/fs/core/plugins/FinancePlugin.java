package cloud.fogbow.fs.core.plugins;

import java.util.Map;

// TODO documentation
public interface FinancePlugin {
	// TODO documentation
	void startThreads();
	// TODO documentation
	void stopThreads();
	// TODO documentation
	boolean isAuthorized(String userId, Map<String, String> operationParameters);
	// TODO documentation
	boolean managesUser(String userId);
	// TODO documentation
	String getUserFinanceState(String userId, String property);
}
