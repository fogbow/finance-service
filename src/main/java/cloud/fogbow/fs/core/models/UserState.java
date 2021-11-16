package cloud.fogbow.fs.core.models;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public enum UserState {
    DEFAULT("DEFAULT"),
    WAITING_FOR_STOP("WAITING_FOR_STOP"),
    STOPPING("STOPPING"),
    STOPPED("STOPPED"),
    RESUMING("RESUMING");
    
    private String value;
    
    private UserState(String value) {
        this.value = value;
    }
    
    public static UserState fromValue(String value) throws InvalidParameterException {
        for (UserState state: UserState.values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_USER_STATE, value));
    }
    
    public String getValue() {
        return value;
    }
}
