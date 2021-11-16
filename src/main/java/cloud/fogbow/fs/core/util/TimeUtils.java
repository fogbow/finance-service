package cloud.fogbow.fs.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;

public class TimeUtils {
    @VisibleForTesting
    static final String DEFAULT_TIME_ZONE = "GMT0:00";
    
    private static final Long MILLISECONDS_CONVERSION_FACTOR = 1L;
    private static final Long SECONDS_CONVERSION_FACTOR = 1000L;
    private static final Long MINUTES_CONVERSION_FACTOR = 60 * SECONDS_CONVERSION_FACTOR;
    private static final Long HOURS_CONVERSION_FACTOR = 60 * MINUTES_CONVERSION_FACTOR;
    
    private String timeZone;
    
    public TimeUtils() {
        this.timeZone = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.TIME_ZONE, DEFAULT_TIME_ZONE);
    }
    
    public TimeUtils(String timeZone) {
        this.timeZone = timeZone;
    }
    
	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	public String toDate(String dateFormat, long timeInMilliseconds) {
		Date date = new Date(timeInMilliseconds); 
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(this.timeZone));
		return simpleDateFormat.format(date);
	}
	
    public Long roundUpTimePeriod(Long periodInMilliseconds, TimeUnit timeUnit) throws InvalidParameterException {
        if (periodInMilliseconds < 0) {
            throw new InvalidParameterException(Messages.Exception.NEGATIVE_TIME_VALUE);
        }

        Long conversionFactor = MILLISECONDS_CONVERSION_FACTOR;

        switch (timeUnit) {
            case MILLISECONDS: break;
            case SECONDS: conversionFactor = SECONDS_CONVERSION_FACTOR; break;
            case MINUTES: conversionFactor = MINUTES_CONVERSION_FACTOR; break;
            case HOURS: conversionFactor = HOURS_CONVERSION_FACTOR; break;
            default: throw new InvalidParameterException(String.format(Messages.Exception.INVALID_TIME_UNIT, timeUnit));
        }

        if (periodInMilliseconds % conversionFactor == 0) {
            return periodInMilliseconds / conversionFactor; 
        } else {
            return periodInMilliseconds / conversionFactor + 1;
        } 
    }
}
