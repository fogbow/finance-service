package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InternalServerErrorException;

public class MultiConsumerSynchronizedListTest {

    private static final String ITEM_1 = "item1";
    private static final String ITEM_2 = "item2";
    private static final String ITEM_3 = "item3";

    // TODO documentation
    @Test
    public void testGetNextFromEmptyList() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        
        Integer consumerId = list.startIterating();
        String item = list.getNext(consumerId);
        list.stopIterating(consumerId);
        
        assertNull(item);
    }
    
    // TODO documentation
    @Test
    public void testAddItemAndGetNext() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        String thirdItem = list.getNext(consumerId);
        String fourthItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_1);
        assertEquals(secondItem, ITEM_2);
        assertEquals(thirdItem, ITEM_3);
        assertNull(fourthItem);
    }
    
    // TODO documentation
    @Test
    public void testAddItemAndGetNextListMultipleConsumers() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        Integer consumerId1 = list.startIterating();
        Integer consumerId2 = list.startIterating();
        
        String firstItemConsumer1 = list.getNext(consumerId1);
        String secondItemConsumer1 = list.getNext(consumerId1);
        String firstItemConsumer2 = list.getNext(consumerId2);
        String secondItemConsumer2 = list.getNext(consumerId2);
        
        String thirdItemConsumer1 = list.getNext(consumerId1);
        String thirdItemConsumer2 = list.getNext(consumerId2);
        String fourthItemConsumer1 = list.getNext(consumerId1);
        String fourthItemConsumer2 = list.getNext(consumerId2);
        
        list.stopIterating(consumerId1);
        list.stopIterating(consumerId2);
        
        assertEquals(firstItemConsumer1, ITEM_1);
        assertEquals(secondItemConsumer1, ITEM_2);
        assertEquals(thirdItemConsumer1, ITEM_3);
        assertNull(fourthItemConsumer1);
        
        assertEquals(firstItemConsumer2, ITEM_1);
        assertEquals(secondItemConsumer2, ITEM_2);
        assertEquals(thirdItemConsumer2, ITEM_3);
        assertNull(fourthItemConsumer2);
    }
 
    // TODO documentation
    @Test
    public void testRemoveItem() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        String thirdItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_1);
        assertEquals(secondItem, ITEM_2);
        assertNull(thirdItem);
        
        list.removeItem(ITEM_1);
        
        consumerId = list.startIterating();
        
        firstItem = list.getNext(consumerId);
        secondItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_2);
        assertNull(secondItem);
    }
    
    // TODO documentation
    @Test(expected = InternalServerErrorException.class)
    public void testGetNextUsingInvalidConsumerId() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        
        Integer consumerId = list.startIterating();
        list.getNext(consumerId + 1);
    }
}
