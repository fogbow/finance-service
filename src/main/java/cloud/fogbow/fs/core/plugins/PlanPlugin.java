package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.RasOperation;

// TODO documentation
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
// TODO maybe create an interface PlanPlugin and move the methods to 
// the interface, and rename this abstract class to StorablePlanPlugin, 
// which will hold only the name.
public abstract class PlanPlugin {

    @Id
    protected String name;
    
    public abstract String getName();
    public abstract boolean isRegisteredUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    public abstract void registerUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException;
    public abstract void unregisterUser(SystemUser user) throws InvalidParameterException, InternalServerErrorException;
    public abstract Map<String, String> getOptions();
    public abstract void setOptions(Map<String, String> financeOptions) throws InvalidParameterException;
    // can be used on an already started plugin
    public abstract void startThreads();
    public abstract boolean isStarted();
    // can be used on an already stopped plugin
    public abstract void stopThreads();
    public abstract String getUserFinanceState(SystemUser user, String property) throws InvalidParameterException, InternalServerErrorException;
    public abstract void updateUserFinanceState(SystemUser user, Map<String, String> financeState) throws InternalServerErrorException, InvalidParameterException;
    public abstract boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException;
    public abstract void setUp(Object ... params) throws ConfigurationErrorException;
}
