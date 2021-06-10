package cloud.fogbow.fs.core.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.util.SubscriptionFactory;
import cloud.fogbow.fs.core.util.TimeUtils;

@Entity
@Table(name = "finance_user_table")
public class FinanceUser implements Serializable {
    private static final long serialVersionUID = 1L;

    // TODO documentation
    /*
     * Properties
     */
    public static final String USER_LAST_BILLING_TIME = "last_billing_time";

    /*
     * Finance state
     */
    public static final String PROPERTY_TYPE_KEY = "PROPERTY_TYPE";
    public static final String INVOICE_PROPERTY_TYPE = "INVOICE";
    public static final String ALL_USER_INVOICES_PROPERTY_NAME = "ALL_USER_INVOICES";
    public static final String INVOICES_SEPARATOR = ",";
    public static final String CREDITS_PROPERTY_TYPE = "CREDITS";
    public static final String USER_CREDITS = "USER_CREDITS";
    public static final String CREDITS_TO_ADD = "CREDITS_TO_ADD";

    /*
     * Database column names
     */
    private static final String STOPPED_RESOURCES_COLUMN_NAME = "stopped_resources";
    private static final String PROPERTIES_COLUMN_NAME = "properties";
    private static final String INVOICES_COLUMN_NAME = "invoices";
    private static final String INACTIVE_SUBSCRIPTIONS_COLUMN_NAME = "inactive_subscriptions";
    private static final String LAST_SUBSCRIPTIONS_COLUMN_NAME = "last_subscriptions_debts";
    
    @EmbeddedId
    private UserId userId;
    
    @Column(name = STOPPED_RESOURCES_COLUMN_NAME)
	private boolean stoppedResources;
    
    @Column(name = PROPERTIES_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> properties;
    
    @Column(name = INVOICES_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
	private List<Invoice> invoices;
    
    @OneToOne(cascade={CascadeType.ALL})
	private UserCredits credits;

    @OneToOne(cascade={CascadeType.ALL})
    private Subscription activeSubscription;
    
    @Column(name = INACTIVE_SUBSCRIPTIONS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
    private List<Subscription> inactiveSubscriptions;
    
    @Column(name = LAST_SUBSCRIPTIONS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> lastSubscriptionsDebts;
    
    @Transient
    private SubscriptionFactory subscriptionFactory;
    
    @Transient
    private TimeUtils timeUtils;
    
    public FinanceUser() {

	}
	
	public FinanceUser(Map<String, String> properties) {
		this.stoppedResources = false;
		this.properties = properties;
        long billingTime = System.currentTimeMillis();
        this.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
        this.activeSubscription = null;
        this.inactiveSubscriptions = new ArrayList<Subscription>();
        this.lastSubscriptionsDebts = new ArrayList<String>();
        this.subscriptionFactory = new SubscriptionFactory();
        this.timeUtils = new TimeUtils();
	}
	
	public FinanceUser(UserId userId, boolean stoppedResources, Map<String, String> properties, 
	        List<Invoice> invoices, UserCredits credits, Subscription activeSubscription, 
	        List<Subscription> inactiveSubscriptions, List<String> lastSubscriptionsDebts, 
	        SubscriptionFactory subscriptionFactory, TimeUtils timeUtils) {
	    this.userId = userId;
	    this.stoppedResources = stoppedResources;
	    this.properties = properties;
	    this.invoices = invoices;
	    this.credits = credits;
	    this.activeSubscription = activeSubscription;
	    this.inactiveSubscriptions = inactiveSubscriptions;
	    this.lastSubscriptionsDebts = lastSubscriptionsDebts;
	    this.subscriptionFactory = subscriptionFactory;
	    this.timeUtils = timeUtils;
	}
	
	public void setUserId(String id, String provider) {
	    this.userId = new UserId(id, provider);
	}
	
	public String getProvider() {
	    return this.userId.getProvider();
	}

	public String getId() {
	    return this.userId.getUserId();
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

    public UserCredits getCredits() {
        return credits;
    }

    public void setCredits(UserCredits credits) {
        this.credits = credits;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void addInvoice(Invoice invoice) {
        this.invoices.add(invoice);
    }
    
    public void addInvoiceAsDebt(Invoice invoice) {
        this.invoices.add(invoice);
        this.lastSubscriptionsDebts.add(invoice.getInvoiceId());
    }
    
    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }
    
    public boolean invoicesArePaid() {
        for (Invoice invoice : invoices) {
            if (!invoice.getState().equals(InvoiceState.PAID)) {
                return false;
            }
        }
        
        return true;
    }
    
    public List<String> getLastSubscriptionsDebts() {
        return this.lastSubscriptionsDebts;
    }
    
    public void removeDebt(String invoiceId) {
        this.lastSubscriptionsDebts.remove(invoiceId);
    }

    public String getFinancePluginName() {
        if (this.activeSubscription == null) {
            return null;
        } else {
            return this.activeSubscription.getPlanName();
        }
    }
    
    public void subscribeToPlan(String planName) throws InvalidParameterException {
        if (this.activeSubscription == null) {
            this.activeSubscription = this.subscriptionFactory.getSubscription(planName);  
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_ALREADY_SUBSCRIBED_TO_A_PLAN, 
                            this.userId, this.activeSubscription.getPlanName()));
        }
    }
    
    public void unsubscribe() throws InvalidParameterException {
        if (this.activeSubscription != null) {
            this.activeSubscription.setEndTime(this.timeUtils.getCurrentTimeMillis());
            this.inactiveSubscriptions.add(activeSubscription);
            this.activeSubscription = null;
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_NOT_SUBSCRIBED_TO_ANY_PLAN, 
                            this.userId));
        }
    }
    
    public boolean isSubscribed() {
        return this.activeSubscription != null;
    }
    
    public String getFinanceState(String property) throws InvalidParameterException {
        String propertyValue = "";
        
        switch(property) {
            case ALL_USER_INVOICES_PROPERTY_NAME: propertyValue = getAllInvoices(); break;
            case USER_CREDITS: propertyValue = getUserCredits(); break;
            default: throw new InvalidParameterException(
                    String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
        }

        return propertyValue;
    }

    private String getAllInvoices() {
        String propertyValue;
        List<String> invoiceJsonReps = new ArrayList<String>();

        for (Invoice invoice : invoices) {
            String invoiceJson = invoice.toString();
            invoiceJsonReps.add(invoiceJson);
        }

        propertyValue = "[" + String.join(INVOICES_SEPARATOR, invoiceJsonReps) + "]";
        return propertyValue;
    }
    
    private String getUserCredits() {
        String propertyValue;
        propertyValue = String.valueOf(credits.getCreditsValue());
        return propertyValue;
    }
    
    public void updateFinanceState(Map<String, String> financeState) throws InvalidParameterException {
        if (!financeState.containsKey(PROPERTY_TYPE_KEY)) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_STATE_PROPERTY, PROPERTY_TYPE_KEY));
        }
        
        String propertyType = financeState.get(PROPERTY_TYPE_KEY);
        
        switch(propertyType) {
            case INVOICE_PROPERTY_TYPE: updateInvoice(financeState); break;
            case CREDITS_PROPERTY_TYPE: updateCredits(financeState); break;
            default: throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_STATE_PROPERTY, propertyType, PROPERTY_TYPE_KEY));
        }
    }

    private void updateCredits(Map<String, String> financeState) throws InvalidParameterException {
        if (!financeState.containsKey(CREDITS_TO_ADD)) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_STATE_PROPERTY, CREDITS_TO_ADD));
        }
        
        Double valueToAdd = 0.0;
        String propertyValue = "";
        
        try {
            propertyValue = financeState.get(CREDITS_TO_ADD);
            valueToAdd = Double.valueOf(propertyValue);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_STATE_PROPERTY, propertyValue, CREDITS_TO_ADD));
        }

        credits.addCredits(valueToAdd);
    }

    private void updateInvoice(Map<String, String> financeState) throws InvalidParameterException {
        financeState.remove(PROPERTY_TYPE_KEY);
        
        for (String invoiceId : financeState.keySet()) {
            InvoiceState newState = InvoiceState.fromValue(financeState.get(invoiceId));
            
            // TODO test
            if (lastSubscriptionsDebts.contains(invoiceId) &&
                    newState.equals(InvoiceState.PAID)) {
                lastSubscriptionsDebts.remove(invoiceId);
            }
            
            for (Invoice invoice : invoices) {
                if (invoice.getInvoiceId().equals(invoiceId)) {
                    invoice.setState(newState);
                }
            }
        }
    }
}
