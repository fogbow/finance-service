package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class UserStateTest {

    // test case: When calling the fromValue method, it must return
    // the UserState instance represented by the given String.
    @Test
    public void testFromValue() throws InvalidParameterException {
        assertEquals(UserState.DEFAULT, UserState.fromValue(UserState.DEFAULT.getValue()));
        assertEquals(UserState.WAITING_FOR_STOP, UserState.fromValue(UserState.WAITING_FOR_STOP.getValue()));
        assertEquals(UserState.STOPPING, UserState.fromValue(UserState.STOPPING.getValue()));
        assertEquals(UserState.STOPPED, UserState.fromValue(UserState.STOPPED.getValue()));
        assertEquals(UserState.RESUMING, UserState.fromValue(UserState.RESUMING.getValue()));
    }
    
    // test case: When calling the fromValue method passing as argument
    // a String that does not represent any UserState instance, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testFromValueInvalidValue() throws InvalidParameterException {
        UserState.fromValue("invalidvalue");
    }
}
