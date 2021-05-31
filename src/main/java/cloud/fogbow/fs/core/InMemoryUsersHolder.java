package cloud.fogbow.fs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.plan.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

public class InMemoryUsersHolder {
    private DatabaseManager databaseManager;
    private UserCreditsFactory userCreditsFactory;
    private MultiConsumerSynchronizedListFactory listFactory;
    
    private Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin;
    private MultiConsumerSynchronizedList<FinanceUser> inactiveUsers;
    
    public InMemoryUsersHolder(DatabaseManager databaseManager) throws InternalServerErrorException, ConfigurationErrorException {
        this(databaseManager, new MultiConsumerSynchronizedListFactory(), new UserCreditsFactory());
    }

    @VisibleForTesting
    InMemoryUsersHolder(DatabaseManager databaseManager,
            MultiConsumerSynchronizedListFactory listFactory, UserCreditsFactory userCreditsFactory)
            throws InternalServerErrorException, ConfigurationErrorException {
        this.userCreditsFactory = userCreditsFactory;
        this.databaseManager = databaseManager;
        this.listFactory = listFactory;

        List<FinanceUser> databaseUsers = this.databaseManager.getRegisteredUsers();
        usersByPlugin = new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        inactiveUsers = this.listFactory.getList();
        
        for (FinanceUser user : databaseUsers) {
            addUserByPlugin(user);
        }
    }
    
    @VisibleForTesting
    InMemoryUsersHolder(DatabaseManager databaseManager, 
            UserCreditsFactory userCreditsFactory, 
            MultiConsumerSynchronizedListFactory listFactory, 
            Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin) {
        this.databaseManager = databaseManager;
        this.userCreditsFactory = userCreditsFactory;
        this.listFactory = listFactory;
        this.usersByPlugin = usersByPlugin;
    }
    
    // TODO Improve tests
    public void registerUser(String userId, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = null;
        
        try {
            user = getUserById(userId, provider);
        } catch (InvalidParameterException e) {
            
        }
        
        if (user != null) {
            tryToSubscribeUserToPlan(userId, provider, pluginName, user);
        } else {
            user = createUserAndSubscribe(userId, provider, pluginName);
        }
        
        this.databaseManager.saveUser(user);
    }

    private void tryToSubscribeUserToPlan(String userId, String provider, String pluginName, FinanceUser user)
            throws InvalidParameterException, InternalServerErrorException {
        if (user.isSubscribed()) {
            throw new InvalidParameterException(String.format(Messages.Exception.USER_ALREADY_EXISTS, provider, userId));
        } else {
            this.inactiveUsers.removeItem(user);
            user.subscribeToPlan(pluginName);
            addUserByPlugin(user);
        }
    }
    
    private FinanceUser createUserAndSubscribe(String userId, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user;
        user = new FinanceUser(new HashMap<String, String>());
        user.setUserId(userId, provider);
        user.setCredits(userCreditsFactory.getUserCredits(userId, provider));
        user.setInvoices(new ArrayList<Invoice>());
        user.subscribeToPlan(pluginName);

        addUserByPlugin(user);
        return user;
    }

    private void addUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        if (user.isSubscribed()) {
            String pluginName = user.getFinancePluginName();

            if (!usersByPlugin.containsKey(pluginName)) {
                usersByPlugin.put(pluginName, this.listFactory.getList());
            }
            
            MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(pluginName);
            pluginUsers.addItem(user);
        } else {
            inactiveUsers.addItem(user);
        }
    }

    public void saveUser(FinanceUser user) throws InvalidParameterException, InternalServerErrorException {
        getUserById(user.getId(), user.getProvider());

        synchronized (user) {
            this.databaseManager.saveUser(user);
        }
    }

    public void unregisterUser(String userId, String provider) 
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser userToUnregister = getUserById(userId, provider);
        
        synchronized (userToUnregister) {
            userToUnregister.unsubscribe();
            removeUserByPlugin(userToUnregister);
            this.inactiveUsers.addItem(userToUnregister);
            this.databaseManager.saveUser(userToUnregister);
        }
    }
    
    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        FinanceUser userToRemove = getUserById(userId, provider);

        synchronized (userToRemove) {
            removeUserByPlugin(userToRemove);
            this.databaseManager.removeUser(userId, provider);
        }
    }

    private void removeUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(user.getFinancePluginName());
        pluginUsers.removeItem(user);
    }

    public FinanceUser getUserById(String id, String provider)
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser userToReturn = null;
        
        for (String pluginName : usersByPlugin.keySet()) {
            userToReturn = getUserByIdAndPlugin(id, provider, pluginName);

            if (userToReturn != null) {
                return userToReturn;
            }
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider));
    }

    private FinanceUser getUserByIdAndPlugin(String id, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedList<FinanceUser> users = usersByPlugin.get(pluginName);
        Integer consumerId = users.startIterating();
        FinanceUser userToReturn = null;

        while (true) {
            try {
                userToReturn = getUserFromList(id, provider, users, consumerId);
                users.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                users = usersByPlugin.get(pluginName);
                consumerId = users.startIterating();
            } catch (Exception e) {
                users.stopIterating(consumerId);
                throw e;
            }
        }

        return userToReturn;
    }

    private FinanceUser getUserFromList(String id, String provider, MultiConsumerSynchronizedList<FinanceUser> users,
            Integer consumerId) throws ModifiedListException, InternalServerErrorException {
        FinanceUser item = users.getNext(consumerId);
        FinanceUser userToReturn = null;

        while (item != null) {
            if (item.getId().equals(id) && item.getProvider().equals(provider)) {
                userToReturn = item;
                break;
            }

            item = users.getNext(consumerId);
        }

        return userToReturn;
    }

    @Deprecated
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return new MultiConsumerSynchronizedList<FinanceUser>();
        }
    }

    public void changeOptions(String userId, String provider, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = getUserById(userId, provider);

        synchronized (user) {
            for (String option : financeOptions.keySet()) {
                user.setProperty(option, financeOptions.get(option));
            }

            this.databaseManager.saveUser(user);
        }
    }
    
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPlan(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return this.listFactory.getList();
        }
    }
}
