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
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListFactory;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListIteratorBuilder;

public class InMemoryUsersHolder {
    private DatabaseManager databaseManager;
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
    InMemoryUsersHolder(DatabaseManager databaseManager, MultiConsumerSynchronizedListFactory listFactory, 
            FinanceUserFactory userFactory, Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin, 
            MultiConsumerSynchronizedList<FinanceUser> inactiveUsers) {
        this.databaseManager = databaseManager;
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
            try {
                userToReturn = getUserByIdAndPlugin(id, provider, pluginName);
            } catch (InvalidParameterException e) {
                
            }

            if (userToReturn != null) {
                return userToReturn;
            }
        }
        
        userToReturn = getUserFromList(id, provider, inactiveUsers);
        
        if (userToReturn != null) {
            return userToReturn;
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider));
    }

    private FinanceUser getUserByIdAndPlugin(String id, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedList<FinanceUser> users = usersByPlugin.get(pluginName);
        return getUserFromList(id, provider, users);
    }
    
    private FinanceUser getUserFromList(String id, String provider, 
            MultiConsumerSynchronizedList<FinanceUser> list) throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedListIteratorBuilder<FinanceUser> iteratorBuilder = 
                new MultiConsumerSynchronizedListIteratorBuilder<>();
        
        return iteratorBuilder
        .processList(list)
        .usingAsArgs(id, provider)
        .usingAsPredicate((user, args) -> {
            String userId = (String) args.get(0);
            String userProvider = (String) args.get(1);
            
            return user.getId().equals(userId) && user.getProvider().equals(userProvider);
        })
        .select();
    }

    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPlan(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return this.listFactory.getList();
        }
    }
}
