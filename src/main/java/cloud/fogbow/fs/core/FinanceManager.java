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
        
        if (objectHolder.getPlans().isEmpty()) {
            tryToCreateDefaultPlanPlugin();
        }
    }

    private void tryToCreateDefaultPlanPlugin() 
            throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
        String defaultPlanPluginType = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_PLUGIN_TYPE);
        String defaultPlanName = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_PLAN_NAME);
        
        PersistablePlanPlugin plan = PlanPluginInstantiator.getPlan(
                defaultPlanPluginType, defaultPlanName, objectHolder.getInMemoryUsersHolder());
        objectHolder.registerFinancePlan(plan);
    }

    public boolean isAuthorized(SystemUser user, RasOperation operation) throws FogbowException {
        MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
        boolean authorized = false;
        
        while (true) {
            Integer consumerId = plans.startIterating();
            
            try {
                authorized = tryToAuthorize(plans, user, operation, consumerId);
                plans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = plans.startIterating();
            } catch (Exception e) {
                plans.stopIterating(consumerId);
                throw e;
            }
        }
        
        return authorized;
    }

    private boolean tryToAuthorize(MultiConsumerSynchronizedList<PersistablePlanPlugin> plans, 
            SystemUser authenticatedUser, RasOperation operation, Integer consumerId) 
            throws InvalidParameterException, InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plan = plans.getNext(consumerId);
        boolean authorized = false;
        
        while (plan != null) {
            synchronized(plan) {
                if (plan.isRegisteredUser(authenticatedUser)) {
                    authorized = plan.isAuthorized(authenticatedUser, operation);
                    return authorized;
                }  
            }
            
            plan = plans.getNext(consumerId);
        }
        
        return false;
    }

    public void startPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
            Integer consumerId = plans.startIterating();
            
            try {
                tryToStart(plans, consumerId);
                plans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = plans.startIterating();
            } catch (Exception e) {
                plans.stopIterating(consumerId);
                throw e;
            }
        }
    }

    private void tryToStart(MultiConsumerSynchronizedList<PersistablePlanPlugin> plans, Integer consumerId)
            throws InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plan = plans.getNext(consumerId);
        
        while (plan != null) {
            if (!plan.isStarted()) {
                plan.startThreads();
            }

            plan = plans.getNext(consumerId);
        }   
    }

    public void stopPlugins() throws InternalServerErrorException {
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
            Integer consumerId = plans.startIterating();
            
            try {
                tryToStop(plans, consumerId);
                plans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = plans.startIterating();
            } catch (Exception e) {
                plans.stopIterating(consumerId);
                throw e;
            }
        }
    }
    
    private void tryToStop(MultiConsumerSynchronizedList<PersistablePlanPlugin> plans, Integer consumerId)
            throws InternalServerErrorException, ModifiedListException {
        PersistablePlanPlugin plugin = plans.getNext(consumerId);
        
        while (plugin != null) {
            if (plugin.isStarted()) {
                plugin.stopThreads();
            }

            plugin = plans.getNext(consumerId);
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
            MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = 
                    this.objectHolder.getPlans();
            Integer consumerId = plans.startIterating();
            
            try {
                tryToAdd(plans, user, financePlan, consumerId);
                plans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = plans.startIterating();
            } catch (Exception e) {
                plans.stopIterating(consumerId);
                throw e;
            }
        } 
    }

    private void tryToAdd(MultiConsumerSynchronizedList<PersistablePlanPlugin> plans, 
            SystemUser user, String pluginName, Integer consumerId) 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        PersistablePlanPlugin plan = plans.getNext(consumerId);
        
        while (plan != null) {
            synchronized(plan) {
                if (plan.getName().equals(pluginName)) {
                    plan.registerUser(user);
                    return;
                }
            }
            
            plan = plans.getNext(consumerId);
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
        PersistablePlanPlugin plan = null;
        
        try {
            plan = getUserPlan(systemUser);
        } catch (InvalidParameterException e) {
            
        }
        
        if (plan != null) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_MANAGED_BY_PLUGIN, 
                    systemUser.getId(), systemUser.getIdentityProviderId(), plan.getName()));  
        }
    }

    public void unregisterUser(SystemUser systemUser) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plan = getUserPlan(systemUser);
        synchronized(plan) {
            plan.unregisterUser(systemUser);
        }
    }

    public void changePlan(SystemUser systemUser, String newPlanName) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plan = getUserPlan(systemUser);
        synchronized(plan) {
            plan.changePlan(systemUser, newPlanName);
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

    private PersistablePlanPlugin getUserPlan(SystemUser systemUser) throws InvalidParameterException, 
    InternalServerErrorException {
        PersistablePlanPlugin plan = null;
        
        while (true) {
            MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
            Integer consumerId = plans.startIterating();
            
            try {
                plan = tryToGet(plans, systemUser, consumerId);
                plans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = plans.startIterating();
            } catch (Exception e) {
                plans.stopIterating(consumerId);
                throw e;
            }
        }
        
        return plan;
    }

    private PersistablePlanPlugin tryToGet(MultiConsumerSynchronizedList<PersistablePlanPlugin> plans, 
            SystemUser user, Integer consumerId) throws InternalServerErrorException, ModifiedListException, 
            InvalidParameterException {
        PersistablePlanPlugin plan = plans.getNext(consumerId);
        
        while (plan != null) {
            if (plan.isRegisteredUser(user)) {
                return plan;
            }
            
            plan = plans.getNext(consumerId);
        } 
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, user.getId()));
    }
    
    /*
     * Plan management
     */

    public void createFinancePlan(String pluginClassName, String planName, Map<String, String> pluginOptions) 
            throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin plan = PlanPluginInstantiator.getPlan(pluginClassName, planName, 
                pluginOptions, objectHolder.getInMemoryUsersHolder());
        this.objectHolder.registerFinancePlan(plan);
        plan.startThreads();
    }
    
    public void removeFinancePlan(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        this.objectHolder.removeFinancePlan(pluginName);
    }
    
    public void changeOptions(String planName, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder.updateFinancePlan(planName, financeOptions);
    }
    
    public Map<String, String> getFinancePlanOptions(String pluginName) 
            throws InternalServerErrorException, InvalidParameterException {
        return this.objectHolder.getFinancePlanOptions(pluginName);
    }
}
