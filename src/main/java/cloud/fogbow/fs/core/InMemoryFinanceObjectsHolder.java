package cloud.fogbow.fs.core;

import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.util.list.ModifiedListException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListFactory;

public class InMemoryFinanceObjectsHolder {
    private DatabaseManager databaseManager;
    private MultiConsumerSynchronizedList<PersistablePlanPlugin> financePlans;
    private InMemoryUsersHolder usersHolder;
    private MultiConsumerSynchronizedListFactory listFactory;

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager, InMemoryUsersHolder usersHolder)
            throws InternalServerErrorException, ConfigurationErrorException {
        this(databaseManager, usersHolder, new MultiConsumerSynchronizedListFactory());
    }

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager, InMemoryUsersHolder usersHolder,
            MultiConsumerSynchronizedListFactory listFactory)
            throws InternalServerErrorException, ConfigurationErrorException {
        this.databaseManager = databaseManager;
        this.usersHolder = usersHolder;
        this.listFactory = listFactory;
        loadData();
    }
    
    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager, InMemoryUsersHolder usersHolder,
            MultiConsumerSynchronizedListFactory listFactory, MultiConsumerSynchronizedList<PersistablePlanPlugin> planPlugins) {
        this.databaseManager = databaseManager;
        this.usersHolder = usersHolder;
        this.listFactory = listFactory;
        this.financePlans = planPlugins;
    }

    private void loadData() throws InternalServerErrorException, ConfigurationErrorException {
        List<PersistablePlanPlugin> databasePlans = this.databaseManager.getRegisteredPlans();
        financePlans = listFactory.getList();
        
        for (PersistablePlanPlugin plan : databasePlans) {
            plan.setUp(this.usersHolder);
            financePlans.addItem(plan);
        }
    }

    public void reset() throws InternalServerErrorException, ConfigurationErrorException {
        loadData();
    }
    
    public InMemoryUsersHolder getInMemoryUsersHolder() {
        return this.usersHolder;
    }
    
    public MultiConsumerSynchronizedList<PersistablePlanPlugin> getPlanPlugins() {
        return this.financePlans;
    }

    /*
     * 
     * FinancePlans methods
     * 
     */

    public void registerPlanPlugin(PersistablePlanPlugin plugin) throws InternalServerErrorException, InvalidParameterException {
        checkIfPluginExists(plugin.getName());
        this.financePlans.addItem(plugin);
        this.databaseManager.savePlan(plugin);
    }

    public PersistablePlanPlugin getPlanPlugin(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        Integer consumerId = financePlans.startIterating();
        PersistablePlanPlugin planToReturn = null;

        while (true) {
            try {
                planToReturn = tryToGetPluginFromList(pluginName, consumerId);
                financePlans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = financePlans.startIterating();
            } catch (Exception e) {
                financePlans.stopIterating(consumerId);
                throw e;
            }
        }

        if (planToReturn != null) {
            return planToReturn;
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, pluginName));
    }

    private PersistablePlanPlugin tryToGetPluginFromList(String pluginName, Integer consumerId)
            throws ModifiedListException, InternalServerErrorException {
        PersistablePlanPlugin item = financePlans.getNext(consumerId);
        PersistablePlanPlugin planToReturn = null;

        while (item != null) {
            if (item.getName().equals(pluginName)) {
                planToReturn = item;
                break;
            }

            item = financePlans.getNext(consumerId);
        }

        return planToReturn;
    }
    
    private void checkIfPluginExists(String name) throws InternalServerErrorException, InvalidParameterException {
        try {
            getPlanPlugin(name);
        } catch (InvalidParameterException e) {
            return;
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.FINANCE_PLAN_ALREADY_EXISTS, name));
    }

    public void removePlanPlugin(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        // TODO how should we treat the users who use this plan?
        PersistablePlanPlugin planPlugin = getPlanPlugin(pluginName);
        
        synchronized(planPlugin) {
            financePlans.removeItem(planPlugin);
            this.databaseManager.removePlan(planPlugin);
        }
    }

    public void updatePlanPlugin(String pluginName, Map<String, String> pluginOptions) 
            throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin planPlugin = getPlanPlugin(pluginName);
        
        synchronized(planPlugin) {
            planPlugin.stopThreads();
            planPlugin.setOptions(pluginOptions);
            this.databaseManager.savePlan(planPlugin);
            planPlugin.startThreads();
        }
    }

    public Map<String, String> getPlanPluginOptions(String pluginName) 
            throws InternalServerErrorException, InvalidParameterException {
        PersistablePlanPlugin planPlugin = getPlanPlugin(pluginName);
        
        synchronized(planPlugin) {
            return planPlugin.getOptions();
        }
    }
}
