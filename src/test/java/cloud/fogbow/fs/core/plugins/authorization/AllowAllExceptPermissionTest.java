package cloud.fogbow.fs.core.plugins.authorization;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PropertiesHolder.class })
public class AllowAllExceptPermissionTest {

    private AllowAllExceptPermission permission;
    private String permissionName = "permission1";
    private String operationsNamesString;
    private List<OperationType> notAllowedOperations = Arrays.asList(OperationType.ADD_USER, OperationType.CREATE_FINANCE_PLAN);
    private List<OperationType> noOperation = new ArrayList<OperationType>();

    private void setUpVariables(List<OperationType> operations) throws InvalidParameterException {
        operationsNamesString = generateOperationNamesString(operations);

        // set up PropertiesHolder
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(operationsNamesString).when(propertiesHolder)
                .getProperty(permissionName + SystemConstants.OPERATIONS_LIST_KEY_SUFFIX);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        permission = new AllowAllExceptPermission(permissionName);
    }

    private String generateOperationNamesString(List<OperationType> operations) {
        ArrayList<String> operationsNames = new ArrayList<String>();
        for (OperationType type : operations) {
            operationsNames.add(type.getValue());
        }
        return String.join(SystemConstants.OPERATION_NAME_SEPARATOR, operationsNames);
    }

    // test case: if the list of the not allowed operations types contains
    // the type of the operation passed as argument, the method isAuthorized must
    // return false. Otherwise, it must return true.
    @Test
    public void testIsAuthorized() throws InvalidParameterException {
        setUpVariables(notAllowedOperations);

        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);

            if (notAllowedOperations.contains(type)) {
                assertFalse(permission.isAuthorized(operation));
            } else {
                assertTrue(permission.isAuthorized(operation));
            }
        }
    }

    // test case: if the list of the not allowed operations is empty,
    // the method isAuthorized must always return true.
    @Test
    public void testIsAuthorizedAllOperationsAreAuthorized() throws InvalidParameterException {
        setUpVariables(noOperation);

        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);
            assertTrue(permission.isAuthorized(operation));
        }
    }
}
