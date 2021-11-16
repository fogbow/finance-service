package cloud.fogbow.fs.core.plugins.plan.postpaid;

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

public class PostPaidResourcesPolicyTest {
    private static final String USER_ID = "userId";
    private static final String PROVIDER = "provider";
    private static final long WAITING_PERIOD_REFERENCE = 100L;
    private static final long TIME_TO_WAIT_BEFORE_STOPPING = 120L;
    private DebtsPaymentChecker debtsChecker;
    private InvoiceManager invoiceManager;
    private TimeUtils timeUtils;
    private RasClient rasClient;
    private FinanceUser user;
    private PostPaidResourcesPolicy policy;

    @Before
    public void setUp() {
        user = Mockito.mock(FinanceUser.class);
        debtsChecker = Mockito.mock(DebtsPaymentChecker.class);
        invoiceManager = Mockito.mock(InvoiceManager.class);
        timeUtils = Mockito.mock(TimeUtils.class);
        rasClient = Mockito.mock(RasClient.class);
        
        policy = new PostPaidResourcesPolicy(debtsChecker, invoiceManager,
                rasClient, TIME_TO_WAIT_BEFORE_STOPPING, timeUtils);
    }
    
    private void setUpUser(UserState state) {
        Mockito.when(user.getId()).thenReturn(USER_ID);
        Mockito.when(user.getProvider()).thenReturn(PROVIDER);
        Mockito.when(user.getState()).thenReturn(state);
        
        Mockito.when(user.getWaitPeriodBeforeStoppingResourcesReference()).thenReturn(WAITING_PERIOD_REFERENCE);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is DEFAULT and the user has not paid past debts, then the method must change the user state 
    // to WAITING_FOR_STOP and set the period to wait reference.
    @Test
    public void testUpdateUserStateUserHasNotPaidPastDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);

        Mockito.verify(user).setWaitPeriodBeforeStoppingResourcesReference(WAITING_PERIOD_REFERENCE);
        Mockito.verify(user).setState(UserState.WAITING_FOR_STOP);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is DEFAULT and the user has not paid current debts, then the method must change the user state to 
    // WAITING_FOR_STOP and set the period to wait reference.
    @Test
    public void testUpdateUserStateUserHasNotPaidCurrentDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);

        Mockito.verify(user).setWaitPeriodBeforeStoppingResourcesReference(WAITING_PERIOD_REFERENCE);
        Mockito.verify(user).setState(UserState.WAITING_FOR_STOP);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is DEFAULT and the user has paid debts, then the method must not change the user state.
    @Test
    public void testUpdateUserStateUserHasPaidDebts() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.DEFAULT);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(WAITING_PERIOD_REFERENCE);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setWaitPeriodBeforeStoppingResourcesReference(Mockito.anyLong());
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is WAITING_FOR_STOP and the user has paid debts, then the method must change the user state to DEFAULT.
    @Test
    public void testUpdateUserStateWaitingForStopAndUserHasPaid() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING - 1);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.DEFAULT);
    }

    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is WAITING_FOR_STOP, the user has not paid debts and the wait period has not passed, then the method 
    // must not change the user state.
    @Test
    public void testUpdateUserStateWaitingForStopAndWaitPeriodHasNotPassed() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);
        
        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING - 1);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }

    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is WAITING_FOR_STOP, the user has not paid debts and the wait period has passed, then the method 
    // must change the user state to STOPPING.
    @Test
    public void testUpdateUserStateWaitingForStopAndWaitPeriodHasPassed() 
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(UserState.WAITING_FOR_STOP);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(
                WAITING_PERIOD_REFERENCE + TIME_TO_WAIT_BEFORE_STOPPING);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPING);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPING and the user has not paid debts, then the method must call the RasClient to hibernate
    // the user resources. If the operation is successful, then the method must change the user state to STOPPED. 
    @Test
    public void testUpdateUserStateStoppingUserResources() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPED);
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPING and the user has not paid debts, then the method must call the RasClient to hibernate
    // the user resources. If the hibernate operation is not available, then the method must stop the user
    // resources. If the stop operation is successful, then the method must change the user state to STOPPED.
    @Test
    public void testUpdateUserStateStoppingUserResourcesHibernateNotAvailable() 
            throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(NotImplementedOperationException.class).when(
                rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.STOPPED);
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPING and the user has not paid debts, then the method must call the RasClient to hibernate
    // the user resources. If the hibernate operation fails, then the method must not change the user state.
    @Test
    public void testUpdateUserStateStoppingUserResourcesHibernateFails() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(FogbowException.class).when(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient, Mockito.never()).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPING and the user has not paid debts, then the method must call the RasClient to hibernate
    // the user resources. If the hibernate operation is not available, then the method must stop the user
    // resources. If the stop operation is not successful, then the method must not change the user state.
    @Test
    public void testUpdateUserStateStoppingUserResourcesStopFails() throws FogbowException {
        setUpUser(UserState.STOPPING);
        
        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(NotImplementedOperationException.class).when(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.doThrow(FogbowException.class).when(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
        Mockito.verify(rasClient).hibernateResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(rasClient).stopResourcesByUser(USER_ID, PROVIDER);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPING and the user has paid debts, then the method must change the user state to DEFAULT. 
    @Test
    public void testUpdateUserStateStoppingUserResourcesUserHasPaid() throws FogbowException {
        setUpUser(UserState.STOPPING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.DEFAULT);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPED and the user has not paid debts, then the method must not change the user state.
    @Test
    public void testUpdateUserStateStoppedAndUserHasNotPaid() throws FogbowException {
        setUpUser(UserState.STOPPED);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);

        policy.updateUserState(user);
        
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is STOPPED and the user has paid debts, then the method must change the user state to RESUMING.
    @Test
    public void testUpdateUserStateStoppedAndUserHasPaid() throws FogbowException {
        setUpUser(UserState.STOPPED);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);

        policy.updateUserState(user);
        
        Mockito.verify(user).setState(UserState.RESUMING);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is RESUMING and the user has paid debts, then the method must call the RasClient to resume the user
    // resources. If the operation is successful, then the method must change the user state to DEFAULT.
    @Test
    public void testUpdateUserStateResumingResources() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user).setState(UserState.DEFAULT);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is RESUMING and the user has paid debts, then the method must call the RasClient to resume the user
    // resources. If the operation is not successful, then the method must not change the user state.
    @Test
    public void testUpdateUserStateResumingResourcesFails() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        Mockito.doThrow(FogbowException.class).when(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user, Mockito.never()).setState(Mockito.any(UserState.class));
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is RESUMING and the user has not paid debts, then the method must change the user state to STOPPED.
    @Test
    public void testUpdateUserStateResumingResourcesUserHasNotPaid() throws FogbowException {
        setUpUser(UserState.RESUMING);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
        
        Mockito.verify(rasClient, Mockito.never()).resumeResourcesByUser(USER_ID, PROVIDER);
        Mockito.verify(user).setState(UserState.STOPPED);
    }
    
    // test case: When calling the method updateUserState, if the state of the user passed as argument
    // is null, then the method must throw an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testUpdateUserStateUserHasInvalidState()
            throws InternalServerErrorException, InvalidParameterException {
        setUpUser(null);

        Mockito.when(debtsChecker.hasPaid(USER_ID, PROVIDER)).thenReturn(false);
        Mockito.when(invoiceManager.hasPaid(USER_ID, PROVIDER)).thenReturn(true);
        
        policy.updateUserState(user);
    }
}
