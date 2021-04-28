package cloud.fogbow.fs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.payment.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

public class InMemoryFinanceObjectsHolder {
    private DatabaseManager databaseManager;
    private UserCreditsFactory userCreditsFactory;
    private MultiConsumerSynchronizedListFactory listFactory;
    private Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin;
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
        user.setCredits(userCreditsFactory.getUserCredits(userId, provider));
        user.setInvoices(new ArrayList<Invoice>());
        
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
        FinanceUser userToRemove = getUserById(userId, provider);
        
        synchronized(userToRemove) {
            removeUserByPlugin(userToRemove);
            this.databaseManager.removeUser(userId, provider);
        }
    }
    
    private void removeUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(user.getFinancePluginName());
        pluginUsers.removeItem(user);
    }
    
    public FinanceUser getUserById(String id, String provider) throws InvalidParameterException {
        for (String pluginName : usersByPlugin.keySet()) {
            MultiConsumerSynchronizedList<FinanceUser> users = usersByPlugin.get(pluginName);
            Integer consumerId = users.startIterating();

            while (true) {
                try {
                    FinanceUser item = users.getNext(consumerId);
                    while (item != null) {
                        if (item.getId().equals(id) && item.getProvider().equals(provider)) {
                            return item;
                        }

                        item = users.getNext(consumerId);
                    }

                    users.stopIterating(consumerId);
                    break;
                    // TODO test
                } catch (ModifiedListException e) {
                    users = usersByPlugin.get(pluginName);
                    consumerId = users.startIterating();
                } catch (Exception e) {
                    users.stopIterating(consumerId);
                }
            }
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider)); 
    }
    
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
            // TODO test
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
        
        while (true) {
            try {
                FinancePlan item = financePlans.getNext(consumerId);

                while (item != null) {
                    if (item.getName().equals(planName)) {
                        return item;
                    }

                    item = financePlans.getNext(consumerId);
                }

                financePlans.stopIterating(consumerId);
                break;
                // TODO test
            } catch (ModifiedListException e) {
                consumerId = financePlans.startIterating();
            } catch (Exception e) {
                financePlans.stopIterating(consumerId);
                throw e;
            }
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, planName));
    }
    
    public FinancePlan getOrDefaultFinancePlan(String planName) throws InvalidParameterException {
        try {
            return this.getFinancePlan(planName);
        } catch (InvalidParameterException e) {
            String defaultFinancePlan = PropertiesHolder.getInstance()
                    .getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME);
            return this.getFinancePlan(defaultFinancePlan);
        }
    }

    public void removeFinancePlan(String planName) throws InvalidParameterException {
        FinancePlan plan = getFinancePlan(planName);
        
        synchronized(plan) {
            this.financePlans.removeItem(plan);
            this.databaseManager.removeFinancePlan(planName);
        }
    }

    // TODO test
    public void updateFinancePlan(String planName, Map<String, String> planInfo) throws InvalidParameterException {
        FinancePlan financePlan = getFinancePlan(planName);
        
        synchronized(financePlan) {
            financePlan.update(planInfo);
            saveFinancePlan(financePlan);
        }
    }

    // TODO test
    public Map<String, String> getFinancePlanMap(String planName) throws InvalidParameterException {
        FinancePlan financePlan = getFinancePlan(planName);   
        
        synchronized(financePlan) {
            return financePlan.getRulesAsMap();
        }
    }
}
