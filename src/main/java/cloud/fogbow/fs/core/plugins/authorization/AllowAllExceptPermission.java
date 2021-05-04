package cloud.fogbow.fs.core.plugins.authorization;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.fs.core.models.OperationType;

public class AllowAllExceptPermission implements Permission<FsOperation> {

    private String name;
    private Set<OperationType> notAllowedOperationTypes;
    
    public AllowAllExceptPermission() {
        
    }
    
    public AllowAllExceptPermission(Set<OperationType> notAllowedOperationTypes) {
        this.notAllowedOperationTypes = notAllowedOperationTypes;
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
    public void setOperationTypes(Set<String> operations) throws InvalidParameterException {
        Set<OperationType> fsOperations = new HashSet<OperationType>();
        
        for (String operationName : operations) {
            fsOperations.add(OperationType.fromString(operationName));
        }
        
        this.notAllowedOperationTypes = fsOperations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
