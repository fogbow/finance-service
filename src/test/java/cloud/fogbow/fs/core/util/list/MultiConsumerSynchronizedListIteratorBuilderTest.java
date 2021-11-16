package cloud.fogbow.fs.core.util.list;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListIteratorBuilder.MultiConsumerSynchronizedListConsumer;
import cloud.fogbow.fs.core.util.list.MultiConsumerSynchronizedListIteratorBuilder.MultiConsumerSynchronizedListPredicate;

public class MultiConsumerSynchronizedListIteratorBuilderTest {

    private static final int CONSUMER_ID = 0;
    private static final Integer ITEM_1 = 1;
    private static final Integer ITEM_2 = 2;
    private static final Integer ITEM_3 = 3;
    private static final Integer ITEM_4 = 4;
    private static final String ERROR_MESSAGE = "error_message";
    private TestObject testObject1;
    private TestObject testObject2;
    private TestObject testObject3;
    private MultiConsumerSynchronizedListIteratorBuilder<Integer> integerIteratorBuilder;
    private MultiConsumerSynchronizedListIteratorBuilder<TestObject> testObjectIteratorBuilder;
    private MultiConsumerSynchronizedList<Integer> integerList;
    private MultiConsumerSynchronizedList<TestObject> objectList;
    private MultiConsumerSynchronizedList<TestObject> emptyObjectList;
    private MultiConsumerSynchronizedListPredicate<Integer> findDoubleValue;
    private MultiConsumerSynchronizedListConsumer<TestObject> startAllObjects;

    @Before
    public void setUp() {
        integerIteratorBuilder = new MultiConsumerSynchronizedListIteratorBuilder<Integer>();
        testObjectIteratorBuilder = new MultiConsumerSynchronizedListIteratorBuilder<TestObject>();
        
        testObject1 = Mockito.mock(TestObject.class);
        testObject2 = Mockito.mock(TestObject.class);
        testObject3 = Mockito.mock(TestObject.class);
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListSelectIterator and calling
    // the select method, it must return the first object in the list passed as parameter 
    // that matches the given predicate.
    @Test
    public void testBuildSelectIterator() 
            throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        setUpSynchronizedList();
        setUpSelector();
        
        assertEquals(ITEM_2, integerIteratorBuilder.processList(integerList).
                usingAsArgs(1).usingAsPredicate(findDoubleValue).select());
        assertEquals(ITEM_4, integerIteratorBuilder.processList(integerList).
                usingAsArgs(2).usingAsPredicate(findDoubleValue).select());
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListSelectIterator and calling
    // the select method, if none of the objects contained in the list passed as parameter matches 
    // the given predicate, it must throw an InvalidParameterException using the error message passed 
    // as parameter.
    @Test
    public void testBuildSelectIteratorElementNotFound() 
            throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        setUpSynchronizedList();
        setUpSelector();
        
        try {
            integerIteratorBuilder.processList(integerList).usingAsArgs(3).
                usingAsErrorMessage(ERROR_MESSAGE).usingAsPredicate(findDoubleValue).select();
            Assert.fail("Expected InvalidParameterException.");
        } catch (InvalidParameterException e) {
            assertEquals(ERROR_MESSAGE, e.getMessage());
        }
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListSelectIterator and calling
    // the select method, if the list throws a ModifiedListException, the method must restart the
    // iteration over the list elements, returning the correct value.
    @Test
    public void testBuildSelectIteratorModifiedListException() 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedListModifiedListException();
        setUpSelector();
        
        assertEquals(ITEM_4, integerIteratorBuilder.processList(integerList).usingAsArgs(2).
                usingAsPredicate(findDoubleValue).select());
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListSelectIterator and calling
    // the select method, if the list throws an exception other than ModifiedListException, the method
    // must stop the iteration and throw an InternalServerErrorException.
    @Test
    public void testBuildSelectIteratorListThrowsException()
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedListGenericException();
        setUpSelector();
        
        try {
            integerIteratorBuilder.processList(integerList).usingAsArgs(2).
            usingAsPredicate(findDoubleValue).select();
        } catch (InternalServerErrorException e) {

        }
        
        Mockito.verify(this.integerList).stopIterating(0);
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListSelectIterator and calling
    // the select method, if the predicate method throws an exception, the method
    // must stop the iteration and throw an InternalServerErrorException.
    @Test
    public void testBuildSelectIteratorSelectorThrowsException() 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedList();
        setUpSelectorThrowsException();
        
        try {
            integerIteratorBuilder.processList(integerList).usingAsArgs(2).
                usingAsPredicate(findDoubleValue).select();
        } catch (InternalServerErrorException e) {

        }
        
        Mockito.verify(this.integerList).stopIterating(0);
    }

    // test case: When creating a new MultiConsumerSynchronizedListProcessIterator and calling
    // the process method, it must call the accept method of the processor for each of the objects
    // contained in the list.
    @Test
    public void testBuildProcessIterator() 
            throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        setUpSynchronizedList();
        setUpProcessor();
        
        testObjectIteratorBuilder.processList(objectList).usingAsArgs().
            usingAsProcessor(startAllObjects).process();
        
        Mockito.verify(testObject1).start();
        Mockito.verify(testObject2).start();
        Mockito.verify(testObject3).start();
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListProcessIterator and calling
    // the process method, if the list to process is empty, then it must not call the accept method
    // for any object.
    @Test
    public void testBuildProcessIteratorEmptyList() 
            throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        setUpSynchronizedList();
        setUpProcessor();
        
        testObjectIteratorBuilder.processList(emptyObjectList).usingAsArgs().
            usingAsProcessor(startAllObjects).process();
        
        Mockito.verify(testObject1, Mockito.never()).start();
        Mockito.verify(testObject2, Mockito.never()).start();
        Mockito.verify(testObject3, Mockito.never()).start();
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListProcessIterator and calling
    // the process method, if the list to process throws a ModifiedListException, then the method 
    // must restart the iteration over the list and call the accept method of the processor for each of
    // the objects correctly.
    @Test
    public void testBuildProcessIteratorModifiedListException() 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedListModifiedListException();
        setUpProcessor();
        
        testObjectIteratorBuilder.processList(objectList).usingAsArgs().
            usingAsProcessor(startAllObjects).process();
        
        Mockito.verify(testObject1, Mockito.times(2)).start();
        Mockito.verify(testObject2).start();
        Mockito.verify(testObject3).start();
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListProcessIterator and calling
    // the process method, if the list to process throws an exception other than ModifiedListException, 
    // then the method must stop the iteration over the list and throw an InternalServerErrorException.
    @Test
    public void testBuildProcessIteratorListThrowsException()
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedListGenericException();
        setUpProcessor();
        
        try {
            testObjectIteratorBuilder.processList(objectList).usingAsArgs().usingAsProcessor(startAllObjects).process();
        } catch (InternalServerErrorException e) {

        }
        
        Mockito.verify(this.objectList).stopIterating(0);
    }
    
    // test case: When creating a new MultiConsumerSynchronizedListProcessIterator and calling
    // the process method, if the processor throws an exception, then the method must stop the iteration 
    // over the list and throw an InternalServerErrorException.
    @Test
    public void testBuildProcessIteratorProcessorThrowsException() 
            throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        setUpSynchronizedList();
        setUpProcessorThrowsException();
        
        try {
            testObjectIteratorBuilder.processList(objectList).usingAsArgs().usingAsProcessor(startAllObjects).process();
        } catch (InternalServerErrorException e) {

        }
        
        Mockito.verify(this.objectList).stopIterating(0);
    }
    
    private void setUpSelector() {
        this.findDoubleValue = (value, args) -> {
            for (Object arg : args) {
                Integer valueToSearch = (Integer) arg;
                if (value == 2*valueToSearch) {
                    return true;
                }
            }
            return false;
        };
    }
    
    private void setUpSelectorThrowsException() {
        this.findDoubleValue = (value, args) -> {
            throw new InternalServerErrorException();
        };
    }
    
    private void setUpProcessor() {
        this.startAllObjects = (object, args) -> {
            object.start();
        };
    }
    
    private void setUpProcessorThrowsException() {
        this.startAllObjects = (object, args) -> {
            throw new InternalServerErrorException();
        };
    }
    
    private void setUpSynchronizedList() throws InternalServerErrorException, ModifiedListException {
        integerList = getListMock();
        Mockito.when(integerList.getNext(0)).thenReturn(ITEM_1, ITEM_2, ITEM_3, ITEM_4, null,
                ITEM_1, ITEM_2, ITEM_3, ITEM_4, null);
        
        objectList = getListMock();
        Mockito.when(objectList.getNext(0)).thenReturn(testObject1, testObject2, testObject3, null);
        
        emptyObjectList = getListMock();
        Mockito.when(emptyObjectList.getNext(0)).thenReturn(null);
    }
    
    private void setUpSynchronizedListModifiedListException() throws InternalServerErrorException, ModifiedListException {
        integerList = getListMock();
        Mockito.when(integerList.getNext(0)).thenReturn(ITEM_1, ITEM_2).
            thenThrow(new ModifiedListException()).
            thenReturn(ITEM_1, ITEM_2, ITEM_3, ITEM_4, null);
        
        objectList = getListMock();
        Mockito.when(objectList.getNext(0)).thenReturn(testObject1).
            thenThrow(new ModifiedListException()).
            thenReturn(testObject1, testObject2, testObject3, null);
    }
    
    private void setUpSynchronizedListGenericException() throws InternalServerErrorException, ModifiedListException {
        integerList = getListMock();
        Mockito.when(integerList.getNext(0)).thenReturn(ITEM_1, ITEM_2).
            thenThrow(new InternalServerErrorException());
        
        objectList = getListMock();
        Mockito.when(objectList.getNext(0)).thenReturn(testObject1).
            thenThrow(new InternalServerErrorException());
    }
     
    private <T> MultiConsumerSynchronizedList<T> getListMock() {
        MultiConsumerSynchronizedList<T> mock = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(mock.startIterating()).thenReturn(CONSUMER_ID);
        return mock;
    }
    
    private static class TestObject { 
        public void start() {
            
        }
    }
}
