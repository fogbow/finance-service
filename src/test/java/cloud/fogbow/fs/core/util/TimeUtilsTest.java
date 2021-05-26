package cloud.fogbow.fs.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TimeUtilsTest {

    private String timeZone1 = "GMT0:00";
    private String timeZone2 = "GMT-3:00";
    
    // test case: When calling the method toDate, it must return a 
    // String representing a date equivalent to the given time 
    // in milliseconds since epoch, considering the given time zone.
    @Test
    public void test() {
        long threeDays = 3 * 24 * 60 * 60 * 1000;
        long threeHours = 3 * 60 * 60 * 1000;
        
        TimeUtils timeUtils1 = new TimeUtils(timeZone1);
        
        assertEquals("1970-01-01", timeUtils1.toDate("yyyy-MM-dd", 0));
        assertEquals("1970-01-04", timeUtils1.toDate("yyyy-MM-dd", threeDays));
        
        TimeUtils timeUtils2 = new TimeUtils(timeZone2);
        
        assertEquals("1970-01-01", timeUtils2.toDate("yyyy-MM-dd", threeHours));
        assertEquals("1970-01-03", timeUtils2.toDate("yyyy-MM-dd", threeDays));
    }

}
