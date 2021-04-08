package cloud.fogbow.fs.core.models;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.fs.core.util.accounting.Record;

public class FinanceUser {
	private static final Logger LOGGER = Logger.getLogger(FinanceUser.class);
	public static final String PAYMENT_TYPE_KEY = "paymentType";
	public static final String PAYMENT_STATUS_KEY = "paymentStatus";
	public static final String USER_LAST_BILLING_TIME = "last_billing_time";
	
	private String id;
	private String provider;
	private String financePluginName;
	private List<Record> periodRecords;
	private boolean stoppedResources;
	private Map<String, String> properties;

	public FinanceUser() {

	}
	
	public FinanceUser(Map<String, String> properties) {
		this.stoppedResources = false;
		this.properties = properties;
        long billingTime = 0L;
        this.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
	}
	
	public List<Record> getPeriodRecords() {
		return periodRecords;
	}

	public void setPeriodRecords(List<Record> periodRecords) {
		this.periodRecords = periodRecords;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean stoppedResources() {
		return stoppedResources;
	}

	public void setStoppedResources(boolean stoppedResources) {
		this.stoppedResources = stoppedResources;
	}
	
	public String getProperty(String propertyName) {
		return this.properties.get(propertyName);
	}
	
	public void setProperty(String propertyName, String propertyValue) {
		this.properties.put(propertyName, propertyValue);
	}
	
	public String getFinancePluginName() {
		return financePluginName;
	}

	public void setFinancePluginName(String financePluginName) {
		this.financePluginName = financePluginName;
	}
}
