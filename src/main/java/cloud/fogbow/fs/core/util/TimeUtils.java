package cloud.fogbow.fs.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;

public class TimeUtils {
    private static final String DEFAULT_TIME_ZONE = "GMT0:00";
    
    private String timeZone;
    
    public TimeUtils() {
        
    }
    
    public TimeUtils(String timeZone) {
        this.timeZone = timeZone;
    }
    
	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	public String toDate(String dateFormat, long timeInMilliseconds) {
	    // FIXME should load timezone in the constructor
	    loadTimeZone();
		Date date = new Date(timeInMilliseconds); 
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(this.timeZone));
		return simpleDateFormat.format(date);
	}

    private void loadTimeZone() {
        // TODO Test
        if (this.timeZone == null) {
		    this.timeZone = PropertiesHolder.getInstance().
	                getProperty(ConfigurationPropertyKeys.TIME_ZONE, DEFAULT_TIME_ZONE);
		}
    }
}
