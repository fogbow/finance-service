package cloud.fogbow.fs.api.http.request;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;

// TODO documentation
@CrossOrigin
@RestController
@RequestMapping(value = User.USER_ENDPOINT)
public class User {
	public static final String USER_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "user";

	// TODO documentation
	@RequestMapping(value = "/{planName}", method = RequestMethod.POST)
    public ResponseEntity<Boolean> registerSelf(
            @PathVariable String planName,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().addSelf(systemUserToken, planName);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
	
	// TODO documentation
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> unregisterSelf(
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().unregisterSelf(systemUserToken);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/{planName}", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> changeSelfPlan(
            @PathVariable String planName,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().changeSelfPlan(systemUserToken, planName);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
}
