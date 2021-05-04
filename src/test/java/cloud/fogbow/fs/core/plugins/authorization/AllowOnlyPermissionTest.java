package cloud.fogbow.fs.core.plugins.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;


@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class AllowOnlyPermissionTest {
    private AllowOnlyPermission permission;
    private Set<OperationType> allowedOperations = getOperationTypeSet(OperationType.ADD_USER, 
                                                                  OperationType.CHANGE_OPTIONS);
    private Set<OperationType> noOperation = getOperationTypeSet();
    private Set<OperationType> updatedAllowedOperations = getOperationTypeSet(OperationType.ADD_USER);
    
    // test case: if the list of the allowed operations types contains 
    // the type of the operation passed as argument, the method isAuthorized must
    // return true. Otherwise, it must return false.
    @Test
    public void testIsAuthorized() throws InvalidParameterException {
        permission = new AllowOnlyPermission(allowedOperations);
        checkIsAuthorizedUsesTheCorrectOperations(allowedOperations);
    }
    
    // test case: if the list of the allowed operations is empty,
    // the method isAuthorized must always return false.
    @Test
    public void testIsAuthorizedNoAuthorizedOperation() throws InvalidParameterException {
        permission = new AllowOnlyPermission(noOperation);
        checkIsAuthorizedUsesTheCorrectOperations(noOperation);
    }
    
    // test case: when calling the method setOperationTypes, it must
    // update the operations used by the permission.
    @Test
    public void testSetOperationTypes() throws InvalidParameterException {
        permission = new AllowOnlyPermission(allowedOperations);
        checkIsAuthorizedUsesTheCorrectOperations(allowedOperations);
        
        permission.setOperationTypes(getOperationTypeStringSet(updatedAllowedOperations));
        checkIsAuthorizedUsesTheCorrectOperations(updatedAllowedOperations);
    }
    
    private void checkIsAuthorizedUsesTheCorrectOperations(Set<OperationType> operations) {
        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);

            if (operations.contains(type)) {
                assertTrue(permission.isAuthorized(operation));
            } else {
                assertFalse(permission.isAuthorized(operation));
            }
        }
    }
    
    private Set<OperationType> getOperationTypeSet(OperationType ... operationTypes) {
        Set<OperationType> operationSet = new HashSet<OperationType>();
        
        for (OperationType operationType : operationTypes) {
            operationSet.add(operationType);
        }
        
        return operationSet;
    }
    
    private Set<String> getOperationTypeStringSet(Set<OperationType> operationTypes) {
        Set<String> operationStrings = new HashSet<String>();
        
        for (OperationType operationType : operationTypes) {
            operationStrings.add(operationType.getValue());
        }
        
        return operationStrings;
    }
}
