package cloud.fogbow.fs.core.plugins.authorization;

import java.util.Set;

import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.fs.core.FsClassFactory;

public class PermissionInstantiator {
    private FsClassFactory classFactory;
    
    public PermissionInstantiator() {
        this.classFactory = new FsClassFactory();
    }
    
    public Permission<FsOperation> getPermissionInstance(String type, String ... params) {
        return (Permission<FsOperation>) this.classFactory.createPluginInstance(type, params);
    }
    
    public Permission<FsOperation> getPermissionInstance(String type, String name, Set<FsOperation> operations) {
        Permission<FsOperation> instance = (Permission<FsOperation>) this.classFactory.createPluginInstance(type);
        
        // Permission constructors require a Set as argument.
        // Since it is difficult to implement a ClassFactory able 
        // to use constructors that have interfaces in their
        // signatures, here we create Permissions using
        // the default constructor and then set the parameters.
        instance.setOperationTypes((Set) operations);
        instance.setName(name);
        return instance;
    }
}
