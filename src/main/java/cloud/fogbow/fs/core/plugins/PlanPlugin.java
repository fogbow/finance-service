package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.RasOperation;

/**
 * A {@link cloud.fogbow.fs.core.models.FinanceUser} manager, 
 * capable of managing the state of several users through a set of working
 * threads. Since a FinancePlugin also works as a thread manager, 
 * this abstraction provides methods for starting and stopping the internal threads.
 */
public interface PlanPlugin {
    /**
     * Returns a String representing the plan name.
     * 
     * @return the name String.
     */
    public String getName();
    
    /**
     * Verifies if this plan manages the financial state
     * of the given user.
     * 
     * @param user the user to verify.
     * @return a boolean stating whether the user is managed by this plan or not.
     * @throws InternalServerErrorException if an error occurs while trying to retrieve the user state. 
     * @throws InvalidParameterException if an error occurs while trying to find the user.
     */
    public boolean isRegisteredUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    
    /**
     * Adds the given user to the list of the users managed by the plan.
     * 
     * @param user the user to be managed.
     * @throws InvalidParameterException if the user is already subscribed to a plan.
     * @throws InternalServerErrorException if an error occurs while trying to add the user.
     */
    public void registerUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    
    /**
     * Removes the given user from the list of the users managed by this plan and adds
     * the user to the given new plan.
     * 
     * @param user the user to change plan.
     * @param newPlanName the new plan to subscribe.
     * @throws InvalidParameterException if an error occurs while trying to find the user or the user
     * state does not allow the operation.
     * @throws InternalServerErrorException if an error occurs while trying to change the user plan.
     */
    public void changePlan(SystemUser systemUser, String newPlanName) throws InternalServerErrorException, InvalidParameterException;
    
    /**
     * Removes all the data related to the given user.
     * 
     * @param user the user to purge.
     * @throws InvalidParameterException if an error occurs while trying to find the user or the user
     * state does not allow the operation.
     * @throws InternalServerErrorException if an error occurs while trying to purge the user.
     */
    public void purgeUser(SystemUser user) throws InvalidParameterException, InternalServerErrorException;
    
    /**
     * Removes the given user from the list of the users managed by the plan.
     * 
     * @param user the user to unregister.
     * @throws InvalidParameterException if an error occurs while trying to find the user or the user
     * state does not allow the operation.
     * @throws InternalServerErrorException if an error occurs while trying to unregister the user.
     */
    public void unregisterUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException;
    
    /**
     * Generates and returns a Map containing all the financial options used to manage the users.
     * 
     * @return a Map containing the financial options.
     */
    public Map<String, String> getOptions();
    
    /**
     * Changes the financial options used to manage the users.
     *
     * @param financeOptions the new financial options to follow.
     * @throws InvalidParameterException if the finance options are invalid.
     */
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException;
    
    /**
     * Starts all the internal threads related to the plan's
     * financial management. If the threads have already been
     * started, does nothing.
     */
    public void startThreads();
    
    /**
     * Verifies if the plan's internal threads are running.
     * 
     * @return a boolean stating whether or not the internal threads are
     * running.
     */
    public boolean isStarted();
    
    /**
     * Stops all the internal threads related to this plugin's
     * financial management. If the threads have already been
     * stopped, does nothing.
     */
    public void stopThreads();
    
    /**
     * Verifies if the user is authorized to perform the operation with the given parameters.
     * 
     * @param user the user to be authorized.
     * @param operation the operation parameters.
     * @return a boolean stating whether the user is authorized or not.
     * @throws InvalidParameterException if an error occurs while trying to find the user 
     * to check state.
     * @throws InternalServerErrorException if an error occurs while trying to retrieve the
     * user state.
     */
    public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException;
    
    /**
     * Sets up the plan's internal data structures and state using given parameters.
     * 
     * @param params a sequence of parameters to be used to set up the plan.
     * @throws ConfigurationErrorException if any of the parameters is invalid.
     */
    public void setUp(Object ... params) throws ConfigurationErrorException;
}
