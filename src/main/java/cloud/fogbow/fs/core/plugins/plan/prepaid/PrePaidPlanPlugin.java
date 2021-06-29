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
    // TODO documentation
    public static final String FINANCE_PLAN_RULES_FILE_PATH = "finance_plan_file_path";
    public static final String FINANCE_PLAN_RULES = "financeplan";
    public static final String PLAN_NAME_COLUMN_NAME = "name";
    public static final String CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME = "credits_deduction_wait_time";

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
    private FinancePlanFactory planFactory;
    
    @Transient
    private JsonUtils jsonUtils;
    
    @Transient
    private DebtsPaymentChecker debtsChecker;
    
    @Column(name = CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME)
    private long creditsDeductionWaitTime;

    @OneToOne(cascade={CascadeType.ALL})
    private FinancePlan plan;
    
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
                new FinancePlanFactory(), new JsonUtils(), new DebtsPaymentChecker(usersHolder), financeOptions);
        
        this.creditsManager = new CreditsManager(this.usersHolder, plan);
    }
    
    public PrePaidPlanPlugin(String planName, InMemoryUsersHolder usersHolder,
            AccountingServiceClient accountingServiceClient, RasClient rasClient,
            FinancePlanFactory financePlanFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, 
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

    PrePaidPlanPlugin(String name, long creditsDeductionWaitTime, InMemoryUsersHolder usersHolder, 
            AccountingServiceClient accountingServiceClient, RasClient rasClient, CreditsManager invoiceManager, 
            FinancePlanFactory planFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, 
            PaymentRunner paymentRunner, StopServiceRunner stopServiceRunner, FinancePlan financePlan, 
            Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException {
        this(name, creditsDeductionWaitTime, usersHolder, accountingServiceClient, rasClient, invoiceManager, 
                planFactory, jsonUtils, debtsChecker, paymentRunner, stopServiceRunner, financePlan);
    }
    
    PrePaidPlanPlugin(String name, long creditsDeductionWaitTime, InMemoryUsersHolder usersHolder, 
            AccountingServiceClient accountingServiceClient, RasClient rasClient, CreditsManager invoiceManager, 
            FinancePlanFactory planFactory, JsonUtils jsonUtils, DebtsPaymentChecker debtsChecker, 
            PaymentRunner paymentRunner, StopServiceRunner stopServiceRunner, FinancePlan financePlan) 
                    throws InvalidParameterException, InternalServerErrorException {
        this.name = name;
        this.creditsDeductionWaitTime = creditsDeductionWaitTime;
        this.usersHolder = usersHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.planFactory = planFactory;
        this.creditsManager = invoiceManager;
        this.jsonUtils = jsonUtils;
        this.debtsChecker = debtsChecker;
        this.paymentRunner = paymentRunner;
        this.stopServiceRunner = stopServiceRunner;
        this.plan = financePlan;
        this.threadsAreRunning = false;
    }
    
    @Override
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        validateFinanceOptions(financeOptions);
        
        this.creditsDeductionWaitTime = Long.valueOf(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME));
        
        setUpPlanFromOptions(financeOptions, this.planFactory);
    }
    
    private void validateFinanceOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        checkContainsProperty(financeOptions, CREDITS_DEDUCTION_WAIT_TIME);
        
        checkPropertyIsParsable(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME), CREDITS_DEDUCTION_WAIT_TIME);
    }

    private void checkContainsProperty(Map<String, String> financeOptions, String property) throws InvalidParameterException {
        if (!financeOptions.keySet().contains(property)) {
            // TODO test
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_OPTION, property));
        }
    }
    
    private void checkPropertyIsParsable(String property, String propertyName) throws InvalidParameterException {
        try {
            Long.valueOf(property);
        } catch (NumberFormatException e) {
            // TODO test
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
        HashMap<String, String> options = new HashMap<String, String>();
        String planRules = plan.toString();
        
        options.put(FINANCE_PLAN_RULES, planRules);
        options.put(CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));

        return options;
    }
    
    @Override
    public void startThreads() {
        if (!this.threadsAreRunning) {
            this.paymentRunner = new PaymentRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    accountingServiceClient, creditsManager);
            this.paymentThread = new Thread(paymentRunner);
            
            this.stopServiceRunner = new StopServiceRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    creditsManager, rasClient, this.debtsChecker);
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
        
        this.creditsManager = new CreditsManager(this.usersHolder, plan);
    }
    
    static class PrePaidPluginOptionsLoader {
        public Map<String, String> load() throws ConfigurationErrorException {
            Map<String, String> options = new HashMap<String, String>();
            
            setOptionIfNotNull(options, FINANCE_PLAN_RULES_FILE_PATH);
            setOptionIfNotNull(options, CREDITS_DEDUCTION_WAIT_TIME);

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
