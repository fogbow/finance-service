package cloud.fogbow.fs.core.util;

import java.util.ArrayList;

import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.plan.prepaid.UserCreditsFactory;

public class FinanceUserFactory {

    private UserCreditsFactory userCreditsFactory;
    
    public FinanceUserFactory(UserCreditsFactory userCreditsFactory) {
        this.userCreditsFactory = userCreditsFactory;
    }
    
    public FinanceUser getUser(String userId, String provider) {
        return new FinanceUser(userId, provider, userCreditsFactory.getUserCredits(userId, provider), 
                new ArrayList<Invoice>());
    }
}
