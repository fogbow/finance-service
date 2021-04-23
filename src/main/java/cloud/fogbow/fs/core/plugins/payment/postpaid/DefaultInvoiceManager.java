package cloud.fogbow.fs.core.plugins.payment.postpaid;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.payment.ResourceItem;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class DefaultInvoiceManager implements PaymentManager {
	public static final String PROPERTY_VALUES_SEPARATOR = ",";
	public static final String ALL_USER_INVOICES_PROPERTY_NAME = "ALL_USER_INVOICES";
	
	private String planName;
	private InMemoryFinanceObjectsHolder objectHolder;
	private RecordUtils resourceItemFactory;
	private InvoiceBuilder invoiceBuilder;
	
    public DefaultInvoiceManager(InMemoryFinanceObjectsHolder objectHolder, String planName) {
        this.objectHolder = objectHolder;
        this.planName = planName;
        this.resourceItemFactory = new RecordUtils();
        this.invoiceBuilder = new InvoiceBuilder();
    }

    public DefaultInvoiceManager(InMemoryFinanceObjectsHolder objectHolder, String planName, RecordUtils resourceItemFactory,
            InvoiceBuilder invoiceBuilder) {
        this.objectHolder = objectHolder;
        this.planName = planName;
        this.resourceItemFactory = resourceItemFactory;
        this.invoiceBuilder = invoiceBuilder;
    }
	
	@Override
	public boolean hasPaid(String userId, String provider) {
	    try {
	        MultiConsumerSynchronizedList<Invoice> userInvoices = this.objectHolder.getInvoiceByUserId(userId, provider);
            Integer consumerId = userInvoices.startIterating();
	        
	        try {
                Invoice invoice = userInvoices.getNext(consumerId);
                
                while (invoice != null) {
                    if (invoice.getState().equals(InvoiceState.DEFAULTING)) {
                        return false;
                    } 
                    
                    invoice = userInvoices.getNext(consumerId);
                }
	        } finally {
	            userInvoices.stopIterating(consumerId);
	        }
            // TODO treat these exceptions
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        } catch (InternalServerErrorException e) {
            e.printStackTrace();
        }
	    
	    return true;
	}

	@Override
	public void startPaymentProcess(String userId, String provider, 
	        Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException, InvalidParameterException {
	    FinanceUser user = this.objectHolder.getUserById(userId, provider);
	    
	    synchronized(user) {
	        FinancePlan plan = this.objectHolder.getFinancePlan(planName);
	        List<Record> records = user.getPeriodRecords();
	        this.invoiceBuilder.setUserId(userId);
	        this.invoiceBuilder.setProviderId(provider);
	        
	        // TODO What is the expected behavior for the empty records list case? 
	        for (Record record : records) {
	            addRecordToInvoice(record, plan, paymentStartTime, paymentEndTime);
	        }
	        
	        Invoice invoice = invoiceBuilder.buildInvoice();
	        invoiceBuilder.reset();
	        
	        this.objectHolder.registerInvoice(invoice);
	    }
	}
	
	private void addRecordToInvoice(Record record, FinancePlan plan, 
            Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException {
		try {
			ResourceItem resourceItem = resourceItemFactory.getItemFromRecord(record);
			Double valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
			Double timeUsed = resourceItemFactory.getTimeFromRecord(record, paymentStartTime, paymentEndTime);
			
			invoiceBuilder.addItem(resourceItem, valueToPayPerTimeUnit, timeUsed);
		} catch (InvalidParameterException e) {
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException {
		String propertyValue = "";
		
		if (property.equals(ALL_USER_INVOICES_PROPERTY_NAME)) {
		    MultiConsumerSynchronizedList<Invoice> userInvoices;
		    
            try {
                userInvoices = this.objectHolder.getInvoiceByUserId(userId, provider);
                List<String> invoiceJsonReps = new ArrayList<String>();
                
                Integer consumerId = userInvoices.startIterating();
                
                
                try {
                    Invoice invoice = userInvoices.getNext(consumerId);
                    
                    while (invoice != null) {
                        synchronized(invoice) {
                            String invoiceJson = invoice.toString();
                            invoiceJsonReps.add(invoiceJson);    
                        }
                        
                        invoice = userInvoices.getNext(consumerId);
                    }
                } finally {
                    userInvoices.stopIterating(consumerId);
                }
                
                propertyValue = "[" + String.join(PROPERTY_VALUES_SEPARATOR, invoiceJsonReps) + "]";
                // TODO treat these exceptions
            } catch (InvalidParameterException e) {
                e.printStackTrace();
            } catch (InternalServerErrorException e) {
                e.printStackTrace();
            }
		} else {
			throw new InvalidParameterException(
					String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
		}
		
		return propertyValue;
	}

	@Override
	public void setFinancePlan(String planName) throws InvalidParameterException {
	    this.objectHolder.getFinancePlan(planName);
	    
		this.planName = planName;
	}
}
