package cloud.fogbow.fs.core.util.list;

import java.util.Arrays;
import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;

// TODO test
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
    
    // TODO documentation
    @FunctionalInterface
    public interface MultiConsumerSynchronizedListConsumer<T> {
        void accept(T t, List<Object> u) throws InternalServerErrorException, InvalidParameterException;
    }
    
    // TODO documentation
    @FunctionalInterface
    public interface MultiConsumerSynchronizedListPredicate<T> {
        boolean test(T t, List<Object> u) throws InternalServerErrorException, InvalidParameterException;
    }
    
    // TODO documentation
    public class MultiConsumerSynchronizedListProcessIterator<A> {
        
        private MultiConsumerSynchronizedList<A> list;
        private MultiConsumerSynchronizedListConsumer<A> processor;
        private List<Object> args;
        private String errorMessage;

        public MultiConsumerSynchronizedListProcessIterator(MultiConsumerSynchronizedList<A> list,
                MultiConsumerSynchronizedListConsumer<A> processor, List<Object> args, String errorMessage) {
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
                    throw new InvalidParameterException(this.errorMessage);
                } catch (Exception e) {
                    list.stopIterating(consumerId);
                    throw new InternalServerErrorException(e.getMessage());
                }
            }
        }
        
        private void tryToProcess(MultiConsumerSynchronizedList<A> listToProcess, Integer consumerId, 
                MultiConsumerSynchronizedListConsumer<A> processor, List<Object> args) throws Exception {
            A element = listToProcess.getNext(consumerId);
            
            while (element != null) {
                processor.accept(element, args);
                element = listToProcess.getNext(consumerId);
            }   
        }
    }
    
    // TODO documentation
    public class MultiConsumerSynchronizedListSelectIterator<A> {
        
        private MultiConsumerSynchronizedList<A> list;
        private MultiConsumerSynchronizedListPredicate<A> processor;
        private List<Object> args;
        private String errorMessage;
        
        public MultiConsumerSynchronizedListSelectIterator(MultiConsumerSynchronizedList<A> list,
                MultiConsumerSynchronizedListPredicate<A> processor, List<Object> args, String errorMessage) {
            this.list = list;
            this.processor = processor;
            this.args = args;
            this.errorMessage = errorMessage;
        }

        public A select() throws InternalServerErrorException, InvalidParameterException {
            A element = null;
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
        
        private A tryToGet(MultiConsumerSynchronizedList<A> listToProcess, Integer consumerId,
                MultiConsumerSynchronizedListPredicate<A> processor, List<Object> args) throws InternalServerErrorException, ModifiedListException, 
                InvalidParameterException {
            A element = listToProcess.getNext(consumerId);
            
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
