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
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListIteratorBuilder;
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
        PersistablePlanPlugin plan = this.objectHolder.getUserPlan(user);
        return plan.isAuthorized(user, operation);
    }

    public void startPlugins() throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
        MultiConsumerSynchronizedListIteratorBuilder<PersistablePlanPlugin> iteratorBuilder = 
                new MultiConsumerSynchronizedListIteratorBuilder<>();
        
        iteratorBuilder
        .processList(plans)
        .usingAsArgs()
        .usingAsProcessor((plan, args) -> {
            if (!plan.isStarted()) {
                plan.startThreads();
            }
        })
        .process();
    }

    public void stopPlugins() throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedList<PersistablePlanPlugin> plans = this.objectHolder.getPlans();
        MultiConsumerSynchronizedListIteratorBuilder<PersistablePlanPlugin> iteratorBuilder = 
                new MultiConsumerSynchronizedListIteratorBuilder<>();
        
        iteratorBuilder
        .processList(plans)
        .usingAsArgs()
        .usingAsProcessor((plan, args) -> {
            if (plan.isStarted()) {
                plan.stopThreads();
            }
        })
        .process();
    }

    public void resetPlugins() throws ConfigurationErrorException, InternalServerErrorException {
        objectHolder.reset();
    }

    /*
     * User Management
     */
  
    public void addUser(SystemUser user, String financePlan) 
            throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plan = this.objectHolder.getFinancePlan(financePlan);
        plan.registerUser(user);
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
            plan = this.objectHolder.getUserPlan(systemUser);
        } catch (InvalidParameterException e) {
            
        }
        
        if (plan != null) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_MANAGED_BY_PLUGIN, 
                    systemUser.getId(), systemUser.getIdentityProviderId(), plan.getName()));  
        }
    }

    public void unregisterUser(SystemUser systemUser) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plan = this.objectHolder.getUserPlan(systemUser);
        synchronized(plan) {
            plan.unregisterUser(systemUser);
        }
    }

    public void changePlan(SystemUser systemUser, String newPlanName) throws InvalidParameterException, InternalServerErrorException {
        PersistablePlanPlugin plan = this.objectHolder.getUserPlan(systemUser);
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
