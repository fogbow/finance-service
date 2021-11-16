package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class InvoiceManager {
    private InMemoryUsersHolder userHolder;
    private RecordUtils recordUtils;
    private InvoiceBuilder invoiceBuilder;
    private FinancePolicy policy;
    private TimeUtils timeUtils;
    
    public InvoiceManager(InMemoryUsersHolder userHolder, FinancePolicy policy) {
        this.userHolder = userHolder;
        this.recordUtils = new RecordUtils();
        this.invoiceBuilder = new InvoiceBuilder();
        this.timeUtils = new TimeUtils();
        this.policy = policy;
    }

    public InvoiceManager(InMemoryUsersHolder userHolder, RecordUtils resourceItemFactory,
            InvoiceBuilder invoiceBuilder, FinancePolicy policy, TimeUtils timeUtils) {
        this.userHolder = userHolder;
        this.recordUtils = resourceItemFactory;
        this.invoiceBuilder = invoiceBuilder;
        this.policy = policy;
        this.timeUtils = timeUtils;
    }
    
    public boolean hasPaid(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);

        synchronized(user) {
            for (Invoice invoice : user.getInvoices()) {
                if (invoice.getState().equals(InvoiceState.DEFAULTING)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public void generateInvoiceForUser(String userId, String provider, 
            Long paymentStartTime, Long paymentEndTime, List<Record> records) 
                    throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);
        
        synchronized(user) {
            synchronized(policy) {
                boolean lastInvoice = false;
                generateInvoiceAndUpdateUser(userId, provider, paymentStartTime, paymentEndTime, records, user, lastInvoice);
            }
        }
    }

    public void generateLastInvoiceForUser(String userId, String provider, Long paymentStartTime, 
            Long paymentEndTime, List<Record> records) 
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);
        
        synchronized(user) {
            synchronized(policy) {
                boolean lastInvoice = true;
                generateInvoiceAndUpdateUser(userId, provider, paymentStartTime, paymentEndTime, records, user, lastInvoice);
            }
        }        
    }

    private void generateInvoiceAndUpdateUser(String userId, String provider, Long paymentStartTime,
            Long paymentEndTime, List<Record> records, FinanceUser user, boolean lastInvoice)
            throws InternalServerErrorException, InvalidParameterException {
        Invoice invoice = generateInvoice(userId, provider, paymentStartTime, paymentEndTime, records);
        
        if (lastInvoice) {
            user.addInvoiceAsDebt(invoice);                
        } else { 
            user.addInvoice(invoice);
        }
        
        user.setLastBillingTime(paymentEndTime);
        this.userHolder.saveUser(user);
    }
    
    private Invoice generateInvoice(String userId, String provider, Long paymentStartTime, Long paymentEndTime,
            List<Record> records) throws InternalServerErrorException {
        this.invoiceBuilder.setUserId(userId);
        this.invoiceBuilder.setProviderId(provider);
        this.invoiceBuilder.setStartTime(paymentStartTime);
        this.invoiceBuilder.setEndTime(paymentEndTime);
        
        for (Record record : records) {
            addRecordToInvoice(record, policy, paymentStartTime, paymentEndTime);
        }
        
        Invoice invoice = invoiceBuilder.buildInvoice();
        invoiceBuilder.reset();
        return invoice;
    }
    
    private void addRecordToInvoice(Record record, FinancePolicy plan, 
            Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException {
        try {
            ResourceItem resourceItem = recordUtils.getItemFromRecord(record);
            NavigableMap<Timestamp, OrderState> stateHistory = recordUtils.getRecordStateHistoryOnPeriod(record, paymentStartTime, paymentEndTime);
            Iterator<Timestamp> timestampsIterator = stateHistory.navigableKeySet().iterator();
            
            Timestamp periodLowerLimit = null;
            Timestamp periodHigherLimit = null;
            periodHigherLimit = timestampsIterator.next();
            
            do {
                periodLowerLimit = periodHigherLimit;
                periodHigherLimit = timestampsIterator.next();
                OrderState periodState = stateHistory.get(periodLowerLimit);
                processPeriod(periodLowerLimit, periodHigherLimit, periodState, resourceItem);
            } while (timestampsIterator.hasNext());

        } catch (InvalidParameterException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
    
    private void processPeriod(Timestamp periodLowerLimit, Timestamp periodHigherLimit, 
            OrderState periodState, ResourceItem resourceItem) throws InvalidParameterException {
        Long realTimeSpentOnState = periodHigherLimit.getTime() - periodLowerLimit.getTime();
        Double roundUpTimeSpentOnState = getRoundUpTimeSpentOnState(resourceItem, periodState, realTimeSpentOnState);
        
        Double financialValue = policy.getItemFinancialValue(resourceItem, periodState);
        
        invoiceBuilder.addItem(resourceItem, periodState, financialValue, roundUpTimeSpentOnState);
    }
    
    private Double getRoundUpTimeSpentOnState(ResourceItem resourceItem, OrderState state, Long realTimeSpentOnState) throws InvalidParameterException {
        TimeUnit timeUnit = policy.getItemFinancialTimeUnit(resourceItem, state);
        Long convertedTime = this.timeUtils.roundUpTimePeriod(realTimeSpentOnState, timeUnit);
        return new Double(convertedTime);
    }

    public void setPlan(FinancePolicy financePlan) {
        this.policy = financePlan;
    }
}
