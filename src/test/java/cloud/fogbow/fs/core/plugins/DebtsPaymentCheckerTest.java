package cloud.fogbow.fs.core.plugins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;

public class DebtsPaymentCheckerTest {
    private static final String USER_ID_1 = "userId1";
    private static final String PROVIDER_1 = "provider1";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
    private static final String INVOICE_ID_3 = "invoiceId3";
    private DebtsPaymentChecker debtsChecker;
    private InMemoryUsersHolder usersHolder;
    private FinanceUser user1;
    private List<String> subscriptionsDebts;
    private List<Invoice> invoices;
    private Invoice invoice1;
    private Invoice invoice2;
    private Invoice invoice3;
    
    // test case: When calling the hasPaid method for a FinanceUser and the
    // user's list of debts contains at least one Invoice in state DEFAULTING, 
    // it must return false.
    @Test
    public void testSubscriptionDebtDefaulting() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoices(InvoiceState.PAID, InvoiceState.DEFAULTING);
        setUpUsers();
        
        debtsChecker = new DebtsPaymentChecker(usersHolder);
        
        boolean hasPaidResponse = debtsChecker.hasPaid(USER_ID_1, PROVIDER_1);
        
        assertFalse(hasPaidResponse);
    }
    
    // test case: When calling the hasPaid method for a FinanceUser and all the
    // invoices in the user's list of debts are in the state PAID, it must return true.
    @Test
    public void testSubscriptionDebtsPaid() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoices(InvoiceState.PAID, InvoiceState.PAID);
        setUpUsers();
        
        debtsChecker = new DebtsPaymentChecker(usersHolder);
        
        boolean hasPaidResponse = debtsChecker.hasPaid(USER_ID_1, PROVIDER_1);
        
        assertTrue(hasPaidResponse);
    }
    
    // test case: When calling the hasPaid method for a FinanceUser and all the invoices 
    // in the user's list of debts are either in the state PAID or in the state WAITING, 
    // it must return true.
    @Test
    public void testSubscriptionDebtsWaiting() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoices(InvoiceState.WAITING, InvoiceState.PAID);
        setUpUsers();
        
        debtsChecker = new DebtsPaymentChecker(usersHolder);
        
        boolean hasPaidResponse = debtsChecker.hasPaid(USER_ID_1, PROVIDER_1);
        
        assertTrue(hasPaidResponse);
    }

    // test case: When calling the hasPaid method for a FinanceUser and the user has no
    // debts, it must return true.
    @Test
    public void testSubscriptionEmptyDebts() throws InternalServerErrorException, InvalidParameterException {
        setUpInvoices(InvoiceState.WAITING, InvoiceState.PAID);
        
        subscriptionsDebts = new ArrayList<String>();
        
        setUpUsers();
        
        debtsChecker = new DebtsPaymentChecker(usersHolder);
        
        boolean hasPaidResponse = debtsChecker.hasPaid(USER_ID_1, PROVIDER_1);
        
        assertTrue(hasPaidResponse);
    }
    
    private void setUpInvoices(InvoiceState stateInvoice1, InvoiceState stateInvoice2) {
        subscriptionsDebts = new ArrayList<String>();
        subscriptionsDebts.add(INVOICE_ID_1);
        subscriptionsDebts.add(INVOICE_ID_2);
        
        invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        Mockito.when(invoice1.getState()).thenReturn(stateInvoice1);
        
        invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        Mockito.when(invoice2.getState()).thenReturn(stateInvoice2);
        
        invoice3 = Mockito.mock(Invoice.class);
        Mockito.when(invoice3.getInvoiceId()).thenReturn(INVOICE_ID_3);
        
        invoices = new ArrayList<Invoice>();
        invoices.add(invoice1);
        invoices.add(invoice2);
        invoices.add(invoice3);
    }
    
    private void setUpUsers() throws InternalServerErrorException, InvalidParameterException {
        user1 = Mockito.mock(FinanceUser.class);
        Mockito.when(user1.getLastSubscriptionsDebts()).thenReturn(subscriptionsDebts);
        Mockito.when(user1.getInvoices()).thenReturn(invoices);
        
        usersHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(usersHolder.getUserById(USER_ID_1, PROVIDER_1)).thenReturn(user1);
    }
}
