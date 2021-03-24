package cloud.fogbow.fs.core.plugins.payment.prepaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItemFactory;

public class DefaultCreditsManager implements PaymentManager {

	private DatabaseManager databaseManager;
	private ResourceItemFactory resourceItemFactory;
	private String planName;

	@Override
	public boolean hasPaid(String userId, String provider) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startPaymentProcess(String userId, String provider) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		UserCredits credits = databaseManager.getUserCreditsByUserId(userId);
		
		Map<String, String> basePlan = databaseManager.getPlan(planName);
		Map<ResourceItem, Double> plan = resourceItemFactory.getPlan(basePlan);
		List<Record> records = user.getPeriodRecords();
		
		for (Record record : records) {
			ResourceItem resourceItem = resourceItemFactory.getItemFromRecord(record);
			Double valueToPayPerTimeUnit = plan.get(resourceItem);
			Double timeUsed = resourceItemFactory.getTimeFromRecord(record);
			
			credits.deduct(resourceItem, valueToPayPerTimeUnit, timeUsed);
		}
		
		databaseManager.saveUserCredits(credits);
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		// TODO Auto-generated method stub
		return null;
	}

}
