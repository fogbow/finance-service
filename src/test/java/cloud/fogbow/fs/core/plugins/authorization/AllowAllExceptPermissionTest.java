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
@PrepareForTest({ PropertiesHolder.class })
public class AllowAllExceptPermissionTest {

    private AllowAllExceptPermission permission;
    private Set<OperationType> notAllowedOperations = getOperationTypeSet(OperationType.ADD_USER, OperationType.CREATE_FINANCE_PLAN);
    private Set<OperationType> updatedNotAllowedOperations =  getOperationTypeSet(OperationType.ADD_USER);
    private Set<OperationType> noOperation = getOperationTypeSet();

    // test case: if the list of the not allowed operations types contains
    // the type of the operation passed as argument, the method isAuthorized must
    // return false. Otherwise, it must return true.
    @Test
    public void testIsAuthorized() throws InvalidParameterException {
        permission = new AllowAllExceptPermission(notAllowedOperations);
        checkIsAuthorizedUsesTheCorrectOperations(notAllowedOperations);
    }

    // test case: if the list of the not allowed operations is empty,
    // the method isAuthorized must always return true.
    @Test
    public void testIsAuthorizedAllOperationsAreAuthorized() throws InvalidParameterException {
        permission = new AllowAllExceptPermission(noOperation);
        checkIsAuthorizedUsesTheCorrectOperations(noOperation);
    }
    
    // test case: when calling the method setOperationTypes, it must
    // update the operations used by the permission.
    @Test
    public void testSetOperationTypes() throws InvalidParameterException {
        permission = new AllowAllExceptPermission(notAllowedOperations);
        checkIsAuthorizedUsesTheCorrectOperations(notAllowedOperations);
        
        permission.setOperationTypes(getOperationTypeStringSet(updatedNotAllowedOperations));
        checkIsAuthorizedUsesTheCorrectOperations(updatedNotAllowedOperations);
    }
    
    private void checkIsAuthorizedUsesTheCorrectOperations(Set<OperationType> operations) {
        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);

            if (operations.contains(type)) {
                assertFalse(permission.isAuthorized(operation));
            } else {
                assertTrue(permission.isAuthorized(operation));
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
