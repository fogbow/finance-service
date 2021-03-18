package cloud.fogbow.fs.core.plugins.payment.postpaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class DefaultPostPaidPaymentManager implements PaymentManager {
	public static final String PAYMENT_STATUS_OK = "payment_ok";
	public static final String PAYMENT_STATUS_WAITING = "payment_waiting";
	public static final String PAYMENT_STATUS_DEFAULTING = "payment_defaulting";
	
	private String planName;
	private DatabaseManager databaseManager;
	
	@Override
	public boolean hasPaid(String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startPaymentProcess(String userId) {
		FinanceUser user = databaseManager.getUserById(userId);
		
		Map<String, String> basePlan = databaseManager.getPlan(planName);
		Map<PostPaidResourceItem, Double> plan = getPlan(basePlan);
		List<Record> records = user.getPeriodRecords();
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder(userId);
		
		for (Record record : records) {
			PostPaidResourceItem resourceItem = getItemFromRecord(record);
			Double valueToPayPerTimeUnit = plan.get(resourceItem);
			Double timeUsed = getTimeFromRecord(record);
			
			invoiceBuilder.addItem(resourceItem, valueToPayPerTimeUnit, timeUsed);
		}
		
		Invoice invoice = invoiceBuilder.buildInvoice();
		
		// add invoice to database
		
	}

	private Double getTimeFromRecord(Record record) {
		// TODO Auto-generated method stub
		return null;
	}

	private PostPaidResourceItem getItemFromRecord(Record record) {
		// TODO Auto-generated method stub
		return null;
	}

	private Map<PostPaidResourceItem, Double> getPlan(Map<String, String> basePlan) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserFinanceState(String userId, String property) {
		// TODO Auto-generated method stub
		return null;
	}

}
