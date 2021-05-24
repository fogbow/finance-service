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
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

@Entity
@Table(name = "pre_paid_plugin_table")
public class PrePaidPlanPlugin extends PlanPlugin {
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
    public static final String PLAN_PLUGIN_NAME = "plan_plugin_name";
    public static final String FINANCE_PLAN_RULES = "financeplan";
    public static final String CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME = "credits_deduction_wait_time";

    @Transient
    private Thread paymentThread;
    
    @Transient
    private Thread stopServiceThread;
    
    @Transient
    private CreditsManager paymentManager;
    
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
    
    @Column(name = CREDITS_DEDUCTION_WAIT_TIME_COLUMN_NAME)
    private long creditsDeductionWaitTime;

    @OneToOne(cascade={CascadeType.ALL})
    private FinancePlan plan;

    public PrePaidPlanPlugin() {
        
    }
    
    // TODO test
    public PrePaidPlanPlugin(InMemoryUsersHolder usersHolder) 
            throws ConfigurationErrorException, InvalidParameterException {
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;
        
        Map<String, String> financeOptions = loadOptionsFromConfig();
        setOptions(financeOptions);
        
        this.paymentManager = new CreditsManager(this.usersHolder, plan);
    }
    
    // TODO test
    public PrePaidPlanPlugin(InMemoryUsersHolder usersHolder, Map<String, String> financeOptions) 
            throws ConfigurationErrorException, InvalidParameterException {
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;
        
        setOptions(financeOptions);
        
        this.paymentManager = new CreditsManager(this.usersHolder, plan);
    }
    
    // TODO test
    public PrePaidPlanPlugin(InMemoryUsersHolder usersHolder, AccountingServiceClient accountingServiceClient,
            RasClient rasClient, CreditsManager paymentManager, Map<String, String> financeOptions) throws InvalidParameterException {
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.paymentManager = paymentManager;
        this.threadsAreRunning = false;
        this.usersHolder = usersHolder;
        
        setOptions(financeOptions);
    }
    
    // TODO test
    private Map<String, String> loadOptionsFromConfig() {
        Map<String, String> options = new HashMap<String, String>();

        options.put(FINANCE_PLAN_RULES_FILE_PATH, PropertiesHolder.getInstance().getProperty(FINANCE_PLAN_RULES_FILE_PATH));
        options.put(PLAN_PLUGIN_NAME, PropertiesHolder.getInstance().getProperty(PLAN_PLUGIN_NAME));
        options.put(CREDITS_DEDUCTION_WAIT_TIME, PropertiesHolder.getInstance().getProperty(CREDITS_DEDUCTION_WAIT_TIME));
        
        return options;
    }
    
    // TODO test
    @Override
    public void setOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        validateFinanceOptions(financeOptions);
        
        this.name = financeOptions.get(PLAN_PLUGIN_NAME);
        this.creditsDeductionWaitTime = Long.valueOf(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME));
        
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
        checkContainsProperty(financeOptions, PLAN_PLUGIN_NAME);
        checkContainsProperty(financeOptions, CREDITS_DEDUCTION_WAIT_TIME);
        
        checkPropertyIsParsable(financeOptions.get(CREDITS_DEDUCTION_WAIT_TIME), CREDITS_DEDUCTION_WAIT_TIME);
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
            this.paymentRunner = new PaymentRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    accountingServiceClient, paymentManager);
            this.paymentThread = new Thread(paymentRunner);
            
            this.stopServiceRunner = new StopServiceRunner(this.name, creditsDeductionWaitTime, usersHolder, 
                    paymentManager, rasClient);
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
    public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException {
        if (operation.getOperationType().equals(Operation.CREATE)) {
            return this.paymentManager.hasPaid(user.getId(), user.getIdentityProviderId());
        }
        
        return true;
    }

    @Override
    public void setName(String name) {
        // FIXME should exist?
    }

    @Override
    public boolean isRegisteredUser(SystemUser systemUser) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
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

    // TODO test
    @Override
    public Map<String, String> getOptions() {
        HashMap<String, String> options = new HashMap<String, String>();
        JsonUtils jsonUtils = new JsonUtils();
        String planRules = jsonUtils.toJson(plan.getRulesAsMap());
        
        options.put(FINANCE_PLAN_RULES, planRules);
        options.put(CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));

        return options;
    }

    @Override
    public String getUserFinanceState(SystemUser user, String property)
            throws InvalidParameterException, InternalServerErrorException {
        return this.paymentManager.getUserFinanceState(user.getId(), user.getIdentityProviderId(), property);
    }

    @Override
    public void updateUserFinanceState(SystemUser systemUser, Map<String, String> financeState)
            throws InternalServerErrorException, InvalidParameterException {
        if (!financeState.containsKey(CREDITS_TO_ADD)) {
            throw new InvalidParameterException(String.format(Messages.Exception.MISSING_FINANCE_STATE_PROPERTY, CREDITS_TO_ADD));
        }
        
        Double valueToAdd = 0.0;
        String propertyValue = "";
        
        try {
            propertyValue = financeState.get(CREDITS_TO_ADD);
            valueToAdd = Double.valueOf(propertyValue);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_FINANCE_STATE_PROPERTY, propertyValue, CREDITS_TO_ADD));
        }
        
        FinanceUser user = this.usersHolder.getUserById(systemUser.getId(), systemUser.getIdentityProviderId());
        
        synchronized(user) {
            UserCredits userCredits = user.getCredits();
            userCredits.addCredits(valueToAdd);
            this.usersHolder.saveUser(user);
        }
    }
    
    // TODO test
    @Override
    public void setUp(Object... params) throws ConfigurationErrorException {
        InMemoryUsersHolder usersHolder = (InMemoryUsersHolder) params[0];
        
        this.usersHolder = usersHolder;
        this.accountingServiceClient = new AccountingServiceClient();
        this.rasClient = new RasClient();
        this.threadsAreRunning = false;
        
        this.paymentManager = new CreditsManager(this.usersHolder, plan);
    }
}
