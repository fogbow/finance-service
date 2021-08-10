package cloud.fogbow.fs.core.plugins.plan.prepaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class CreditsManager {
    private RecordUtils recordUtils;
    private InMemoryUsersHolder usersHolder;
    private FinancePolicy policy;
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePolicy plan) {
        this.usersHolder = usersHolder;
        this.policy = plan;
        this.recordUtils = new RecordUtils();
    }
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePolicy policy, RecordUtils recordUtils) {
        this.usersHolder = usersHolder;
        this.policy = policy;
        this.recordUtils = recordUtils;
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
						Map<OrderState, Double> timeSpentOnStates = recordUtils.getTimeFromRecordPerState(record,
								paymentStartTime, paymentEndTime);

						for (OrderState state : timeSpentOnStates.keySet()) {
							Double valueToPayPerTimeUnit = policy.getItemFinancialValue(resourceItem, state);
							Double timeUsed = timeSpentOnStates.get(state);
							credits.deduct(resourceItem, valueToPayPerTimeUnit, timeUsed);
						}
					} catch (InvalidParameterException e) {
						throw new InternalServerErrorException(e.getMessage());
					}                	
                }

                user.setLastBillingTime(paymentEndTime);
                this.usersHolder.saveUser(user);
            }
        }
    }
}
