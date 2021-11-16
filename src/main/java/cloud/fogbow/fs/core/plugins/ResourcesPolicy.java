package cloud.fogbow.fs.core.plugins;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinanceUser;

public interface ResourcesPolicy {
    void updateUserState(FinanceUser user) throws InvalidParameterException, InternalServerErrorException;
}
