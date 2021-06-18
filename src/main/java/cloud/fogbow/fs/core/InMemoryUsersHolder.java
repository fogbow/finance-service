package cloud.fogbow.fs.core;

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
import cloud.fogbow.fs.core.plugins.plan.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.FinanceUserFactory;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListFactory;

public class InMemoryUsersHolder {
    private DatabaseManager databaseManager;
    private UserCreditsFactory userCreditsFactory;
    private MultiConsumerSynchronizedListFactory listFactory;
    private FinanceUserFactory userFactory;
    
    private Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin;
    private MultiConsumerSynchronizedList<FinanceUser> inactiveUsers;
    
    public InMemoryUsersHolder(DatabaseManager databaseManager) throws InternalServerErrorException, ConfigurationErrorException {
        this(databaseManager, new MultiConsumerSynchronizedListFactory(), new UserCreditsFactory(), 
                new FinanceUserFactory(new UserCreditsFactory()));
    }

    @VisibleForTesting
    InMemoryUsersHolder(DatabaseManager databaseManager,
            MultiConsumerSynchronizedListFactory listFactory, UserCreditsFactory userCreditsFactory, 
            FinanceUserFactory userFactory)
            throws InternalServerErrorException, ConfigurationErrorException {
        this.userCreditsFactory = userCreditsFactory;
        this.databaseManager = databaseManager;
        this.listFactory = listFactory;
        this.userFactory = userFactory;

        List<FinanceUser> databaseUsers = this.databaseManager.getRegisteredUsers();
        usersByPlugin = new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();
        inactiveUsers = this.listFactory.getList();
        
        for (FinanceUser user : databaseUsers) {
            addUserByPlugin(user);
        }
    }
    
    @VisibleForTesting
    InMemoryUsersHolder(DatabaseManager databaseManager, UserCreditsFactory userCreditsFactory, 
            MultiConsumerSynchronizedListFactory listFactory, FinanceUserFactory userFactory,
            Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin, 
            MultiConsumerSynchronizedList<FinanceUser> inactiveUsers) {
        this.databaseManager = databaseManager;
        this.userCreditsFactory = userCreditsFactory;
        this.listFactory = listFactory;
        this.userFactory = userFactory;
        this.usersByPlugin = usersByPlugin;
        this.inactiveUsers = inactiveUsers;
    }
    
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
        FinanceUser user = this.userFactory.getUser(userId, provider);
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
            removeUserByPlugin(userToUnregister);
            userToUnregister.unsubscribe();
            this.inactiveUsers.addItem(userToUnregister);
            this.databaseManager.saveUser(userToUnregister);
        }
    }
    
    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        FinanceUser userToRemove = getUserById(userId, provider);

        synchronized (userToRemove) {
            if (userToRemove.isSubscribed()) {
                removeUserByPlugin(userToRemove);
            } else {
                this.inactiveUsers.removeItem(userToRemove);
            }
            this.databaseManager.removeUser(userId, provider);
        }
    }

    private void removeUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(user.getFinancePluginName());
        pluginUsers.removeItem(user);
    }

    public void changePlan(String userId, String provider, String newPlanName)
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser userToChangePlan = getUserById(userId, provider);
        
        synchronized (userToChangePlan) {
            removeUserByPlugin(userToChangePlan);
            userToChangePlan.unsubscribe();
            
            userToChangePlan.subscribeToPlan(newPlanName);
            addUserByPlugin(userToChangePlan);
            
            this.databaseManager.saveUser(userToChangePlan);    
        }
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
        
        userToReturn = getUnregisteredUser(id, provider);
        
        if (userToReturn != null) {
            return userToReturn;
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider));
    }

    private FinanceUser getUnregisteredUser(String id, String provider) 
            throws InternalServerErrorException, InvalidParameterException {
        Integer consumerId = this.inactiveUsers.startIterating();
        FinanceUser userToReturn = null;

        while (true) {
            try {
                userToReturn = getUserFromList(id, provider, this.inactiveUsers, consumerId);
                this.inactiveUsers.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = this.inactiveUsers.startIterating();
            } catch (Exception e) {
                this.inactiveUsers.stopIterating(consumerId);
                throw e;
            }
        }

        return userToReturn;
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
    
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPlan(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return this.listFactory.getList();
        }
    }
}
