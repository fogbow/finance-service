package cloud.fogbow.fs.core.plugins;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;

public class DebtsPaymentChecker {

    private InMemoryUsersHolder usersHolder;
    
    public DebtsPaymentChecker(InMemoryUsersHolder usersHolder) {
        this.usersHolder = usersHolder;
    }
    
    public boolean hasPaid(String userId, String provider) 
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = usersHolder.getUserById(userId, provider);
        
        synchronized (user) {
            List<String> debts = user.getLastSubscriptionsDebts();
            List<Invoice> userInvoices = user.getInvoices();
            
            if (debts.isEmpty()) {
                return true;
            }
            
            for (Invoice invoice : userInvoices) {
                if (debts.contains(invoice.getInvoiceId())) {
                    if (invoice.getState().equals(InvoiceState.DEFAULTING)) { 
                        return false;
                    }
                }
            }

            return true;    
        }
    }
}
