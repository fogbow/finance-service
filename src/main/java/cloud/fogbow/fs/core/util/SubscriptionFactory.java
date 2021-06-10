package cloud.fogbow.fs.core.util;

import java.util.UUID;

import cloud.fogbow.fs.core.models.Subscription;

public class SubscriptionFactory {

    public Subscription getSubscription(String planName) {
        return new Subscription(UUID.randomUUID().toString(), 
                    new TimeUtils().getCurrentTimeMillis(), planName);
    }
}
