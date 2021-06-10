package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.plugins.PlanPluginInstantiator;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.ras.core.models.RasOperation;

public class FinanceManager {
    private InMemoryFinanceObjectsHolder objectHolder;
    
    public FinanceManager(InMemoryFinanceObjectsHolder objectHolder)
            throws ConfigurationErrorException, InternalServerErrorException, InvalidParameterException {
        this.objectHolder = objectHolder;
        
        if (objectHolder.getPlanPlugins().isEmpty()) {
            tryToCreateDefaultPlanPlugin();
        }
    }

    private void tryToCreateDefaultPlanPlugin() throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
        String defaultPlanPluginType = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE);
        String defaultPlanName = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_NAME);
        
        PersistablePlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(defaultPlanPluginType, defaultPlanName, objectHolder.getInMemoryUsersHolder());
        objectHolder.registerPlanPlugin(plugin);
    }

    // TODO this method should receive SystemUser and operation
    public boolean isAuthorized(AuthorizableUser user) throws FogbowException {
        String userToken = user.getUserToken();
        RSAPublicKey rasPublicKey = FsPublicKeysHolder.getInstance().getRasPublicKey();
        SystemUser authenticatedUser = AuthenticationUtil.authenticate(rasPublicKey, userToken);
        
        MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
        boolean authorized = false;
        
        while (true) {
            Integer consumerId = planPlugins.startIterating();
            
            try {
                authorized = tryToAuthorize(planPlugins, authenticatedUser, user.getOperation(), consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
                // TODO test
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
        
        return authorized;
    }

    private boolean tryToAuthorize(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, 
            SystemUser authenticatedUser, RasOperation operation, Integer consumerId) 
            throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        boolean authorized = false;
        
        while (plugin != null) {
            synchronized(plugin) {
                if (plugin.isRegisteredUser(authenticatedUser)) {
                    authorized = plugin.isAuthorized(authenticatedUser, operation);
                    return authorized;
                }  
            }
            
            plugin = planPlugins.getNext(consumerId);
        }
        
        return false;
    }

    public void startPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                tryToStart(planPlugins, consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
                // TODO test
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
    }

    private void tryToStart(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, Integer consumerId) throws InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            if (!plugin.isStarted()) {
                plugin.startThreads();
            }

            plugin = planPlugins.getNext(consumerId);
        }   
    }

    public void stopPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                tryToStop(planPlugins, consumerId);
                planPlugins.stopIterating(consumerId);
                break;
                // TODO test
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
    }
    
    private void tryToStop(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, Integer consumerId) throws InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            if (plugin.isStarted()) {
                plugin.startThreads();
            }

            plugin = planPlugins.getNext(consumerId);
        }  
    }

    public void resetPlugins() throws ConfigurationErrorException, InternalServerErrorException {
        objectHolder.reset();
    }

    /*
     * User Management
     */
  
    public void addUser(SystemUser user, String financePlan) throws InvalidParameterException, InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                tryToAdd(planPlugins, user, financePlan, consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        } 
    }

    private void tryToAdd(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, SystemUser user, String pluginName, Integer consumerId) 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            synchronized(plugin) {
                if (plugin.getName().equals(pluginName)) {
                    plugin.registerUser(user);
                    return;
                }
            }
            
            plugin = planPlugins.getNext(consumerId);
        } 
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, user.getId()));
    }

    // TODO we need to decide a standard for these methods signatures
    // some receive userid + provider id, others receive systemuser
    // TODO test
    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        try {
            getUserPlugin(userId, provider);
            // TODO add message: user is still registered in a plan
            throw new InternalServerErrorException();
        } catch (InvalidParameterException e) {
            InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
            FinanceUser user = usersHolder.getUserById(userId, provider);
            
            synchronized(user) {
                usersHolder.removeUser(userId, provider);
            }
        }
    }

    // TODO test
    public void unregisterUser(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plugin = getUserPlugin(userId, provider);
        synchronized(plugin) {
            plugin.unregisterUser(new SystemUser(userId, userId, provider));
        }
    }

    // TODO test
    public void changePlan(String userId, String provider, String newPlanName) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plugin = getUserPlugin(userId, provider);
        synchronized(plugin) {
            plugin.changePlan(new SystemUser(userId, userId, provider), newPlanName);
        }
    }

    public void updateFinanceState(String userId, String provider, Map<String, String> financeState)
            throws InvalidParameterException, InternalServerErrorException {
        InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
        FinanceUser user = usersHolder.getUserById(userId, provider);
        
        synchronized(user) {
            user.updateFinanceState(financeState);
            usersHolder.saveUser(user);
        }
    }

    public String getFinanceStateProperty(String userId, String provider, String property) throws FogbowException {
        InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
        FinanceUser user = usersHolder.getUserById(userId, provider);
        
        synchronized(user) {
            return user.getFinanceState(property);
        }
    }

    private PersistablePlanPlugin getUserPlugin(String userId, String provider) throws InvalidParameterException, 
    InternalServerErrorException {
        PersistablePlanPlugin plugin = null;
        
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                plugin = tryToGet(planPlugins, new SystemUser(userId, userId, provider), consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
        
        return plugin;
    }

    private PersistablePlanPlugin tryToGet(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, SystemUser user, Integer consumerId) 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            if (plugin.isRegisteredUser(user)) {
                return plugin;
            }
            
            plugin = planPlugins.getNext(consumerId);
        } 
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, user.getId()));
    }
    
    /*
     * Plan management
     */

    public void createFinancePlan(String pluginClassName, String planName, Map<String, String> pluginOptions) throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(pluginClassName, planName, pluginOptions, objectHolder.getInMemoryUsersHolder());
        this.objectHolder.registerPlanPlugin(plugin);
        // TODO update test
        plugin.startThreads();
    }
    
    public void removeFinancePlan(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        this.objectHolder.removePlanPlugin(pluginName);
    }
    
    public void changeOptions(String planName, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder.updatePlanPlugin(planName, financeOptions);
    }
    
    public Map<String, String> getFinancePlanOptions(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        return this.objectHolder.getPlanPluginOptions(pluginName);
    }
}
