package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PropertiesHolder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class TimeUtilsTest {

    private String timeZone1 = "GMT0:00";
    private String timeZone2 = "GMT-3:00";
    
    // test case: When calling the method toDate, it must return a 
    // String representing a date equivalent to the given time 
    // in milliseconds since epoch, considering the given time zone.
    @Test
    public void testToDate() {
        long threeDays = 3 * 24 * 60 * 60 * 1000;
        long threeHours = 3 * 60 * 60 * 1000;
        
        TimeUtils timeUtils1 = new TimeUtils(timeZone1);
        
        assertEquals("1970-01-01", timeUtils1.toDate("yyyy-MM-dd", 0));
        assertEquals("1970-01-04", timeUtils1.toDate("yyyy-MM-dd", threeDays));
        
        TimeUtils timeUtils2 = new TimeUtils(timeZone2);
        
        assertEquals("1970-01-01", timeUtils2.toDate("yyyy-MM-dd", threeHours));
        assertEquals("1970-01-03", timeUtils2.toDate("yyyy-MM-dd", threeDays));
    }
    
    // test case: When creating a new TimeUtils instance, the constructor must
    // get the TimeZone property from the PropertiesHolder.
    @Test
    public void constructorReadsTimeZoneProperty() {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        new TimeUtils();
        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.TIME_ZONE, 
                TimeUtils.DEFAULT_TIME_ZONE);
    }
    
    // test case: When calling the roundUpTimePeriod method, it must convert the amount of 
    // time in milliseconds passed as parameter to the given TimeUnit and round up the converted
    // value to the smallest higher long value.
    @Test
    public void testRoundUpTimePeriod() throws InvalidParameterException {
        TimeUtils timeUtils = new TimeUtils(timeZone1);
        
        assertEquals(new Long(0L), timeUtils.roundUpTimePeriod(0L, TimeUnit.MILLISECONDS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1L, TimeUnit.MILLISECONDS));
        assertEquals(new Long(1000L), timeUtils.roundUpTimePeriod(1000L, TimeUnit.MILLISECONDS));
        assertEquals(new Long(1001L), timeUtils.roundUpTimePeriod(1001L, TimeUnit.MILLISECONDS));
        
        assertEquals(new Long(0L), timeUtils.roundUpTimePeriod(0L, TimeUnit.SECONDS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1L, TimeUnit.SECONDS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(999L, TimeUnit.SECONDS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1000L, TimeUnit.SECONDS));
        assertEquals(new Long(2L), timeUtils.roundUpTimePeriod(1001L, TimeUnit.SECONDS));
        
        assertEquals(new Long(0L), timeUtils.roundUpTimePeriod(0L, TimeUnit.MINUTES));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1L, TimeUnit.MINUTES));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1000L, TimeUnit.MINUTES));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(59999L, TimeUnit.MINUTES));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(60000L, TimeUnit.MINUTES));
        assertEquals(new Long(2L), timeUtils.roundUpTimePeriod(60001L, TimeUnit.MINUTES));
        
        assertEquals(new Long(0L), timeUtils.roundUpTimePeriod(0L, TimeUnit.HOURS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1L, TimeUnit.HOURS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(1000L, TimeUnit.HOURS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(60000L, TimeUnit.HOURS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(60*60000L - 1, TimeUnit.HOURS));
        assertEquals(new Long(1L), timeUtils.roundUpTimePeriod(60*60000L, TimeUnit.HOURS));
        assertEquals(new Long(2L), timeUtils.roundUpTimePeriod(60*60000L + 1, TimeUnit.HOURS));
    }
    
    // test case: When calling the roundUpTimePeriod method passing as TimeUnit Nanoseconds, 
    // Microseconds or Days, it must throw an InvalidParameterException.
    @Test
    public void testRoundUpTimePeriodInvalidTimeUnit() {
        for (TimeUnit timeUnit : Arrays.asList(TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS, TimeUnit.DAYS)) {
            try {
                new TimeUtils(timeZone1).roundUpTimePeriod(0L, timeUnit);
                Assert.fail("Expected InvalidParameterException.");
            } catch (InvalidParameterException e) {
                
            }
        }
    }
    
    // test case: When calling the roundUpTimePeriod method passing as time value a negative
    // value, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRoundUpTimePeriodNegativeTime() throws InvalidParameterException {
        new TimeUtils(timeZone1).roundUpTimePeriod(-1L, TimeUnit.MILLISECONDS);
    }
}
