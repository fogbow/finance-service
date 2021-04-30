package cloud.fogbow.fs.core.plugins.authorization;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;

public class AllowAllExceptPermission implements Permission<FsOperation> {

    private String name;
    private Set<OperationType> notAllowedOperationTypes;
    
    public AllowAllExceptPermission() {
        
    }
    
    public AllowAllExceptPermission(Set<OperationType> notAllowedOperationTypes) {
        this.notAllowedOperationTypes = notAllowedOperationTypes;
    }
    
    public AllowAllExceptPermission(String name, Set<OperationType> notAllowedOperationTypes) {
        this.name = name;
        this.notAllowedOperationTypes = notAllowedOperationTypes;
    }
    
    public AllowAllExceptPermission(String permissionName) throws InvalidParameterException {
        this.notAllowedOperationTypes = new HashSet<OperationType>();
        this.name = permissionName;
        
        String operationTypesString = PropertiesHolder.getInstance().getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX).trim();
        
        if (!operationTypesString.isEmpty()) {
            for (String operationString : operationTypesString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
                this.notAllowedOperationTypes.add(OperationType.fromString(operationString.trim()));
            }            
        }
    }

    @Override
    public boolean isAuthorized(FsOperation operation) {
        return !this.notAllowedOperationTypes.contains(operation.getOperationType());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof AllowAllExceptPermission) {
            return this.name.equals(((AllowAllExceptPermission) o).name);
        }
        
        return false;
    }
    
    @Override
    public Set<String> getOperationsTypes() {
        HashSet<String> operationsStrings = new HashSet<String>(); 
        
        for (OperationType operation : notAllowedOperationTypes) {
            operationsStrings.add(operation.getValue());
        }
        
        return operationsStrings;
    }

    @Override
    public void setOperationTypes(Set operations) {
        this.notAllowedOperationTypes = (Set<OperationType>) operations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
