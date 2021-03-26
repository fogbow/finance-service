package cloud.fogbow.fs.core.plugins.payment.postpaid;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
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
	
	public DefaultInvoiceManager(DatabaseManager databaseManager, String planName) {
		this.planName = planName;
		this.databaseManager = databaseManager;
		this.resourceItemFactory = new ResourceItemFactory();
	}

	@Override
	public boolean hasPaid(String userId, String provider) {
		List<Invoice> userInvoices = databaseManager.getInvoiceByUserId(userId, provider);
		
		for (Invoice invoice : userInvoices) {
			if (invoice.getState().equals(InvoiceState.DEFAULTING)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void startPaymentProcess(String userId, String provider) throws InternalServerErrorException {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		FinancePlan plan = databaseManager.getFinancePlan(planName);
		List<Record> records = user.getPeriodRecords();
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder(userId, provider);
		
		for (Record record : records) {
			ResourceItem resourceItem;
			
			try {
				resourceItem = resourceItemFactory.getItemFromRecord(record);
			} catch (InvalidParameterException e) {
				throw new InternalServerErrorException(e.getMessage());
			}
			
			Double valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
			Double timeUsed = resourceItemFactory.getTimeFromRecord(record);
			
			invoiceBuilder.addItem(resourceItem, valueToPayPerTimeUnit, timeUsed);
		}
		
		Invoice invoice = invoiceBuilder.buildInvoice();
		databaseManager.saveInvoice(invoice);
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		String propertyValue = "";
		
		// FIXME constant
		if (property.equals("ALL_USER_INVOICES")) {
			List<Invoice> userInvoices = databaseManager.getInvoiceByUserId(userId, provider);
			List<String> invoiceJsonReps = new ArrayList<String>();
			
			for (Invoice invoice : userInvoices) {
				String invoiceJson = invoice.jsonRepr();
				invoiceJsonReps.add(invoiceJson);
			}
			
			// FIXME constant
			propertyValue = String.join(",", invoiceJsonReps);
		} else {
			// FIXME treat this
		}
		
		return propertyValue;
	}

	@Override
	public void setFinancePlan(String planName) {
		this.planName = planName;
	}
}
