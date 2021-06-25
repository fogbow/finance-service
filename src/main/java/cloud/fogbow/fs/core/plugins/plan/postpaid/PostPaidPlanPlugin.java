package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.util.FinancePlanFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

@Entity
@Table(name = "post_paid_plugin_table")
public class PostPaidPlanPlugin extends PersistablePlanPlugin {
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
    public static final String PLAN_NAME_COLUMN_NAME = "name";
    public static final String FINANCE_PLAN_RULES_FILE_PATH = "finance_plan_file_path";

    @Transient
    private Thread paymentThread;
    
    @Transient
    private Thread stopServiceThread;
    
    @Transient
    private InvoiceManager invoiceManager;
    
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
    
    @Transient
    private JsonUtils jsonUtils;
    
    @Transient
    private FinancePlanFactory planFactory;
    
    @Transient
    private DebtsPaymentChecker debtsChecker;
    
    @Column(name = USER_BILLING_TIME_COLUMN_NAME)
    private long userBillingTime;
    
    @Column(name = INVOICE_WAIT_TIME_COLUMN_NAME)
    private long invoiceWaitTime;
    
    @OneToOne(cascade={CascadeType.ALL})
    private FinancePlan plan;

    @Column(name = PLAN_NAME_COLUMN_NAME)
    private String name;
    
    public PostPaidPlanPlugin() {
        
    }
    
    public PostPaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder) throws ConfigurationErrorException, 
                    InvalidParameterException, InternalServerErrorException { 
        this(planName, usersHolder, new PostPaidPluginOptionsLoader().load());
    }

    public PostPaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder, Map<String, String> financeOptions)
            throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
        this(planName, usersHolder, new AccountingServiceClient(), new RasClient(),
                new FinancePlanFactory(), new JsonUtils(), new DebtsPaymentChecker(usersHolder), financeOptions);
        
        this.invoiceManager = new InvoiceManager(this.usersHolder, plan);
    }
    
    PostPaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder, AccountingServiceClient accountingServiceClient,
            RasClient rasClient, FinancePlanFactory planFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, 
            Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException {
        this.name = planName;
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.planFactory = planFactory;
        this.jsonUtils = jsonUtils;
        this.debtsChecker = debtsChecker;
        this.threadsAreRunning = false;

        setOptions(financeOptions);
    }
    
    PostPaidPlanPlugin(String name, long userBillingInterval, long invoiceWaitTime, InMemoryUsersHolder usersHolder, 
            AccountingServiceClient accountingServiceClient, RasClient rasClient, InvoiceManager invoiceManager, 
            FinancePlanFactory planFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, PaymentRunner paymentRunner,
            StopServiceRunner stopServiceRunner, FinancePlan financePlan, Map<String, String> financeOptions) 
                    throws InvalidParameterException, InternalServerErrorException {
        this(name, userBillingInterval, invoiceWaitTime, usersHolder, accountingServiceClient, rasClient, invoiceManager, 
                planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, financePlan);
    }
    
    PostPaidPlanPlugin(String name, long userBillingInterval, long invoiceWaitTime, InMemoryUsersHolder usersHolder, 
            AccountingServiceClient accountingServiceClient, RasClient rasClient, InvoiceManager invoiceManager, 
            FinancePlanFactory planFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, PaymentRunner paymentRunner, 
            StopServiceRunner stopServiceRunner, FinancePlan financePlan) 
                    throws InvalidParameterException, InternalServerErrorException {
        this.name = name;
        this.userBillingTime = userBillingInterval;
        this.invoiceWaitTime = invoiceWaitTime;
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.planFactory = planFactory;
        this.invoiceManager = invoiceManager;
        this.jsonUtils = jsonUtils;
        this.plan = financePlan;
        this.debtsChecker = debtsChecker;
        this.paymentRunner = paymentRunner;
        this.stopServiceRunner = stopServiceRunner;
        this.threadsAreRunning = false;
    }
    
    @Override
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        validateFinanceOptions(financeOptions);
        
        this.userBillingTime = Long.valueOf(financeOptions.get(PaymentRunner.USER_BILLING_INTERVAL));
        this.invoiceWaitTime = Long.valueOf(financeOptions.get(INVOICE_WAIT_TIME));
        
        setUpPlanFromOptions(financeOptions, this.planFactory);
    }
    
    private void validateFinanceOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        checkContainsProperty(financeOptions, PaymentRunner.USER_BILLING_INTERVAL);
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
    
    private void setUpPlanFromOptions(Map<String, String> financeOptions, FinancePlanFactory planFactory) throws InvalidParameterException {
        if (financeOptions.containsKey(FINANCE_PLAN_RULES)) {
            setUpPlanFromRulesString(financeOptions.get(FINANCE_PLAN_RULES), planFactory);
        } else if (financeOptions.containsKey(FINANCE_PLAN_RULES_FILE_PATH))  {
            setUpPlanFromRulesFile(financeOptions.get(FINANCE_PLAN_RULES_FILE_PATH), planFactory);
        } else {
            throw new InvalidParameterException(Messages.Exception.NO_FINANCE_PLAN_CREATION_METHOD_PROVIDED);
        }
    }

    private void setUpPlanFromRulesString(String rulesString, FinancePlanFactory planFactory)
            throws InvalidParameterException {
        Map<String, String> planInfo = this.jsonUtils.fromJson(rulesString, Map.class);
        
        if (this.plan == null) {
            this.plan = planFactory.createFinancePlan(this.name, planInfo);
        } else {
            synchronized(this.plan) {
                this.plan.update(planInfo);
            }
        }
    }
    
    private void setUpPlanFromRulesFile(String financePlanFilePath, FinancePlanFactory planFactory)
            throws InvalidParameterException {
        this.plan = planFactory.createFinancePlan(this.name, financePlanFilePath);
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public Map<String, String> getOptions() {
        String planRules = plan.toString();
        
        HashMap<String, String> options = new HashMap<String, String>();
        options.put(FINANCE_PLAN_RULES, planRules);
        options.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingTime));
        options.put(INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));

        return options;
    }
    
    @Override
    public void startThreads() {
        if (!this.threadsAreRunning) {
            this.paymentRunner = new PaymentRunner(this.name, invoiceWaitTime, userBillingTime, usersHolder, 
                    accountingServiceClient, invoiceManager);
            this.paymentThread = new Thread(paymentRunner);
            
            this.stopServiceRunner = new StopServiceRunner(this.name, invoiceWaitTime, usersHolder, 
                    invoiceManager, rasClient, this.debtsChecker);
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
    public boolean isStarted() {
        return this.threadsAreRunning;
    }

    @Override
    public boolean isRegisteredUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());

        synchronized (user) {
            if (user.isSubscribed()) {
                return user.getFinancePluginName().equals(this.name);
            } else {
                return false;
            }
        }
    }

    @Override
    public void registerUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        this.usersHolder.registerUser(systemUser.getId(), systemUser.getIdentityProviderId(), this.name);
        
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized (user) {
            this.stopServiceRunner.resumeResourcesForUser(user);
        }
    }

    @Override
    public void purgeUser(SystemUser systemUser) throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
            this.stopServiceRunner.purgeUserResources(user);
            this.usersHolder.removeUser(systemUser.getId(), systemUser.getIdentityProviderId());
        }
    }

    @Override
    public void changePlan(SystemUser systemUser, String newPlanName) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized (user) {
            if (user.invoicesArePaid()) {
                this.paymentRunner.runLastPaymentForUser(systemUser.getId(), systemUser.getIdentityProviderId());
                this.usersHolder.changePlan(systemUser.getId(), systemUser.getIdentityProviderId(), newPlanName);
            } else {
                throw new InvalidParameterException(
                        String.format(Messages.Exception.USER_HAS_NOT_PAID_ALL_INVOICES, user.getId(), user.getProvider()));
            }
        }
    }

    @Override
    public void unregisterUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized (user) {
            if (user.invoicesArePaid()) {
                this.stopServiceRunner.purgeUserResources(user);
                this.paymentRunner.runLastPaymentForUser(systemUser.getId(), systemUser.getIdentityProviderId());
                this.usersHolder.unregisterUser(systemUser.getId(), systemUser.getIdentityProviderId());
            } else {
                throw new InvalidParameterException(
                        String.format(Messages.Exception.USER_HAS_NOT_PAID_ALL_INVOICES, user.getId(), user.getProvider()));
            }
        }
    }
    
    @Override
    public boolean isAuthorized(SystemUser user, RasOperation operation) 
            throws InvalidParameterException, InternalServerErrorException {
        if (operation.getOperationType().equals(Operation.CREATE)) {
            boolean currentInvoicesHaveBeenPaid = 
                    this.invoiceManager.hasPaid(user.getId(), user.getIdentityProviderId()); 
            boolean pastInvoicesHaveBeenPaid = 
                    this.debtsChecker.hasPaid(user.getId(), user.getIdentityProviderId()); 
            
            return currentInvoicesHaveBeenPaid && pastInvoicesHaveBeenPaid;
        }
        
        return true;
    }

    @Override
    public void setUp(Object... params) throws ConfigurationErrorException {
        InMemoryUsersHolder objectsHolder = (InMemoryUsersHolder) params[0];
        
        this.usersHolder = objectsHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.planFactory = new FinancePlanFactory();
        this.jsonUtils = new JsonUtils();
        this.debtsChecker = new DebtsPaymentChecker(usersHolder);
        this.threadsAreRunning = false;
        
        this.invoiceManager = new InvoiceManager(this.usersHolder, plan);
    }

    @VisibleForTesting
    static class PostPaidPluginOptionsLoader {
        public Map<String, String> load() throws ConfigurationErrorException {
            Map<String, String> options = new HashMap<String, String>();
            
            setOptionIfNotNull(options, PaymentRunner.USER_BILLING_INTERVAL);
            setOptionIfNotNull(options, FINANCE_PLAN_RULES_FILE_PATH);
            setOptionIfNotNull(options, INVOICE_WAIT_TIME);

            return options;
        }
        
        private void setOptionIfNotNull(Map<String, String> options, String optionName) 
                throws ConfigurationErrorException {
            String optionValue = PropertiesHolder.getInstance().getProperty(optionName);
            
            if (optionValue == null) {
                throw new ConfigurationErrorException(
                        String.format(Messages.Exception.MISSING_FINANCE_OPTION, optionName));
            } else {
                options.put(optionName, optionValue);
            }
        }
    }
}
