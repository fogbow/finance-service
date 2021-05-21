package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

@Entity
@Table(name = "post_paid_plugin_table")
public class PostPaidPlanPlugin extends PlanPlugin {
    /**
     * The key to use in the configuration property which
     * indicates the delay between invoice generation attempts.
     * Normally the PaymentRunner checks if the user configuration
     * allows the invoice generation at each attempt.
     */
    public static final String INVOICE_WAIT_TIME = "invoice_wait_time";
    // TODO documentation
    public static final String FINANCE_PLAN_RULES = "financeplan";
    public static final String USER_BILLING_TIME_COLUMN_NAME = "user_billing_time";
    public static final String INVOICE_WAIT_TIME_COLUMN_NAME = "invoice_wait_time";
    public static final String FINANCE_PLAN_RULES_FILE_PATH = "finance_plan_file_path";
    public static final String PLAN_PLUGIN_NAME = "plan_plugin_name";

    @Transient
    private Thread paymentThread;
    
    @Transient
    private Thread stopServiceThread;
    
    @Transient
    private InvoiceManager paymentManager;
    
    @Transient
    private AccountingServiceClient accountingServiceClient;
    
    @Transient
    private RasClient rasClient;
    
    @Transient
    private PaymentRunner paymentRunner;
    
    @Transient
    private StopServiceRunner stopServiceRunner;
    
    @Transient
    private boolean threadsAreRunning;
    
    @Transient
    private InMemoryUsersHolder usersHolder;

    @Column(name = USER_BILLING_TIME_COLUMN_NAME)
    private long userBillingTime;
    
    @Column(name = INVOICE_WAIT_TIME_COLUMN_NAME)
    private long invoiceWaitTime;
    
    @OneToOne(cascade={CascadeType.ALL})
    private FinancePlan plan;

    public PostPaidPlanPlugin() {
        
    }
    
    public PostPaidPlanPlugin(InMemoryUsersHolder usersHolder) throws ConfigurationErrorException, 
                    InvalidParameterException, InternalServerErrorException {
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;

        Map<String, String> financeOptions = loadOptionsFromConfig();
        setOptions(financeOptions);
        
        this.paymentManager = new InvoiceManager(this.usersHolder, plan);
    }
    
    public PostPaidPlanPlugin(InMemoryUsersHolder usersHolder, Map<String, String> financeOptions)
            throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;

        setOptions(financeOptions);
        
        this.paymentManager = new InvoiceManager(this.usersHolder, plan);
    }
    
    public PostPaidPlanPlugin(String name, InMemoryUsersHolder usersHolder, AccountingServiceClient accountingServiceClient,
            RasClient rasClient, InvoiceManager paymentManager, Map<String, String> financeOptions) 
                    throws InvalidParameterException, InternalServerErrorException {
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.paymentManager = paymentManager;
        this.threadsAreRunning = false;
        this.name = name;
        
        setOptions(financeOptions);
        
        this.paymentManager = paymentManager;
    }
    
    private Map<String, String> loadOptionsFromConfig() {
        Map<String, String> options = new HashMap<String, String>();

        options.put(PaymentRunner.USER_BILLING_INTERVAL, PropertiesHolder.getInstance().getProperty(PaymentRunner.USER_BILLING_INTERVAL));
        options.put(FINANCE_PLAN_RULES_FILE_PATH, PropertiesHolder.getInstance().getProperty(FINANCE_PLAN_RULES_FILE_PATH));
        options.put(PLAN_PLUGIN_NAME, PropertiesHolder.getInstance().getProperty(PLAN_PLUGIN_NAME));
        options.put(INVOICE_WAIT_TIME, PropertiesHolder.getInstance().getProperty(INVOICE_WAIT_TIME));
        
        return options;
    }

    @Override
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        validateFinanceOptions(financeOptions);
        
        this.name = financeOptions.get(PLAN_PLUGIN_NAME);
        this.userBillingTime = Long.valueOf(financeOptions.get(PaymentRunner.USER_BILLING_INTERVAL));
        this.invoiceWaitTime = Long.valueOf(financeOptions.get(INVOICE_WAIT_TIME));
        
        JsonUtils jsonUtils = new JsonUtils();
        
        // TODO refactor
        if (financeOptions.containsKey(FINANCE_PLAN_RULES)) {
            Map<String, String> planInfo = jsonUtils.fromJson(financeOptions.get(FINANCE_PLAN_RULES), Map.class);
            
            if (this.plan == null) {
                this.plan = new FinancePlan(this.name, planInfo);
            } else {
                synchronized(this.plan) {
                    this.plan.update(planInfo);
                }
            }
        } else if (financeOptions.containsKey(FINANCE_PLAN_RULES_FILE_PATH))  {
            String financePlanFilePath = financeOptions.get(FINANCE_PLAN_RULES_FILE_PATH);
            this.plan = new FinancePlan(this.name, financePlanFilePath);
        }
    }
    
    private void validateFinanceOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        checkContainsProperty(financeOptions, PaymentRunner.USER_BILLING_INTERVAL);
        checkContainsProperty(financeOptions, PLAN_PLUGIN_NAME);
        checkContainsProperty(financeOptions, INVOICE_WAIT_TIME);
        
        checkPropertyIsParsable(financeOptions.get(PaymentRunner.USER_BILLING_INTERVAL), PaymentRunner.USER_BILLING_INTERVAL);
        checkPropertyIsParsable(financeOptions.get(INVOICE_WAIT_TIME), INVOICE_WAIT_TIME);
    }
    
    private void checkContainsProperty(Map<String, String> financeOptions, String property) throws InvalidParameterException {
        if (!financeOptions.keySet().contains(property)) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_OPTION, property));
        }
    }
    
    private void checkPropertyIsParsable(String property, String propertyName) throws InvalidParameterException {
        try {
            Long.valueOf(property);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_OPTION, property, propertyName));
        }
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public void startThreads() {
        if (!this.threadsAreRunning) {
            this.paymentRunner = new PaymentRunner(this.name, invoiceWaitTime, userBillingTime, usersHolder, accountingServiceClient, paymentManager);
            this.paymentThread = new Thread(paymentRunner);
            
            this.stopServiceRunner = new StopServiceRunner(this.name, invoiceWaitTime, usersHolder, paymentManager, rasClient);
            this.stopServiceThread = new Thread(stopServiceRunner);
            
            this.paymentThread.start();
            this.stopServiceThread.start();
            
            while (!this.paymentRunner.isActive());
            while (!this.stopServiceRunner.isActive());
            
            this.threadsAreRunning = true;
        }
    }

    @Override
    public void stopThreads() {
        if (this.threadsAreRunning) {
            this.paymentRunner.stop();
            this.stopServiceRunner.stop();
            
            this.threadsAreRunning = false;
        }
    }

    @Override
    public void setName(String name) {
        // FIXME should exist?
    }

    @Override
    public boolean isRegisteredUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());

        synchronized (user) {
            return user.getFinancePluginName().equals(this.name);
        }
    }

    @Override
    public void registerUser(SystemUser user) throws InternalServerErrorException, InvalidParameterException {
        this.usersHolder.registerUser(user.getId(), user.getIdentityProviderId(), this.name);
    }

    @Override
    public void unregisterUser(SystemUser user) throws InvalidParameterException, InternalServerErrorException {
        this.usersHolder.removeUser(user.getId(), user.getIdentityProviderId()); 
    }

    @Override
    public Map<String, String> getOptions() {
        HashMap<String, String> options = new HashMap<String, String>();
        JsonUtils jsonUtils = new JsonUtils();
        String planRules = jsonUtils.toJson(plan.getRulesAsMap());
        
        options.put(FINANCE_PLAN_RULES, planRules);
        options.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingTime));
        options.put(INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));

        return options;
    }

    @Override
    public String getUserFinanceState(SystemUser user, String property) throws InvalidParameterException, InternalServerErrorException {
        return this.paymentManager.getUserFinanceState(user.getId(), user.getIdentityProviderId(), property);
    }

    @Override
    public void updateUserFinanceState(SystemUser systemUser, Map<String, String> financeState) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());

        synchronized (user) {
            List<Invoice> invoices = user.getInvoices();

            for (String invoiceId : financeState.keySet()) {
                for (Invoice invoice : invoices) {
                    if (invoice.getInvoiceId().equals(invoiceId)) {
                        invoice.setState(InvoiceState.fromValue(financeState.get(invoiceId)));
                    }
                }
            }

            this.usersHolder.saveUser(user);
        }
  
    }

    @Override
    public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException {
        if (operation.getOperationType().equals(Operation.CREATE)) {
            return this.paymentManager.hasPaid(user.getId(), user.getIdentityProviderId());
        }

        return true;
    }

    @Override
    public void setUp(Object... params) throws ConfigurationErrorException {
        InMemoryUsersHolder objectsHolder = (InMemoryUsersHolder) params[0];
        
        this.usersHolder = objectsHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;
        
        this.paymentManager = new InvoiceManager(this.usersHolder, plan);
    }
}
