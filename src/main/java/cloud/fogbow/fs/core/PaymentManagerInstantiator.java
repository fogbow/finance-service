package cloud.fogbow.fs.core;

import cloud.fogbow.fs.core.plugins.PaymentManager;

public class PaymentManagerInstantiator {

	private static FsClassFactory classFactory = new FsClassFactory();

    public static PaymentManager getPaymentManager(String className, InMemoryFinanceObjectsHolder objectHolder, String planName) {
        return (PaymentManager) classFactory.createPluginInstance(className, objectHolder, planName);
    }
}
