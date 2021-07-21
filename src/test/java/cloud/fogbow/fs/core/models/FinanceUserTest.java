package cloud.fogbow.fs.core.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.SubscriptionFactory;
import cloud.fogbow.fs.core.util.TimeUtils;

public class FinanceUserTest {

    private static final String INVOICE_1_JSON_REPR = "invoice1json";
    private static final String INVOICE_2_JSON_REPR = "invoice2json";
    private static final String INVOICE_3_JSON_REPR = "invoice3json";
    private static final String INVOICE_4_JSON_REPR = "invoice4json";
    private static final String USER_ID_1 = "userId1";
    private static final String USER_PROVIDER_1 = "provider1";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
    private static final String INVOICE_ID_3 = "invoiceId3";
    private static final String INVOICE_ID_4 = "invoiceId4";
    private static final String NEW_PLAN_NAME = "newPlan";
    private static final Long UNSUBSCRIPTION_TIME = 100L;
    private static final Long USER_LAST_BILLING_TIME = 1L;
    private FinanceUser financeUser;
    private UserId userId;
    private Map<String, String> properties;
    private List<Invoice> invoices;
    private UserCredits credits;
    private Subscription activeSubscription;
    private List<Subscription> inactiveSubscriptions;
    private List<String> lastSubscriptionsDebts;
    private Invoice invoice1;
    private Invoice invoice2;
    private Invoice invoice3;
    private Invoice invoice4;
    private HashMap<String, String> financeState;
    private SubscriptionFactory subscriptionFactory;
    private TimeUtils timeUtils;
    
    // test case: When calling the getFinanceState method using the 
    // property ALL_USER_INVOICES, it must get all the user's invoices and,
    // for each invoice, generate a representing string. Then, return a 
    // concatenation of the strings.
    @Test
    public void testGetFinanceStateAllInvoices() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        invoices.add(invoice1);
        invoices.add(invoice2);
        invoices.add(invoice3);
        invoices.add(invoice4);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        String user1State = financeUser.getFinanceState(FinanceUser.ALL_USER_INVOICES_PROPERTY_NAME);
        
        assertEquals("[" + String.join(FinanceUser.INVOICES_SEPARATOR, 
                INVOICE_1_JSON_REPR, INVOICE_2_JSON_REPR, INVOICE_3_JSON_REPR, INVOICE_4_JSON_REPR) + "]", 
                user1State);
    }
    
    // test case: When calling the getFinanceState method using the 
    // property ALL_USER_INVOICES and the user has no invoices, it must return a 
    // String containing empty brackets.
    @Test
    public void testGetFinanceStateNoInvoices() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();

        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);

        String user1State = financeUser.getFinanceState(FinanceUser.ALL_USER_INVOICES_PROPERTY_NAME);
        
        assertEquals("[]", user1State);
    }
    
    // test case: When calling the getUserFinanceState, if the given property
    // is USER_CREDITS, it must return a String representing
    // the value of the given user credits.
    @Test
    public void testGetFinanceStateUserCredits() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpFinanceUserData();
        
        credits = Mockito.mock(UserCredits.class);
        Mockito.when(credits.getCreditsValue()).thenReturn(10.51);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        String returnedPropertyUser = financeUser.getFinanceState(FinanceUser.USER_CREDITS);
        
        assertEquals("10.51", returnedPropertyUser);
    }
    
    // test case: When calling the getUserFinanceState, if the given property
    // is USER_CREDITS, it must return a String representing
    // the value of the given user credits.
    @Test
    public void testGetFinanceStateUserCreditsZeroCredits() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpFinanceUserData();
        
        credits = Mockito.mock(UserCredits.class);
        Mockito.when(credits.getCreditsValue()).thenReturn(0.0);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);

        String returnedPropertyUser = financeUser.getFinanceState(FinanceUser.USER_CREDITS);

        assertEquals("0.0", returnedPropertyUser);
    }
    
    // test case: When calling the getUserFinanceState, if the given property
    // is USER_CREDITS, it must return a String representing
    // the value of the given user credits.
    @Test
    public void testGetFinanceStateUserCreditsNegativeCredits() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpFinanceUserData();

        credits = Mockito.mock(UserCredits.class);
        Mockito.when(credits.getCreditsValue()).thenReturn(-1.113);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);

        String returnedPropertyUser = financeUser.getFinanceState(FinanceUser.USER_CREDITS);

        assertEquals("-1.113", returnedPropertyUser);
    }
    
    // test case: When calling the getFinanceState method using an
    // unknown property, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinanceStateUnknownProperty() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.getFinanceState("unknownProperty");
    }

    // test case: When calling the updateFinanceState method passing INVOICE as property type, 
    // it must get the correct invoices and change the invoices states.
    @Test
    public void testUpdateFinanceStateInvoice() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpFinanceUserData();
        setUpInvoices();
        
        ArrayList<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(invoice1);
        invoices.add(invoice2);
 
        financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, FinanceUser.INVOICE_PROPERTY_TYPE);
        financeState.put(INVOICE_ID_1, InvoiceState.PAID.getValue());
        financeState.put(INVOICE_ID_2, InvoiceState.DEFAULTING.getValue());

        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);

        
        financeUser.updateFinanceState(financeState);
        
        
        Mockito.verify(invoice1).setState(InvoiceState.PAID);
        Mockito.verify(invoice2).setState(InvoiceState.DEFAULTING);
        Mockito.verify(lastSubscriptionsDebts, Mockito.never()).remove(Mockito.anyInt());
    }
    
    // test case: When calling the updateFinanceState method passing INVOICE as property type, 
    // if any invoice state is changed to PAID, it must remove the invoice from the 
    // lastSubscriptionDebts list if necessary.
    @Test
    public void testUpdateFinanceStateInvoiceUpdatesSubscriptionsDebts() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpFinanceUserData();
        setUpInvoices();
        
        ArrayList<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(invoice1);
        invoices.add(invoice2);
        
        financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, FinanceUser.INVOICE_PROPERTY_TYPE);
        financeState.put(INVOICE_ID_1, InvoiceState.PAID.getValue());
        financeState.put(INVOICE_ID_2, InvoiceState.DEFAULTING.getValue());
        
        this.lastSubscriptionsDebts = new ArrayList<String>();
        this.lastSubscriptionsDebts.add(invoice1.getInvoiceId());
        this.lastSubscriptionsDebts.add(invoice2.getInvoiceId());
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        
        financeUser.updateFinanceState(financeState);
        
        
        Mockito.verify(invoice1).setState(InvoiceState.PAID);
        assertEquals(1, lastSubscriptionsDebts.size());
        assertEquals(invoice2.getInvoiceId(), lastSubscriptionsDebts.get(0));
    }
    
    // test case: When calling the updateFinanceState method passing CREDITS as property type, 
    // it must update the credits using the given value.
    @Test
    public void testUpdateFinanceStateCredits() 
            throws InvalidParameterException, InternalServerErrorException {
        this.credits = Mockito.mock(UserCredits.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, FinanceUser.CREDITS_PROPERTY_TYPE);
        financeState.put(FinanceUser.CREDITS_TO_ADD, "10.5");
        
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.updateFinanceState(financeState);

        
        Mockito.verify(credits).addCredits(10.5);
    }
    
    // test case: When calling the updateFinanceState method passing CREDITS as property type and 
    // the property CREDITS_TO_ADD is not present in the state passed as argument, it must throw an 
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateCreditsMissingCreditsToAddProperty() 
            throws InvalidParameterException, InternalServerErrorException {
        this.credits = Mockito.mock(UserCredits.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, FinanceUser.CREDITS_PROPERTY_TYPE);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.updateFinanceState(financeState);
    }
    
    // test case: When calling the updateFinanceState method passing CREDITS as property type and
    // the value of property CREDITS_TO_ADD is not valid, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateCreditsInvalidCreditsToAddProperty() 
            throws InvalidParameterException, InternalServerErrorException {
        this.credits = Mockito.mock(UserCredits.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, FinanceUser.CREDITS_PROPERTY_TYPE);
        financeState.put(FinanceUser.CREDITS_TO_ADD, "invalidproperty");
        
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.updateFinanceState(financeState);
    }
    
    // test case: When calling the updateFinanceState method passing a state which contains no 
    // property type field, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateCreditsMissingPropertyType() 
            throws InvalidParameterException, InternalServerErrorException {
        this.credits = Mockito.mock(UserCredits.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.CREDITS_TO_ADD, "10.5");
        
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.updateFinanceState(financeState);
    }
    
    // test case: When calling the updateFinanceState method passing a state with an invalid
    // property type field, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateCreditsInvalidPropertyType() 
            throws InvalidParameterException, InternalServerErrorException {
        this.credits = Mockito.mock(UserCredits.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(FinanceUser.PROPERTY_TYPE_KEY, "invalidpropertytype");
        financeState.put(FinanceUser.CREDITS_TO_ADD, "10.5");
        
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.updateFinanceState(financeState);
    }

    // test case: When calling the subscribeToPlan method, it must call the SubscriptionFactory
    // to create a new subscription.
    @Test
    public void testSubscribeToPlan() throws InvalidParameterException {
        setUpFinanceUserData();

        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                null, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.subscribeToPlan(NEW_PLAN_NAME);

        Mockito.verify(this.subscriptionFactory).getSubscription(NEW_PLAN_NAME);
    }
    
    // test case: When calling the subscribeToPlan method and the user is already
    // subscribed to a plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSubscribeToPlanAlreadySubscribed() throws InvalidParameterException {
        this.activeSubscription = Mockito.mock(Subscription.class);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                this.activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, 
                subscriptionFactory, USER_LAST_BILLING_TIME, timeUtils);

        financeUser.subscribeToPlan(NEW_PLAN_NAME);
    }
    
    // test case: When calling the unsubscribe method, it must set the current subscription end time
    // and move the subscription to the list of inactiveSubscriptions. 
    @Test
    public void testUnsubscribe() throws InvalidParameterException {
        setUpFinanceUserData();

        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.unsubscribe();

        Mockito.verify(this.timeUtils).getCurrentTimeMillis();
        Mockito.verify(this.activeSubscription).setEndTime(UNSUBSCRIPTION_TIME);
        Mockito.verify(this.inactiveSubscriptions).add(activeSubscription);
    }
    
    // test case: When calling the unsubscribe method and the user is not subscribed
    // to any plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUnsubscribeNotSubscribedToPlan() throws InvalidParameterException {
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                null, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.unsubscribe();
    }
    
    // test case: When calling the isSubscribed method and the user is not subscribed to
    // any plan, it must return false.
    @Test
    public void testIsSubscribedUserIsNotSubscribed() {
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                null, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertFalse(financeUser.isSubscribed());
    }
    
    // test case: When calling the isSubscribed method and the user is subscribed to a plan,
    // it must return true.
    @Test
    public void testIsSubscribedUserIsSubscribed() {
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory,
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertTrue(financeUser.isSubscribed());
    }
    
    // test case: When calling the getFinancePluginName and the user is subscribed to a plan, 
    // it must return the current plan name.
    @Test
    public void testGetFinancePluginNameUserIsSubscribed() {
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertEquals(NEW_PLAN_NAME, financeUser.getFinancePluginName());
    }
    
    // test case: When calling the getFinancePluginName and the user is not subscribed to any plan,
    // it must return null.
    @Test
    public void testGetFinancePluginNameUserIsNotSubscribed() {
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                null, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertNull(financeUser.getFinancePluginName());
    }
    
    // test case: When calling the invoicesArePaid method and the state of all the registered 
    // invoices is PAID, the method must return true. 
    @Test
    public void testInvoicesArePaidAllInvoicesArePaid() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        invoices.add(invoice2);
        invoices.add(invoice3);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory, 
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertTrue(financeUser.invoicesArePaid());
    }
    
    // test case: When calling the invoicesArePaid method and the state of at least one invoice
    // is not PAID, the method must return false. 
    @Test
    public void testInvoicesArePaidNotAllInvoicesArePaid() 
            throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        invoices.add(invoice1);
        invoices.add(invoice2);
        invoices.add(invoice3);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory,
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertFalse(financeUser.invoicesArePaid());
    }
    
    // test case: When calling the addInvoiceAsDebt method, it must add the invoice to the list
    // of subscription debts.
    @Test
    public void testInvoiceAsDebt() throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        invoices.add(invoice2);
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory,
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.addInvoiceAsDebt(invoice1);
        
        Mockito.verify(this.lastSubscriptionsDebts).add(INVOICE_ID_1);
    }
    
    // test case: When calling the removeDebt method, it must remove the given invoice id from the list
    // of subscription debts.
    @Test
    public void testRemoveDebt() throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory,
                USER_LAST_BILLING_TIME, timeUtils);
        
        financeUser.removeDebt(INVOICE_ID_1);
        
        Mockito.verify(this.lastSubscriptionsDebts).remove(INVOICE_ID_1);
    }
    
    // test case: When calling the addInvoice method, it must add the invoice correctly.
    @Test
    public void testGetAndAddInvoices() throws InvalidParameterException, InternalServerErrorException {
        setUpInvoices();
        setUpFinanceUserData();
        
        financeUser = new FinanceUser(userId, properties, invoices, credits, 
                activeSubscription, inactiveSubscriptions, lastSubscriptionsDebts, subscriptionFactory,
                USER_LAST_BILLING_TIME, timeUtils);
        
        assertTrue(financeUser.getInvoices().isEmpty());
        
        financeUser.addInvoice(invoice1);
        financeUser.addInvoice(invoice2);
        
        assertEquals(2, financeUser.getInvoices().size());
        assertTrue(financeUser.getInvoices().contains(invoice1));
        assertTrue(financeUser.getInvoices().contains(invoice2));
    }
    
    private void setUpInvoices() throws InvalidParameterException, InternalServerErrorException {
        invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        Mockito.when(invoice1.getState()).thenReturn(InvoiceState.WAITING);
        Mockito.when(invoice1.toString()).thenReturn(INVOICE_1_JSON_REPR);
        
        invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        Mockito.when(invoice2.getState()).thenReturn(InvoiceState.PAID);
        Mockito.when(invoice2.toString()).thenReturn(INVOICE_2_JSON_REPR);

        invoice3 = Mockito.mock(Invoice.class);
        Mockito.when(invoice3.getInvoiceId()).thenReturn(INVOICE_ID_3);
        Mockito.when(invoice3.getState()).thenReturn(InvoiceState.PAID);
        Mockito.when(invoice3.toString()).thenReturn(INVOICE_3_JSON_REPR);
        
        invoice4 = Mockito.mock(Invoice.class);
        Mockito.when(invoice4.getInvoiceId()).thenReturn(INVOICE_ID_4);
        Mockito.when(invoice4.getState()).thenReturn(InvoiceState.DEFAULTING);
        Mockito.when(invoice4.toString()).thenReturn(INVOICE_4_JSON_REPR);
    }

    private void setUpFinanceUserData() {
        userId = new UserId(USER_ID_1, USER_PROVIDER_1);
        properties = new HashMap<String, String>();
        invoices = new ArrayList<Invoice>();
        lastSubscriptionsDebts = Mockito.mock(ArrayList.class);
        
        this.timeUtils = Mockito.mock(TimeUtils.class);
        Mockito.when(timeUtils.getCurrentTimeMillis()).thenReturn(UNSUBSCRIPTION_TIME);
        
        this.activeSubscription = Mockito.mock(Subscription.class);
        Mockito.when(this.activeSubscription.getPlanName()).thenReturn(NEW_PLAN_NAME);
        
        this.subscriptionFactory = Mockito.mock(SubscriptionFactory.class);
        Mockito.when(this.subscriptionFactory.getSubscription(NEW_PLAN_NAME)).thenReturn(activeSubscription);
        
        this.inactiveSubscriptions = Mockito.mock(ArrayList.class);
    }
}
