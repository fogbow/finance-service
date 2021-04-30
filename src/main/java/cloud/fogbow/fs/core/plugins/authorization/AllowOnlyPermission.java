package cloud.fogbow.fs.core.plugins.authorization;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;

public class AllowOnlyPermission implements Permission<FsOperation> {

    private String name;
    private Set<OperationType> allowedOperationTypes;
    
    public AllowOnlyPermission(Set<OperationType> allowedOperationTypes) {
        this.allowedOperationTypes = allowedOperationTypes;
    }
    
    public AllowOnlyPermission(String name, Set<OperationType> allowedOperationTypes) {
        this.name = name;
        this.allowedOperationTypes = allowedOperationTypes;
    }
    
    public AllowOnlyPermission(String permissionName) throws InvalidParameterException {
        this.name = permissionName;
        this.allowedOperationTypes = new HashSet<OperationType>();
        
        String operationTypesString = PropertiesHolder.getInstance().getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX).trim();
        
        if (!operationTypesString.isEmpty()) {
            for (String operationString : operationTypesString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
                this.allowedOperationTypes.add(OperationType.fromString(operationString.trim()));
            }
        }
    }

    @Override
    public boolean isAuthorized(FsOperation operation) {
        return this.allowedOperationTypes.contains(operation.getOperationType());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof AllowOnlyPermission) {
            return this.name.equals(((AllowOnlyPermission) o).name);
        }
        
        return false;
    }
    
    @Override
    public Set<String> getOperationsTypes() {
        HashSet<String> operationsStrings = new HashSet<String>(); 
        
        for (OperationType operation : allowedOperationTypes) {
            operationsStrings.add(operation.getValue());
        }
        
        return operationsStrings;
    }

    @Override
    public void setOperationTypes(Set operations) {
        this.allowedOperationTypes = (Set<OperationType>) operations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
