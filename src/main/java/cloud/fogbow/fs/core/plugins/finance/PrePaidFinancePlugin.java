package cloud.fogbow.fs.core.plugins.finance;

import java.util.Map;

import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class PrePaidFinancePlugin implements FinancePlugin {

	@Override
	public void startThreads() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopThreads() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isAuthorized(String userId, Map<String, String> operationParameters) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean managesUser(String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getUserFinanceState(String userId, String property) {
		// TODO Auto-generated method stub
		return null;
	}

}
