package cloud.fogbow.fs.core.plugins.payment.postpaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.plugins.payment.ResourceItemFactory;

public class DefaultInvoiceManager implements PaymentManager {
	public static final String PAYMENT_STATUS_OK = "payment_ok";
	public static final String PAYMENT_STATUS_WAITING = "payment_waiting";
	public static final String PAYMENT_STATUS_DEFAULTING = "payment_defaulting";
	
	private String planName;
	private DatabaseManager databaseManager;
	private ResourceItemFactory resourceItemFactory;
	
	@Override
	public boolean hasPaid(String userId, String provider) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startPaymentProcess(String userId, String provider) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		
		Map<String, String> basePlan = databaseManager.getPlan(planName);
		Map<ResourceItem, Double> plan = resourceItemFactory.getPlan(basePlan);
		List<Record> records = user.getPeriodRecords();
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder(userId);
		
		for (Record record : records) {
			ResourceItem resourceItem = resourceItemFactory.getItemFromRecord(record);
			Double valueToPayPerTimeUnit = plan.get(resourceItem);
			Double timeUsed = resourceItemFactory.getTimeFromRecord(record);
			
			invoiceBuilder.addItem(resourceItem, valueToPayPerTimeUnit, timeUsed);
		}
		
		Invoice invoice = invoiceBuilder.buildInvoice();
		databaseManager.saveInvoice(invoice);
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		// TODO Auto-generated method stub
		return null;
	}

}
