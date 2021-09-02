package cloud.fogbow.fs.core.plugins.plan.prepaid;

import java.util.HashMap;
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
import cloud.fogbow.fs.core.models.FinancePolicy;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;
import cloud.fogbow.fs.core.plugins.ResourcesPolicy;
import cloud.fogbow.fs.core.util.FinancePolicyFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.client.AccountingServiceClient;
import cloud.fogbow.fs.core.util.client.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

@Entity
@Table(name = "pre_paid_plugin_table")
public class PrePaidPlanPlugin extends PersistablePlanPlugin {
    /**
     * The key to use in the configuration property which
     * indicates the delay between credits deduction attempts.
     */
    public static final String CREDITS_DEDUCTION_WAIT_TIME = "credits_deduction_wait_time";
    /**
     * The key to use in the map passed as argument to updateFinanceState 
     * to indicate the value of credits to add to the user state.
     */
    public static final String CREDITS_TO_ADD = "CREDITS_TO_ADD";
    /**
     * The key to use in the map passed as argument to setOptions and the constructors
     * to indicate the path of the file that contains the plan configuration.
     */
    public static final String FINANCE_PLAN_RULES_FILE_PATH = "finance_plan_file_path";
    /**
     * The key to use in the map passed as argument to setOptions and the constructors
     * to indicate the time to wait before stopping user resources, after the plan evaluating
     * the user financial state as not good.
     */
    public static final String TIME_TO_WAIT_BEFORE_STOPPING = "time_to_wait_before_stopping";
    /**
     * The key to use in the map passed as argument to setOptions and the constructors
     * to indicate the string that contains the plan configuration.
     */
    public static final String FINANCE_PLAN_RULES = "financeplan";
    /**
     * The key to use in the map passed as argument to setOptions and the constructors
     * to indicate the string that contains the default resource value, used when the policy
     * does not specify a value for a certain resource.
     */
    public static final String FINANCE_PLAN_DEFAULT_RESOURCE_VALUE = "finance_plan_default_resource_value";
    
    private static final String PLAN_NAME_COLUMN_NAME = "name";
    private static final String CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME = "credits_deduction_wait_time";
    private static final String TIME_TO_WAIT_BEFORE_STOPPING_COLUMN_NAME = "time_to_wait_before_stopping";

    @Transient
    private Thread paymentThread;
    
    @Transient
    private Thread stopServiceThread;
    
    @Transient
    private CreditsManager creditsManager;
    
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
    private FinancePolicyFactory planFactory;
    
    @Transient
    private JsonUtils jsonUtils;
    
    @Transient
    private DebtsPaymentChecker debtsChecker;
    
    @Column(name = CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME)
    private long creditsDeductionWaitTime;

    @Column(name = TIME_TO_WAIT_BEFORE_STOPPING_COLUMN_NAME)
    private long timeToWaitBeforeStopping;
    
    @OneToOne(cascade={CascadeType.ALL})
    private FinancePolicy policy;
    
    @Column(name = PLAN_NAME_COLUMN_NAME)
    private String name;

    public PrePaidPlanPlugin() {
        
    }

    public PrePaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder) 
            throws ConfigurationErrorException, InvalidParameterException {
        this(planName, usersHolder, new PrePaidPluginOptionsLoader().load());
    }
    
    public PrePaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder, 
            Map<String, String> financeOptions) throws InvalidParameterException, ConfigurationErrorException {
        this(planName, usersHolder, new AccountingServiceClient(), new RasClient(),
                new FinancePolicyFactory(), new JsonUtils(), new DebtsPaymentChecker(usersHolder), financeOptions);
        
        this.creditsManager = new CreditsManager(this.usersHolder, policy);
    }
    
    public PrePaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder,
            AccountingServiceClient accountingServiceClient, RasClient rasClient,
            FinancePolicyFactory financePlanFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, 
            Map<String, String> financeOptions) throws InvalidParameterException {
        this.name = planName;
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.planFactory = financePlanFactory;
        this.jsonUtils = jsonUtils;
        this.debtsChecker = debtsChecker;
        this.threadsAreRunning = false;
        
        setOptions(financeOptions);
    }

    PrePaidPlanPlugin(String name, long creditsDeductionWaitTime, long timeToWaitBeforeStopping,
            InMemoryUsersHolder usersHolder, AccountingServiceClient accountingServiceClient, RasClient rasClient, 
            CreditsManager invoiceManager, FinancePolicyFactory planFactory, JsonUtils jsonUtils, 
            DebtsPaymentChecker debtsChecker, PaymentRunner paymentRunner, StopServiceRunner stopServiceRunner, 
            FinancePolicy policy, Map<String, String> financeOptions) 
                    throws InvalidParameterException, InternalServerErrorException {
        this(name, creditsDeductionWaitTime, timeToWaitBeforeStopping, usersHolder, accountingServiceClient, 
                rasClient, invoiceManager, planFactory, jsonUtils, debtsChecker, paymentRunner, 
                stopServiceRunner, policy);
    }
    
    PrePaidPlanPlugin(String name, long creditsDeductionWaitTime,  long timeToWaitBeforeStopping,
            InMemoryUsersHolder usersHolder, AccountingServiceClient accountingServiceClient, RasClient rasClient, 
            CreditsManager invoiceManager, FinancePolicyFactory planFactory, JsonUtils jsonUtils, 
            DebtsPaymentChecker debtsChecker, PaymentRunner paymentRunner, StopServiceRunner stopServiceRunner, 
            FinancePolicy policy) throws InvalidParameterException, InternalServerErrorException {
        this.name = name;
        this.creditsDeductionWaitTime = creditsDeductionWaitTime;
        this.timeToWaitBeforeStopping = timeToWaitBeforeStopping;
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.planFactory = planFactory;
        this.creditsManager = invoiceManager;
        this.jsonUtils = jsonUtils;
        this.debtsChecker = debtsChecker;
        this.paymentRunner = paymentRunner;
        this.stopServiceRunner = stopServiceRunner;
        this.policy = policy;
        this.threadsAreRunning = false;
    }
    
    @Override
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        validateFinanceOptions(financeOptions);
        
        this.creditsDeductionWaitTime = Long.valueOf(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME));
        this.timeToWaitBeforeStopping = Long.valueOf(financeOptions.get(TIME_TO_WAIT_BEFORE_STOPPING));
        
        setUpPlanFromOptions(financeOptions, this.planFactory);
    }
    
    private void validateFinanceOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        checkContainsProperty(financeOptions, CREDITS_DEDUCTION_WAIT_TIME);
        checkContainsProperty(financeOptions, TIME_TO_WAIT_BEFORE_STOPPING);
        checkContainsProperty(financeOptions, FINANCE_PLAN_DEFAULT_RESOURCE_VALUE);
        
        checkPropertyIsParsableAsLong(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME), CREDITS_DEDUCTION_WAIT_TIME);
        checkPropertyIsParsableAsLong(financeOptions.get(TIME_TO_WAIT_BEFORE_STOPPING), TIME_TO_WAIT_BEFORE_STOPPING);
        checkPropertyIsParsableAsDouble(financeOptions.get(FINANCE_PLAN_DEFAULT_RESOURCE_VALUE), FINANCE_PLAN_DEFAULT_RESOURCE_VALUE);
    }

    private void checkContainsProperty(Map<String, String> financeOptions, String property) throws InvalidParameterException {
        if (!financeOptions.keySet().contains(property)) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_OPTION, property));
        }
    }
    
    private void checkPropertyIsParsableAsLong(String property, String propertyName) throws InvalidParameterException {
        try {
            Long.valueOf(property);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_OPTION, property, propertyName));
        }
    }
    
    private void checkPropertyIsParsableAsDouble(String property, String propertyName) throws InvalidParameterException {
        try {
            Double.valueOf(property);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_OPTION, property, propertyName));
        }
    }
    
    private void setUpPlanFromOptions(Map<String, String> financeOptions, FinancePolicyFactory planFactory) throws InvalidParameterException {
        if (financeOptions.containsKey(FINANCE_PLAN_RULES)) {
            setUpPlanFromRulesString(financeOptions.get(FINANCE_PLAN_RULES), 
                    Double.valueOf(financeOptions.get(FINANCE_PLAN_DEFAULT_RESOURCE_VALUE)), planFactory);
        } else if (financeOptions.containsKey(FINANCE_PLAN_RULES_FILE_PATH))  {
            setUpPlanFromRulesFile(financeOptions.get(FINANCE_PLAN_RULES_FILE_PATH),
                    Double.valueOf(financeOptions.get(FINANCE_PLAN_DEFAULT_RESOURCE_VALUE)), planFactory);
        } else {
            throw new InvalidParameterException(Messages.Exception.NO_FINANCE_PLAN_CREATION_METHOD_PROVIDED);
        }
    }

    private void setUpPlanFromRulesString(String rulesString, Double defaultResourceValue, FinancePolicyFactory planFactory)
            throws InvalidParameterException {
        Map<String, String> planInfo = this.jsonUtils.fromJson(rulesString, Map.class);
        
        if (this.policy == null) {
            this.policy = planFactory.createFinancePolicy(this.name, defaultResourceValue, planInfo);
        } else {
            synchronized(this.policy) {
                this.policy.update(planInfo);
            }
        }
    }
    
    private void setUpPlanFromRulesFile(String financePlanFilePath, Double defaultResourceValue, FinancePolicyFactory planFactory)
            throws InvalidParameterException {
        this.policy = planFactory.createFinancePolicy(this.name, defaultResourceValue, financePlanFilePath);
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public Map<String, String> getOptions() {
        HashMap<String, String> options = new HashMap<String, String>();
        String planRules = policy.toString();
        
        options.put(FINANCE_PLAN_RULES, planRules);
        options.put(CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        options.put(TIME_TO_WAIT_BEFORE_STOPPING, String.valueOf(timeToWaitBeforeStopping));

        return options;
    }
    
    @Override
    public void startThreads() {
        if (!this.threadsAreRunning) {
            ResourcesPolicy resourcesPolicy = new PrePaidResourcesPolicy(this.debtsChecker, 
                    creditsManager, this.rasClient, this.timeToWaitBeforeStopping);
            
            this.paymentRunner = new PaymentRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    accountingServiceClient, creditsManager);
            this.paymentThread = new Thread(paymentRunner);
            
            this.stopServiceRunner = new StopServiceRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    rasClient, resourcesPolicy);
            this.stopServiceThread = new Thread(stopServiceRunner);
            
            this.paymentThread.start();
            this.stopServiceThread.start();
            
            while (!this.paymentRunner.isActive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            while (!this.stopServiceRunner.isActive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
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
        return threadsAreRunning;
    }

    @Override
    public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException {
        if (operation.getOperationType().equals(Operation.CREATE)) {
            boolean pastDebtsArePaid = this.debtsChecker.hasPaid(user.getId(), user.getIdentityProviderId());
            boolean currentStateIsGood = this.creditsManager.hasPaid(user.getId(), user.getIdentityProviderId()); 
            
            return pastDebtsArePaid && currentStateIsGood;
        }
        
        return true;
    }

    @Override
    public boolean isRegisteredUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
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
        
        synchronized (user) {
            this.stopServiceRunner.purgeUserResources(user);
            this.usersHolder.removeUser(systemUser.getId(), systemUser.getIdentityProviderId());
        }
    }

    @Override
    public void changePlan(SystemUser user, String newPlanName) throws InternalServerErrorException, InvalidParameterException {
        this.usersHolder.changePlan(user.getId(), user.getIdentityProviderId(), newPlanName);
    }

    @Override
    public void unregisterUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized (user) {
            this.stopServiceRunner.purgeUserResources(user);
            this.usersHolder.unregisterUser(systemUser.getId(), systemUser.getIdentityProviderId());
        }
    }
    
    @Override
    public void setUp(Object... params) throws ConfigurationErrorException {
        InMemoryUsersHolder usersHolder = (InMemoryUsersHolder) params[0];
        
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;
        this.planFactory = new FinancePolicyFactory();
        this.jsonUtils = new JsonUtils();
        this.debtsChecker = new DebtsPaymentChecker(usersHolder);
        
        this.creditsManager = new CreditsManager(this.usersHolder, policy);
    }
    
    static class PrePaidPluginOptionsLoader {
        public Map<String, String> load() throws ConfigurationErrorException {
            Map<String, String> options = new HashMap<String, String>();
            
            setOptionIfNotNull(options, FINANCE_PLAN_RULES_FILE_PATH);
            setOptionIfNotNull(options, CREDITS_DEDUCTION_WAIT_TIME);
            setOptionIfNotNull(options, TIME_TO_WAIT_BEFORE_STOPPING);
            setOptionIfNotNull(options, FINANCE_PLAN_DEFAULT_RESOURCE_VALUE);

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
