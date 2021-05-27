package cloud.fogbow.fs.core.plugins;

import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;

@Deprecated
public class PaymentManagerInstantiator {

	private static FsClassFactory classFactory = new FsClassFactory();

    public static PaymentManager getPaymentManager(String className, InMemoryFinanceObjectsHolder objectHolder, String planName) {
        return (PaymentManager) classFactory.createPluginInstance(className, objectHolder, planName);
    }
}
