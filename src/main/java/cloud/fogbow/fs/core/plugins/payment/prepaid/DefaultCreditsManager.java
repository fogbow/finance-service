package cloud.fogbow.fs.core.plugins.payment.prepaid;

import java.util.List;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
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
	public void startPaymentProcess(String userId, String provider) throws InternalServerErrorException {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		UserCredits credits = databaseManager.getUserCreditsByUserId(userId);
		FinancePlan plan = databaseManager.getFinancePlan(planName);
		List<Record> records = user.getPeriodRecords();
		
		for (Record record : records) {
			ResourceItem resourceItem;
			
			try {
				resourceItem = resourceItemFactory.getItemFromRecord(record);
			} catch (InvalidParameterException e) {
				throw new InternalServerErrorException(e.getMessage());
			}
			
			Double valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
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

	@Override
	public void setFinancePlan(String planName) {
		// TODO Auto-generated method stub
		
	}

}
