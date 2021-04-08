package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.RasOperation;

/**
 * A {@link cloud.fogbow.fs.core.models.FinanceUser} manager, 
 * capable of managing the state of several users through a set of working
 * threads and providing information on the user state. Since a FinancePlugin
 * also works as a thread manager, this abstraction provides methods for 
 * starting and stopping the internal threads.
 */
public interface FinancePlugin {
	/**
	 * Starts all the internal threads related to this plugin's
	 * financial management. 
	 */
	void startThreads();
	
	/**
	 * Stops all the internal threads related to this plugin's
	 * financial management.
	 */
	void stopThreads();
	
	/**
	 * Verifies if the user represented by {@code userId} is authorized to perform the
	 * operation with the given parameters.
	 * 
	 * @param userId the id of the user to be authorized.
	 * @param operationParameters a map containing the operation parameters.
	 * @return a boolean stating whether the user is authorized or not.
	 * @throws InvalidParameterException 
	 */
	boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException;
	
	/**
	 * Verifies if this plugin manages the financial state
	 * of the user represented by {@code userId}.
	 * 
	 * @param userId the id of the user to verify.
	 * @return a boolean stating whether the user is managed by this plugin or not.
	 */
	boolean managesUser(String userId, String provider);
	
	/**
	 * Generates and returns a representation of the given property of
	 * a user finance state.
	 * 
	 * @param userId the id of the user whose state is used in the operation.
	 * @param property a description of the property to generate.
	 * @return a representation of the property.
	 * @throws InvalidParameterException 
	 */
	String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException;

	// TODO documentation
	String getName();
	
	// TODO documentation
	void addUser(String userId, String provider, Map<String, String> financeOptions);
	
	// TODO documentation
	void removeUser(String userId, String provider) throws InvalidParameterException;

	// TODO documentation
	void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException;
	
	// TODO documentation
	void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException;
}
