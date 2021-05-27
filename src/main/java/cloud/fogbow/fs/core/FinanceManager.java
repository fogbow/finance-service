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
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.plugins.PlanPluginInstantiator;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
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
        
        PlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(defaultPlanPluginType, defaultPlanName, objectHolder.getInMemoryUsersHolder());
        objectHolder.registerPlanPlugin(plugin);
    }

    public boolean isAuthorized(AuthorizableUser user) throws FogbowException {
        String userToken = user.getUserToken();
        RSAPublicKey rasPublicKey = FsPublicKeysHolder.getInstance().getRasPublicKey();
        SystemUser authenticatedUser = AuthenticationUtil.authenticate(rasPublicKey, userToken);
        
        MultiConsumerSynchronizedList<PlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
        boolean authorized = false;
        
        while (true) {
            Integer consumerId = planPlugins.startIterating();
            
            try {
                authorized = tryToAuthorize(planPlugins, authenticatedUser, user.getOperation(), consumerId);
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

    private boolean tryToAuthorize(MultiConsumerSynchronizedList<PlanPlugin> planPlugins, SystemUser authenticatedUser, RasOperation operation, Integer consumerId) 
            throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
        PlanPlugin plugin = planPlugins.getNext(consumerId);
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
        
        throw new InvalidParameterException(
                String.format(Messages.Exception.UNMANAGED_USER, authenticatedUser.getId()));
    }

    public void startPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
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

    private void tryToStart(MultiConsumerSynchronizedList<PlanPlugin> planPlugins, Integer consumerId) throws InternalServerErrorException, ModifiedListException {
        PlanPlugin plugin = planPlugins.getNext(consumerId);
        
        while (plugin != null) {
            if (!plugin.isStarted()) {
                plugin.startThreads();
            }

            plugin = planPlugins.getNext(consumerId);
        }   
    }

    public void stopPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
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
    
    private void tryToStop(MultiConsumerSynchronizedList<PlanPlugin> planPlugins, Integer consumerId) throws InternalServerErrorException, ModifiedListException {
        PlanPlugin plugin = planPlugins.getNext(consumerId);
        
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

    public void addUser(User user) throws InvalidParameterException, InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
            Integer consumerId = planPlugins.startIterating();
            
            try {
                tryToAdd(planPlugins, new SystemUser(user.getUserId(), user.getUserId(), user.getProvider()), user.getFinancePluginName(), consumerId);
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

    private void tryToAdd(MultiConsumerSynchronizedList<PlanPlugin> planPlugins, SystemUser user, String pluginName, Integer consumerId) 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        PlanPlugin plugin = planPlugins.getNext(consumerId);
        
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

    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        PlanPlugin plugin = getUserPlugin(userId, provider);
        synchronized(plugin) {
            plugin.unregisterUser(new SystemUser(userId, userId, provider));
        }
    }

    public void updateFinanceState(String userId, String provider, Map<String, String> financeState)
            throws InvalidParameterException, InternalServerErrorException {
        PlanPlugin plugin = getUserPlugin(userId, provider);
        synchronized(plugin) {
            plugin.updateUserFinanceState(new SystemUser(userId, userId, provider), financeState);
        }
    }

    public String getFinanceStateProperty(String userId, String provider, String property) throws FogbowException {
        PlanPlugin plugin = getUserPlugin(userId, provider);
        synchronized(plugin) {
            return plugin.getUserFinanceState(new SystemUser(userId, userId, provider), property);
        }
    }

    private PlanPlugin getUserPlugin(String userId, String provider) throws InvalidParameterException, 
    InternalServerErrorException {
        PlanPlugin plugin = null;
        
        while (true) {
            MultiConsumerSynchronizedList<PlanPlugin> planPlugins = this.objectHolder.getPlanPlugins();
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

    private PlanPlugin tryToGet(MultiConsumerSynchronizedList<PlanPlugin> planPlugins, SystemUser user, Integer consumerId) 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        PlanPlugin plugin = planPlugins.getNext(consumerId);
        
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
        PlanPlugin plugin = PlanPluginInstantiator.getPlanPlugin(pluginClassName, planName, pluginOptions, objectHolder.getInMemoryUsersHolder());
        this.objectHolder.registerPlanPlugin(plugin);
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
