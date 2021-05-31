package cloud.fogbow.fs.core.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.util.TimeUtils;

@Entity
@Table(name = "finance_user_table")
public class FinanceUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String PAYMENT_TYPE_KEY = "paymentType";
    public static final String PAYMENT_STATUS_KEY = "paymentStatus";
    public static final String USER_LAST_BILLING_TIME = "last_billing_time";
    
    private static final String STOPPED_RESOURCES_COLUMN_NAME = "stopped_resources";
    private static final String PROPERTIES_COLUMN_NAME = "properties";
    private static final String INVOICES_COLUMN_NAME = "invoices";
    private static final String ACTIVE_SUBSCRIPTION_COLUMN_NAME = "active_subscription";
    private static final String INACTIVE_SUBSCRIPTIONS_COLUMN_NAME = "inactive_subscriptions";
    
    @Transient
	private static final Logger LOGGER = Logger.getLogger(FinanceUser.class);

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

    @Column(name = ACTIVE_SUBSCRIPTION_COLUMN_NAME)
    @OneToOne(cascade={CascadeType.ALL})
    private Subscription activeSubscription;
    
    @Column(name = INACTIVE_SUBSCRIPTIONS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
    private List<Subscription> inactiveSubscriptions;
    
    public FinanceUser() {

	}
	
	public FinanceUser(Map<String, String> properties) {
		this.stoppedResources = false;
		this.properties = properties;
        long billingTime = System.currentTimeMillis();
        this.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
        this.activeSubscription = null;
        this.inactiveSubscriptions = new ArrayList<Subscription>();
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
	
	public String getFinancePluginName() {
		return this.activeSubscription.getPlanName();
	}

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void addInvoice(Invoice invoice) {
        this.invoices.add(invoice);
    }
    
    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public UserCredits getCredits() {
        return credits;
    }

    public void setCredits(UserCredits credits) {
        this.credits = credits;
    }
    
    // TODO test
    public void subscribeToPlan(String planName) throws InvalidParameterException {
        if (this.activeSubscription == null) {
            Subscription newSubscription = new Subscription(UUID.randomUUID().toString(), 
                    new TimeUtils().getCurrentTimeMillis(), planName);
            this.activeSubscription = newSubscription;    
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_ALREADY_SUBSCRIBED_TO_A_PLAN, 
                            this.userId, this.activeSubscription.getPlanName()));
        }
    }
    
    // TODO test
    public void unsubscribe() throws InvalidParameterException {
        if (this.activeSubscription != null) {
            this.activeSubscription.setEndTime(new TimeUtils().getCurrentTimeMillis());
            this.inactiveSubscriptions.add(activeSubscription);
            this.activeSubscription = null;
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.USER_IS_NOT_SUBSCRIBE_TO_ANY_PLAN, 
                            this.userId));
        }
    }
    
    // TODO test
    public boolean isSubscribed() {
        return this.activeSubscription != null;
    }
}
