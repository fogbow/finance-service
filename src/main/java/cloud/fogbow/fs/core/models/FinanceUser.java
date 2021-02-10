package cloud.fogbow.fs.core.models;

import java.util.List;
import java.util.Map;

import cloud.fogbow.accs.api.http.response.Record;

public class FinanceUser {

	private String id;
	private String provider;
	private List<Record> periodRecords;
	private boolean stoppedResources;
	private Map<String, String> properties;

	public FinanceUser() {
		
	}
	
	public FinanceUser(Map<String, String> properties) {
		this.properties = properties;
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
}
