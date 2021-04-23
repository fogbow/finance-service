package cloud.fogbow.fs.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class MultiConsumerSynchronizedList<T> {
    private List<T> internalList;
    private boolean modifyingList;
    private Integer consumersCount;
    private Map<Integer, Integer> pointers;
    private Integer currentConsumerIndex;
    
    public MultiConsumerSynchronizedList() {
        internalList = new ArrayList<T>();
        modifyingList = false;
        consumersCount = 0;
        currentConsumerIndex = 0;
        pointers = new HashMap<Integer, Integer>();
    }
    
    public synchronized Integer startIterating() {
        while (modifyingList); 
        
        pointers.put(currentConsumerIndex, 0);
        consumersCount++;
        return currentConsumerIndex++;
    }
    
    public synchronized T getNext(Integer consumerIndex) throws InvalidParameterException {
        if (!pointers.containsKey(consumerIndex)) {
            // TODO add message
            throw new InvalidParameterException();
        }

        Integer pointer = pointers.get(consumerIndex);
        
        if (internalList.size() <= pointer) {
            return null;
        }
        
        T value = internalList.get(pointer);
        pointers.put(consumerIndex, ++pointer);
        return value;
    }
    
    public synchronized void stopIterating(Integer consumerIndex) {
        pointers.remove(consumerIndex);
        consumersCount--;
    }
    
    public synchronized void addItem(T item) {
        modifyingList = true;
        
        // stop all iterations
        while (consumersCount > 0);
        
        internalList.add(item);
        
        modifyingList = false;
    }

    public synchronized void removeItem(T item) {
        modifyingList = true;
        
        // stop all iterations
        while (consumersCount > 0);
        
        internalList.remove(item);
        
        modifyingList = false;
    }
}
