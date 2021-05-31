package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.RasOperation;

// TODO documentation
public interface PlanPlugin {
    // TODO documentation
    public String getName();
    
    // TODO documentation
    public boolean isRegisteredUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    
    // TODO documentation
    public void registerUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    
    // TODO documentation
    public void changePlan(SystemUser systemUser, String newPlanName);
    
    // TODO documentation
    public void unregisterUser(SystemUser user) throws InvalidParameterException, InternalServerErrorException;
    
    // TODO documentation
    public Map<String, String> getOptions();
    
    // TODO documentation
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException;
    
    // TODO documentation
    // can be used on an already started plugin
    public void startThreads();
    
    // TODO documentation
    public boolean isStarted();
    
    // TODO documentation
    // can be used on an already stopped plugin
    public void stopThreads();
    
    // TODO documentation
    public String getUserFinanceState(SystemUser user, String property) throws InvalidParameterException, InternalServerErrorException;
    
    // TODO documentation
    public void updateUserFinanceState(SystemUser user, Map<String, String> financeState) throws InternalServerErrorException, InvalidParameterException;
    
    // TODO documentation
    public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException;
    
    // TODO documentation
    public void setUp(Object ... params) throws ConfigurationErrorException;
}
