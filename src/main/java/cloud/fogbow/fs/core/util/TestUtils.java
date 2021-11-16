package cloud.fogbow.fs.core.util;

import java.util.Iterator;
import java.util.List;

public class TestUtils {

    public <T> Iterator<T> getIterator(List<T> objs) {
        return new TestIterator<T>(objs);
    }
    
    private class TestIterator<T> implements Iterator<T> {

        private int currentIndex;
        private List<T> objs;
          
        public TestIterator(List<T> objs) {
            this.currentIndex = 0;
            this.objs = objs;
        }
        
        @Override
        public boolean hasNext() {
            return currentIndex < this.objs.size();
        }

        @Override
        public T next() {
            return objs.get(currentIndex++);
        }
    }
}
