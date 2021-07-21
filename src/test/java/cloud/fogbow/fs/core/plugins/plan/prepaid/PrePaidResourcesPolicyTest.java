package cloud.fogbow.fs.core.plugins.plan.prepaid;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserState;
import cloud.fogbow.fs.core.plugins.DebtsPaymentChecker;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.client.RasClient;

public class PrePaidResourcesPolicyTest {
    private static final String USER_ID = "userId";
    private static final String PROVIDER = "provider";
    private static final long WAITING_PERIOD_REFERENCE = 100L;
    private static final long TIME_TO_WAIT_BEFORE_STOPPING = 120L;
    private DebtsPaymentChecker debtsChecker;
    private CreditsManager creditsManager;
    private TimeUtils timeUtils;
    private RasClient rasClient;
    private FinanceUser user;
    private PrePaidResourcesPolicy policy;

    @Before
    public void setUp() {
        user = Mockito.mock(FinanceUser.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        creditsManager = Mockito.mock(CreditsManager.class);
        timeUtils = Mockito.mock(TimeUtils.class);
        rasClient = Mockito.mock(RasClient.class);
        
        policy = new PrePaidResourcesPolicy(debtsChecker, creditsManager,
                rasClient, TIME_TO_WAIT_BEFORE_STOPPING, timeUtils);
    }
    
    private void setUpUser(UserState state) {
        Mockito.when(user.getId()).thenReturn(USER_ID);
        Mockito.when(user.getProvider()).thenReturn(PROVIDER);
        Mockito.when(user.getState()).thenReturn(state);
        
        Mockito.when(user.getWaitPeriodBeforeStoppingResourcesReference()).thenReturn(WAITING_PERIOD_REFERENCE);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateUserHasNotPaidPastDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);

        Mockito.verify(user).setWaitPeriodBeforeStoppingResourcesReference(WAITING_PERIOD_REFERENCE);
        Mockito.verify(user).setState(UserState.WAITING_FOR_STOP);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateUserHasNotPaidCurrentDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);

        Mockito.verify(user).setWaitPeriodBeforeStoppingResourcesReference(WAITING_PERIOD_REFERENCE);
        Mockito.verify(user).setState(UserState.WAITING_FOR_STOP);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateUserHasPaidDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setWaitPeriodBeforeStoppingResourcesReference(Mockito.anyLong());
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateWaitingForStopAndUserHasPaid() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING - 1);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.DEFAULT);
    }

    // TODO documentation
    @Test
    public void testUpdateUserStateWaitingForStopAndWaitPeriodHasNotPassed() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);
        
        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING - 1);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }

    // TODO documentation
    @Test
    public void testUpdateUserStateWaitingForStopAndWaitPeriodHasPassed() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPING);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppingUserResources() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPED);
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppingUserResourcesHibernateNotAvailable() 
            throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(NotImplementedOperationException.class).when(
                rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPED);
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppingUserResourcesHibernateFails() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(FogbowException.class).when(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient, Mockito.never()).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppingUserResourcesStopFails() throws FogbowException {
        setUpUser(UserState.STOPPING);
        
        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(NotImplementedOperationException.class).when(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.doThrow(FogbowException.class).when(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppingUserResourcesUserHasPaid() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.DEFAULT);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppedAndUserHasNotPaid() throws FogbowException {
        setUpUser(UserState.STOPPED);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);

        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateStoppedAndUserHasPaid() throws FogbowException {
        setUpUser(UserState.STOPPED);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);

        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.RESUMING);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateResumingResources() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user).setState(UserState.DEFAULT);
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateResumingResourcesFails() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(FogbowException.class).when(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // TODO documentation
    @Test
    public void testUpdateUserStateResumingResourcesUserHasNotPaid() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient, Mockito.never()).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user).setState(UserState.STOPPED);
    }
    
    // TODO documentation
    @Test(expected = InternalServerErrorException.class)
    public void testUpdateUserStateUserHasInvalidState()
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(null);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(creditsManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
    }
}
