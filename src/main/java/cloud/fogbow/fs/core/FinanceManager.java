package cloud.fogbow.fs.core;

import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
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

    private void tryToCreateDefaultPlanPlugin() 
            throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
        String defaultPlanPluginType = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE);
        String defaultPlanName = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_NAME);
        
        PersistablePlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(
                defaultPlanPluginType, defaultPlanName, objectHolder.getInMemoryUsersHolder());
        objectHolder.registerPlanPlugin(plugin);
    }

    public boolean isAuthorized(SystemUser user, RasOperation operation) throws FogbowException {
        MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
        boolean authorized = false;
        
        while (true) {
            Integer consumerId = planPlugins.startIterating();
            
            try {
                authorized = tryToAuthorize(planPlugins, user, operation, consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
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
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
    }

    private void tryToStart(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, Integer consumerId)
            throws InternalServerErrorException, ModifiedListException {
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
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }
    }
    
    private void tryToStop(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, Integer consumerId)
            throws InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            if (plugin.isStarted()) {
                plugin.stopThreads();
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
  
    public void addUser(SystemUser user, String financePlan) 
            throws InvalidParameterException, InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = 
                    this.objectHolder.getPlanPlugins();
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

    private void tryToAdd(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, 
            SystemUser user, String pluginName, Integer consumerId) 
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

    public void removeUser(SystemUser systemUser)
            throws InvalidParameterException, InternalServerErrorException {
        checkUserIsNotManaged(systemUser);

        InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
        FinanceUser user = usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());

        synchronized (user) {
            usersHolder.removeUser(systemUser.getId(), systemUser.getIdentityProviderId());
        }
    }

    private void checkUserIsNotManaged(SystemUser systemUser)
            throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin plugin = null;
        
        try {
            plugin = getUserPlugin(systemUser);
        } catch (InvalidParameterException e) {
            
        }
        
        if (plugin != null) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_MANAGED_BY_PLUGIN, 
                    systemUser.getId(), systemUser.getIdentityProviderId(), plugin.getName()));  
        }
    }

    public void unregisterUser(SystemUser systemUser) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plugin = getUserPlugin(systemUser);
        synchronized(plugin) {
            plugin.unregisterUser(systemUser);
        }
    }

    public void changePlan(SystemUser systemUser, String newPlanName) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plugin = getUserPlugin(systemUser);
        synchronized(plugin) {
            plugin.changePlan(systemUser, newPlanName);
        }
    }

    public void updateFinanceState(SystemUser systemUser, Map<String, String> financeState)
            throws InvalidParameterException, InternalServerErrorException {
        InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
        FinanceUser user = usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
            user.updateFinanceState(financeState);
            usersHolder.saveUser(user);
        }
    }

    public String getFinanceStateProperty(SystemUser systemUser, String property) throws FogbowException {
        InMemoryUsersHolder usersHolder = this.objectHolder.getInMemoryUsersHolder();
        FinanceUser user = usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
            return user.getFinanceState(property);
        }
    }

    private PersistablePlanPlugin getUserPlugin(SystemUser systemUser) throws InvalidParameterException, 
    InternalServerErrorException {
        PersistablePlanPlugin plugin = null;
        
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                plugin = tryToGet(planPlugins, systemUser, consumerId);
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
        
        return plugin;
    }

    private PersistablePlanPlugin tryToGet(MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins, 
            SystemUser user, Integer consumerId) throws InternalServerErrorException, ModifiedListException, 
            InvalidParameterException {
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

    public void createFinancePlan(String pluginClassName, String planName, Map<String, String> pluginOptions) 
            throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(pluginClassName, planName, 
                pluginOptions, objectHolder.getInMemoryUsersHolder());
        this.objectHolder.registerPlanPlugin(plugin);
        plugin.startThreads();
    }
    
    // TODO test
    public void removeFinancePlan(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        this.objectHolder.removePlanPlugin(pluginName);
    }
    
    public void changeOptions(String planName, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder.updatePlanPlugin(planName, financeOptions);
    }
    
    public Map<String, String> getFinancePlanOptions(String pluginName) 
            throws InternalServerErrorException, InvalidParameterException {
        return this.objectHolder.getPlanPluginOptions(pluginName);
    }
}
