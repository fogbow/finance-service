package cloud.fogbow.fs.core.plugins.plan.prepaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserState;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.plugins.ResourcesPolicy;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.client.RasClient;

public class PrePaidResourcesPolicy implements ResourcesPolicy {
    private static Logger LOGGER = Logger.getLogger(PrePaidResourcesPolicy.class);
    
    private DebtsPaymentChecker debtsChecker;
    private CreditsManager creditsManager;
    private long timeToWaitBeforeStopping;
    private TimeUtils timeUtils;
    private RasClient rasClient;
    
    public PrePaidResourcesPolicy(DebtsPaymentChecker debtsChecker, 
            CreditsManager creditsManager, RasClient rasClient, long timeToWaitBeforeStopping) {
        this(debtsChecker, creditsManager, rasClient, timeToWaitBeforeStopping, new TimeUtils());
    }
    
    public PrePaidResourcesPolicy(DebtsPaymentChecker debtsChecker, 
            CreditsManager creditsManager, RasClient rasClient, long timeToWaitBeforeStopping, 
            TimeUtils timeUtils) {
        this.debtsChecker = debtsChecker;
        this.creditsManager = creditsManager;
        this.timeToWaitBeforeStopping = timeToWaitBeforeStopping;
        this.timeUtils = timeUtils;
        this.rasClient = rasClient;
    }
    
    @Override
    public void updateUserState(FinanceUser user) throws InternalServerErrorException, InvalidParameterException {
        UserState state = user.getState();
        boolean pastDebtsHaveBeenPaid = this.debtsChecker.hasPaid(user.getId(), user.getProvider());
        boolean currentStateIsGood = this.creditsManager.hasPaid(user.getId(), user.getProvider());
        boolean paid = pastDebtsHaveBeenPaid && currentStateIsGood;
        
        if (state == null) {
            throw new InternalServerErrorException(String.format(Messages.Exception.UNKNOWN_USER_STATE, state));
        }
        
        switch(state) {
            case DEFAULT: processDefaultState(user, paid); break;
            case WAITING_FOR_STOP: processWaitingForStopState(user, paid); break;
            case STOPPING: processStoppingState(user, paid); break;
            case STOPPED: processStoppedState(user, paid); break;
            case RESUMING: processResumingState(user, paid); break;
        }
    }

    private void processDefaultState(FinanceUser user, boolean paid) {
        if (!paid) {
            user.setWaitPeriodBeforeStoppingResourcesReference(this.timeUtils.getCurrentTimeMillis());
            user.setState(UserState.WAITING_FOR_STOP);
        }
    }

    private void processWaitingForStopState(FinanceUser user, boolean paid) {
        if (paid) {
            user.setState(UserState.DEFAULT);
        } else {
            long currentTime = this.timeUtils.getCurrentTimeMillis();
            long referenceTime = user.getWaitPeriodBeforeStoppingResourcesReference();
            
            if (currentTime - referenceTime >= this.timeToWaitBeforeStopping) {
                user.setState(UserState.STOPPING);
            }
        }
    }
    
    private void processStoppingState(FinanceUser user, boolean paid) {
        if (paid) {
            user.setState(UserState.DEFAULT);
        } else {
            try {
                tryToHibernateThenTryToStop(user);
                user.setState(UserState.STOPPED);
            } catch (FogbowException e) {
                LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(), 
                        e.getMessage()));
            }
        }
    }
    
    private void processStoppedState(FinanceUser user, boolean paid) {
        if (paid) {
            user.setState(UserState.RESUMING);
        }
    }
    
    private void processResumingState(FinanceUser user, boolean paid) {
        if (!paid) {
            user.setState(UserState.STOPPED);
        } else {
            try {
                this.rasClient.resumeResourcesByUser(user.getId(), user.getProvider());
                user.setState(UserState.DEFAULT);
            } catch (FogbowException e) {
                LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(), 
                        e.getMessage()));
            }
        }
    }
    
    private void tryToHibernateThenTryToStop(FinanceUser user) throws FogbowException {
        try {
            this.rasClient.hibernateResourcesByUser(user.getId(), user.getProvider());
        } catch (NotImplementedOperationException e) {
            this.rasClient.stopResourcesByUser(user.getId(), user.getProvider());
        }
    }
}
