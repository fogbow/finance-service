package cloud.fogbow.fs.core;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.plugins.plan.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

public class InMemoryFinanceObjectsHolder {
    private static Logger LOGGER = Logger.getLogger(InMemoryFinanceObjectsHolder.class);
    
    private DatabaseManager databaseManager;
    private MultiConsumerSynchronizedList<PlanPlugin> planPlugins;
    private InMemoryUsersHolder usersHolder;

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager) throws InternalServerErrorException, ConfigurationErrorException {
        this(databaseManager, new MultiConsumerSynchronizedListFactory(), new UserCreditsFactory());
    }

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager,
            MultiConsumerSynchronizedListFactory listFactory, UserCreditsFactory userCreditsFactory)
            throws InternalServerErrorException, ConfigurationErrorException {
        this.databaseManager = databaseManager;
        loadData(listFactory, userCreditsFactory);
    }

    private void loadData(MultiConsumerSynchronizedListFactory listFactory,
            UserCreditsFactory userCreditsFactory) throws InternalServerErrorException, ConfigurationErrorException {
        // read users data
        this.usersHolder = new InMemoryUsersHolder(databaseManager, listFactory, userCreditsFactory);
        
        // read plan plugins data
        List<PlanPlugin> databasePlanPlugins = this.databaseManager.getRegisteredPlanPlugins();
        planPlugins = listFactory.getList();
        
        for (PlanPlugin plugin : databasePlanPlugins) {
            plugin.setUp(this.usersHolder);
            planPlugins.addItem(plugin);
        }
    }

    public void reset() throws InternalServerErrorException, ConfigurationErrorException {
        loadData(new MultiConsumerSynchronizedListFactory(), new UserCreditsFactory());
    }
    
    public InMemoryUsersHolder getInMemoryUsersHolder() {
        return this.usersHolder;
    }
    
    /*
     * 
     * User methods
     * 
     */

    @Deprecated
    public void registerUser(String userId, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        this.usersHolder.registerUser(userId, provider, pluginName);
    }

    @Deprecated
    public void saveUser(FinanceUser user) throws InvalidParameterException, InternalServerErrorException { 
        this.usersHolder.saveUser(user);
    }

    @Deprecated
    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        this.usersHolder.removeUser(userId, provider);
    }

    @Deprecated
    public FinanceUser getUserById(String id, String provider)
            throws InternalServerErrorException, InvalidParameterException {
        return this.usersHolder.getUserById(id, provider);
    }

    @Deprecated
    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
        return this.usersHolder.getRegisteredUsersByPaymentType(pluginName);
    }

    @Deprecated
    public void changeOptions(String userId, String provider, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        this.usersHolder.changeOptions(userId, provider, financeOptions);
    }

    /*
     * 
     * FinancePlans methods
     * 
     */

    // TODO test
    public MultiConsumerSynchronizedList<PlanPlugin> getPlanPlugins() {
        return planPlugins;
    }

    // TODO test
    public void registerPlanPlugin(PlanPlugin plugin) throws InternalServerErrorException, InvalidParameterException {
        LOGGER.info(plugin.getName());
        
        checkIfPluginExists(plugin.getName());
        this.planPlugins.addItem(plugin);
        this.databaseManager.savePlanPlugin(plugin);
    }

    // TODO test
    public PlanPlugin getPlanPluginPlan(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        Integer consumerId = planPlugins.startIterating();
        PlanPlugin planToReturn = null;

        while (true) {
            try {
                planToReturn = tryToGetPluginFromList(pluginName, consumerId);
                planPlugins.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = planPlugins.startIterating();
            } catch (Exception e) {
                planPlugins.stopIterating(consumerId);
                throw e;
            }
        }

        if (planToReturn != null) {
            return planToReturn;
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, pluginName));
    }

    private PlanPlugin tryToGetPluginFromList(String pluginName, Integer consumerId)
            throws ModifiedListException, InternalServerErrorException {
        PlanPlugin item = planPlugins.getNext(consumerId);
        PlanPlugin planToReturn = null;

        while (item != null) {
            if (item.getName().equals(pluginName)) {
                planToReturn = item;
                break;
            }

            item = planPlugins.getNext(consumerId);
        }

        return planToReturn;
    }
    
    private void checkIfPluginExists(String name) throws InternalServerErrorException, InvalidParameterException {
        try {
            getPlanPluginPlan(name);
        } catch (InvalidParameterException e) {
            return;
        }
        
        // FIXME message
        throw new InvalidParameterException(String.format(Messages.Exception.FINANCE_PLAN_ALREADY_EXISTS, name));
    }

    // TODO test
    public void removePlanPlugin(String pluginName) throws InternalServerErrorException, InvalidParameterException {
        PlanPlugin planPlugin = getPlanPluginPlan(pluginName);
        
        synchronized(planPlugin) {
            planPlugins.removeItem(planPlugin);
            this.databaseManager.removePlanPlugin(planPlugin);
        }
    }

    // TODO test
    public void updatePlanPlugin(String pluginName, Map<String, String> pluginOptions) 
            throws InternalServerErrorException, InvalidParameterException {
        PlanPlugin planPlugin = getPlanPluginPlan(pluginName);
        
        synchronized(planPlugin) {
            planPlugin.setOptions(pluginOptions);
        }
    }

    // TODO test
    public Map<String, String> getPlanPluginOptions(String pluginName) 
            throws InternalServerErrorException, InvalidParameterException {
        PlanPlugin planPlugin = getPlanPluginPlan(pluginName);
        
        synchronized(planPlugin) {
            return planPlugin.getOptions();
        }
    }
}
