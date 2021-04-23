package cloud.fogbow.fs.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.util.Pair;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.payment.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

public class InMemoryFinanceObjectsHolder {
    private DatabaseManager databaseManager;
    private UserCreditsFactory userCreditsFactory;
    private MultiConsumerSynchronizedListFactory listFactory;
    private Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin;
    private Map<Pair<String, String>, MultiConsumerSynchronizedList<Invoice>> invoicesByUser;
    private MultiConsumerSynchronizedList<UserCredits> userCredits;
    private MultiConsumerSynchronizedList<FinancePlan> financePlans;
    
    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager) throws InternalServerErrorException {
        this(databaseManager, new MultiConsumerSynchronizedListFactory(), 
                new UserCreditsFactory());
    }
    
    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager, 
            MultiConsumerSynchronizedListFactory listFactory, UserCreditsFactory userCreditsFactory) throws InternalServerErrorException {
        this.userCreditsFactory = userCreditsFactory;
        this.databaseManager = databaseManager;
        this.listFactory = listFactory;

        // read users data
        List<FinanceUser> databaseUsers = this.databaseManager.getRegisteredUsers();
        usersByPlugin = new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        
        for (FinanceUser user : databaseUsers) {
            addUserByPlugin(user);
        }
        
        // read invoices data
        List<Invoice> databaseInvoices = this.databaseManager.getRegisteredInvoices();
        invoicesByUser = new HashMap<Pair<String, String>, MultiConsumerSynchronizedList<Invoice>>();
        
        for (Invoice invoice : databaseInvoices) {
            addInvoiceByUser(invoice);
        }
        
        // read credits data
        List<UserCredits> databaseUserCredits = this.databaseManager.getRegisteredUserCredits();
        userCredits = listFactory.getList();
        
        for (UserCredits credits : databaseUserCredits) {
            userCredits.addItem(credits);
        }
 
        // read finance plans data
        List<FinancePlan> databaseFinancePlans = this.databaseManager.getRegisteredFinancePlans();
        financePlans = listFactory.getList();
        
        for (FinancePlan plan : databaseFinancePlans) {
            financePlans.addItem(plan);
        }
    }

    /*
     * 
     * User methods
     * 
     */

    public void registerUser(String userId, String provider, String pluginName, Map<String, String> financeOptions) throws InternalServerErrorException {
        FinanceUser user = new FinanceUser(financeOptions);

        user.setId(userId);
        user.setProvider(provider);
        user.setFinancePluginName(pluginName);
        
        addUserByPlugin(user);
        
        this.databaseManager.saveUser(user);
    }
    
    private void addUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        String pluginName = user.getFinancePluginName();
        
        if (!usersByPlugin.containsKey(pluginName)) {
            usersByPlugin.put(pluginName, this.listFactory.getList());
        }
        
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(pluginName);
        pluginUsers.addItem(user);
    }
    
    public void saveUser(FinanceUser user) throws InvalidParameterException {
        getUserById(user.getId(), user.getProvider());
        
        synchronized(user) {
            this.databaseManager.saveUser(user);
        }
    }
    
    public void removeUser(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        removeUserByPlugin(getUserById(userId, provider));
        this.databaseManager.removeUser(userId, provider);
    }
    
    private void removeUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(user.getFinancePluginName());
        pluginUsers.removeItem(user);
    }
    
    public FinanceUser getUserById(String id, String provider) throws InvalidParameterException {
        for (String pluginName : usersByPlugin.keySet()) {
            MultiConsumerSynchronizedList<FinanceUser> users = usersByPlugin.get(pluginName);
            Integer consumerId = users.startIterating();
            try {
                FinanceUser item = users.getNext(consumerId);
                while (item != null) {
                    if (item.getId().equals(id) && 
                            item.getProvider().equals(provider)) {
                        return item;
                    }
                    
                    item = users.getNext(consumerId);
                }
            } finally {
                users.stopIterating(consumerId);
            }
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider)); 
    }
    
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return new MultiConsumerSynchronizedList<FinanceUser>();
        }
    }

    public void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException {
        FinanceUser user = getUserById(userId, provider);
        
        synchronized(user) {
            for (String option : financeOptions.keySet()) {
                user.setProperty(option, financeOptions.get(option));
            }
            
            this.databaseManager.changeOptions(user, financeOptions);
        }
    }

    /*
     * 
     * Invoice methods
     * 
     */
    
    public void registerInvoice(Invoice invoice) {
        this.addInvoiceByUser(invoice);
        this.databaseManager.saveInvoice(invoice);
    }
    
    private void addInvoiceByUser(Invoice invoice) {
        Pair<String, String> invoiceUserId = Pair.of(invoice.getUserId(), 
                invoice.getProviderId());
        
        if (!invoicesByUser.containsKey(invoiceUserId)) {
            invoicesByUser.put(invoiceUserId, listFactory.getList());
        }
        
        invoicesByUser.get(invoiceUserId).addItem(invoice);
    }
    
    public void saveInvoice(Invoice invoice) {
        this.databaseManager.saveInvoice(invoice);
    }

    public Invoice getInvoice(String invoiceId) throws InvalidParameterException {
        for (Pair<String, String> invoiceUserId : this.invoicesByUser.keySet()) {
            MultiConsumerSynchronizedList<Invoice> invoicesList = this.invoicesByUser.get(invoiceUserId);
            
            Integer consumerId = invoicesList.startIterating();
            
            try {
                Invoice item = invoicesList.getNext(consumerId);
                
                while (item != null) {
                    if (item.getInvoiceId().equals(invoiceId)) {
                        return item;
                    }
                    
                    item = invoicesList.getNext(consumerId);
                }
            } finally {
                invoicesList.stopIterating(consumerId);
            }
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_INVOICE, invoiceId));
    }

    public MultiConsumerSynchronizedList<Invoice> getInvoiceByUserId(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        if (invoicesByUser.containsKey(Pair.of(userId, provider))) {
            return invoicesByUser.get(Pair.of(userId, provider));
        } else {
            return this.listFactory.getList();
        }
    }
    
    /*
     * 
     * UserCredits methods
     * 
     */
    
    public void registerUserCredits(String userId, String provider) throws InternalServerErrorException {
        UserCredits credits = this.userCreditsFactory.getUserCredits(userId, provider);
        this.userCredits.addItem(credits);
        this.databaseManager.saveUserCredits(credits);
    }
    
    public void saveUserCredits(UserCredits credits) throws InternalServerErrorException {
        this.databaseManager.saveUserCredits(credits);
    }
    
    public UserCredits getUserCreditsByUserId(String userId, String provider) throws InvalidParameterException {
        Integer consumerId = userCredits.startIterating();
        
        try {
            UserCredits item = userCredits.getNext(consumerId);
        
            while (item != null) {
                if (item.getUserId().equals(userId) && 
                        item.getProvider().equals(provider)) {
                    return item;
                }
                
                item = userCredits.getNext(consumerId);
            }
            
        } finally {
            userCredits.stopIterating(consumerId);
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER_CREDITS, userId, provider));
    }

    /*
     * 
     * FinancePlans methods
     * 
     */
    
    public void registerFinancePlan(FinancePlan financePlan) {
        this.financePlans.addItem(financePlan);
        this.databaseManager.saveFinancePlan(financePlan);
    }
    
    public void saveFinancePlan(FinancePlan financePlan) {
        this.databaseManager.saveFinancePlan(financePlan);
    }

    public FinancePlan getFinancePlan(String planName) throws InvalidParameterException {
        Integer consumerId = financePlans.startIterating();
        
        try {
            FinancePlan item = financePlans.getNext(consumerId);
            
            while (item != null) {
                if (item.getName().equals(planName)) {
                    return item;
                }
                
                item = financePlans.getNext(consumerId);
            }
        } finally {
            financePlans.stopIterating(consumerId);
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, planName));
    }

    public void removeFinancePlan(String planName) throws InvalidParameterException {
        this.financePlans.removeItem(getFinancePlan(planName));
        this.databaseManager.removeFinancePlan(planName);
    }
}
