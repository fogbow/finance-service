package cloud.fogbow.fs.core.plugins.plan.prepaid;

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
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class CreditsManager {
    private RecordUtils recordUtils;
    private InMemoryUsersHolder usersHolder;
    private FinancePolicy policy;
    private TimeUtils timeUtils;
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePolicy plan) {
        this.usersHolder = usersHolder;
        this.policy = plan;
        this.recordUtils = new RecordUtils();
        this.timeUtils = new TimeUtils();
    }
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePolicy policy, RecordUtils recordUtils, 
            TimeUtils timeUtils) {
        this.usersHolder = usersHolder;
        this.policy = policy;
        this.recordUtils = recordUtils;
        this.timeUtils = timeUtils;
    }
    
    public boolean hasPaid(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = this.usersHolder.getUserById(userId, provider);
        
        synchronized(user) {
            UserCredits credits = user.getCredits();
            return credits.getCreditsValue() >= 0.0;
        }
    }

    public void startPaymentProcess(String userId, String provider, 
            Long paymentStartTime, Long paymentEndTime, List<Record> records) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(userId, provider);
        
        synchronized (user) {
            synchronized (policy) {
                UserCredits credits = user.getCredits();
                
                for (Record record : records) {
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
			                processPeriod(periodLowerLimit, periodHigherLimit, periodState, resourceItem, credits);
			            } while (timestampsIterator.hasNext());
					} catch (InvalidParameterException e) {
						throw new InternalServerErrorException(e.getMessage());
					}                	
                }

                user.setLastBillingTime(paymentEndTime);
                this.usersHolder.saveUser(user);
            }
        }
    }
    
    private void processPeriod(Timestamp periodLowerLimit, Timestamp periodHigherLimit, OrderState periodState,
            ResourceItem resourceItem, UserCredits credits) throws InvalidParameterException {
        Long realTimeSpentOnState = periodHigherLimit.getTime() - periodLowerLimit.getTime();
        Double roundUpTimeSpentOnState = getRoundUpTimeSpentOnState(resourceItem, periodState, realTimeSpentOnState);

        Double financialValue = policy.getItemFinancialValue(resourceItem, periodState);

        credits.deduct(resourceItem, financialValue, roundUpTimeSpentOnState);
    }
    
    private Double getRoundUpTimeSpentOnState(ResourceItem resourceItem, OrderState state, Long realTimeSpentOnState) throws InvalidParameterException {
        TimeUnit timeUnit = policy.getItemFinancialTimeUnit(resourceItem, state);
        Long convertedTime = this.timeUtils.roundUpTimePeriod(realTimeSpentOnState, timeUnit);
        return new Double(convertedTime);
    }
}
