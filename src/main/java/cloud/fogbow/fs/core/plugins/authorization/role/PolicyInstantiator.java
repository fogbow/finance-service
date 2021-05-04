package cloud.fogbow.fs.core.plugins.authorization.role;

import java.io.File;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.models.policy.XMLRolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

public class PolicyInstantiator {
    private FsClassFactory classFactory;

    public PolicyInstantiator() {
        this.classFactory = new FsClassFactory();
    }
    
    public RolePolicy<FsOperation> getRolePolicyInstance(String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        
        // TODO check this
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<FsOperation>) this.classFactory.createPluginInstance(policyType, new PermissionInstantiator<FsOperation>(classFactory), policyString, adminRole, path);            
        } else {
            return new XMLRolePolicy<FsOperation>(new PermissionInstantiator<FsOperation>(classFactory), policyString, adminRole, path);
        }
    }
    
    public RolePolicy<FsOperation> getRolePolicyInstanceFromFile(String policyFileName) throws ConfigurationErrorException, WrongPolicyTypeException {
        File policyFile = new File(policyFileName);
        
        // TODO check this
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<FsOperation>) this.classFactory.createPluginInstance(policyType, new PermissionInstantiator<FsOperation>(classFactory), policyFile, adminRole, path);            
        } else {
            return new XMLRolePolicy<FsOperation>(new PermissionInstantiator<FsOperation>(classFactory), policyFile, adminRole, path);
        }
    }
}
