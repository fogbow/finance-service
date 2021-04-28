package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class InvoiceStateTest {

    // TODO documentation
    @Test
    public void testFromValue() throws InvalidParameterException {
        assertEquals(InvoiceState.DEFAULTING, InvoiceState.fromValue(InvoiceState.DEFAULTING.getValue()));
        assertEquals(InvoiceState.PAID, InvoiceState.fromValue(InvoiceState.PAID.getValue()));
        assertEquals(InvoiceState.WAITING, InvoiceState.fromValue(InvoiceState.WAITING.getValue()));
    }
    
    // TODO documentation
    @Test(expected = InvalidParameterException.class)
    public void testFromValueInvalidValue() throws InvalidParameterException {
        InvoiceState.fromValue("invalidvalue");
    }
}
