package cloud.fogbow.fs.core.util.list;

import java.util.Arrays;
import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;

/**
 * This builder is responsible for providing methods for constructing
 * multiple types of iterators of MultiConsumerSynchronizedList objects.
 * 
 * The iterators are implemented as inner classes and use processors described
 * by the interfaces MultiConsumerSynchronizedListConsumer and MultiConsumerSynchronizedListPredicate.
 */
public class MultiConsumerSynchronizedListIteratorBuilder<T> {
    
    private MultiConsumerSynchronizedList<T> list;
    private List<Object> args;
    private String errorMessage;

    public MultiConsumerSynchronizedListIteratorBuilder() {
        this.errorMessage = "";
    }
    
    public MultiConsumerSynchronizedListIteratorBuilder<T> processList(MultiConsumerSynchronizedList<T> list) {
        this.list = list;
        return this;
    }

    public MultiConsumerSynchronizedListIteratorBuilder<T> usingAsArgs(Object... args) {
        this.args = Arrays.asList(args);
        return this;
    }
    
    public MultiConsumerSynchronizedListIteratorBuilder<T> usingAsErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
    
    public MultiConsumerSynchronizedListProcessIterator<T> usingAsProcessor(MultiConsumerSynchronizedListConsumer<T> processor) {
        return new MultiConsumerSynchronizedListProcessIterator<T>(this.list, processor, this.args, this.errorMessage);
    }
    
    public MultiConsumerSynchronizedListSelectIterator<T> usingAsPredicate(MultiConsumerSynchronizedListPredicate<T> predicate) {
        return new MultiConsumerSynchronizedListSelectIterator<T>(this.list, predicate, this.args, this.errorMessage);
    }
    
    /**
     * This interface describes a processor of objects of param type T.
     * The operation it describes yields no return value.
     *
     * @param <T> The type of the object to process.
     */
    @FunctionalInterface
    public interface MultiConsumerSynchronizedListConsumer<T> {
        /**
         * Processes the object t, using the list of parameters u.
         * 
         * @param t the object to process.
         * @param u a list of parameters to be used in the processing.
         */
        void accept(T t, List<Object> u) throws InternalServerErrorException, InvalidParameterException;
    }
    
    /**
     * This interface describes an evaluator of a certain property of 
     * objects of param type T.
     * The operation it describes yields a boolean as return value.
     *
     * @param <T> The type of the object to process.
     */
    @FunctionalInterface
    public interface MultiConsumerSynchronizedListPredicate<T> {
        /**
         * Evaluates a property of object t as true or false, using the list of
         * parameters u.
         * 
         * @param t the object to process.
         * @param u a list of parameters to be used in the processing.
         * @return a boolean stating whether the property is true or false for the object.
         */
        boolean test(T t, List<Object> u) throws InternalServerErrorException, InvalidParameterException;
    }
    
    /**
     * Implements an iterator over MultiConsumerSynchronizedList objects of type P using a
     * MultiConsumerSynchronizedListConsumer to process each of the elements of the list.
     */
    public class MultiConsumerSynchronizedListProcessIterator<P> {
        
        private MultiConsumerSynchronizedList<P> list;
        private MultiConsumerSynchronizedListConsumer<P> processor;
        private List<Object> args;
        private String errorMessage;

        public MultiConsumerSynchronizedListProcessIterator(MultiConsumerSynchronizedList<P> list,
                MultiConsumerSynchronizedListConsumer<P> processor, List<Object> args, String errorMessage) {
            this.list = list;
            this.processor = processor;
            this.args = args;
            this.errorMessage = errorMessage;
        }

        public void process() throws InternalServerErrorException, InvalidParameterException {
            Integer consumerId = list.startIterating();
            
            while (true) {
                
                try {
                    tryToProcess(list, consumerId, processor, args);
                    list.stopIterating(consumerId);
                    break;
                } catch (ModifiedListException e) {
                    consumerId = list.startIterating();
                } catch (InvalidParameterException e) { 
                    list.stopIterating(consumerId);
                    throw new InvalidParameterException(this.errorMessage);
                } catch (Exception e) {
                    System.out.println(consumerId);
                    list.stopIterating(consumerId);
                    throw new InternalServerErrorException(e.getMessage());
                }
            }
        }
        
        private void tryToProcess(MultiConsumerSynchronizedList<P> listToProcess, Integer consumerId, 
                MultiConsumerSynchronizedListConsumer<P> processor, List<Object> args) throws Exception {
            P element = listToProcess.getNext(consumerId);
            
            while (element != null) {
                processor.accept(element, args);
                element = listToProcess.getNext(consumerId);
            }   
        }
    }
    
    /**
     * Implements an iterator over MultiConsumerSynchronizedList objects of type P, using a
     * MultiConsumerSynchronizedListPredicate to select the correct list element to return.
     */
    public class MultiConsumerSynchronizedListSelectIterator<P> {
        
        private MultiConsumerSynchronizedList<P> list;
        private MultiConsumerSynchronizedListPredicate<P> processor;
        private List<Object> args;
        private String errorMessage;
        
        public MultiConsumerSynchronizedListSelectIterator(MultiConsumerSynchronizedList<P> list,
                MultiConsumerSynchronizedListPredicate<P> processor, List<Object> args, String errorMessage) {
            this.list = list;
            this.processor = processor;
            this.args = args;
            this.errorMessage = errorMessage;
        }

        public P select() throws InternalServerErrorException, InvalidParameterException {
            P element = null;
            Integer consumerId = list.startIterating();
                    
            while (true) {                
                try {
                    element = tryToGet(list, consumerId, processor, args);
                    list.stopIterating(consumerId);
                    break;
                } catch (ModifiedListException e) {
                    consumerId = list.startIterating();
                } catch (InvalidParameterException e) {
                    list.stopIterating(consumerId);
                    throw e;
                } catch (Exception e) {
                    list.stopIterating(consumerId);
                    throw new InternalServerErrorException(e.getMessage());
                }
            }
            
            return element;
        }
        
        private P tryToGet(MultiConsumerSynchronizedList<P> listToProcess, Integer consumerId,
                MultiConsumerSynchronizedListPredicate<P> processor, List<Object> args) throws InternalServerErrorException, ModifiedListException, 
                InvalidParameterException {
            P element = listToProcess.getNext(consumerId);
            
            while (element != null) {
                boolean result = processor.test(element, args);
                
                if (result) {
                    return element;
                }
                
                element = listToProcess.getNext(consumerId);
            }
            
            throw new InvalidParameterException(this.errorMessage);
        }
    }
}
