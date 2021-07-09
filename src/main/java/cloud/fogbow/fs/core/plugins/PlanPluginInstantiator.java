package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.InMemoryUsersHolder;

public class PlanPluginInstantiator {
    private static FsClassFactory classFactory = new FsClassFactory();

    public static PersistablePlanPlugin getPlan(String className, String planName, InMemoryUsersHolder usersHolder) {
        return (PersistablePlanPlugin) classFactory.createPluginInstance(className, planName, usersHolder);
    }

    public static PersistablePlanPlugin getPlan(String className, String planName, Map<String, String> pluginOptions,
            InMemoryUsersHolder usersHolder) {
        return (PersistablePlanPlugin) classFactory.createPlanPluginInstance(className, planName, usersHolder, pluginOptions);
    }
}
