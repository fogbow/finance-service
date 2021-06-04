package cloud.fogbow.fs.core.util.list;

public class MultiConsumerSynchronizedListFactory {
    public <T> MultiConsumerSynchronizedList<T> getList() {
        return new MultiConsumerSynchronizedList<T>();
    }
}
