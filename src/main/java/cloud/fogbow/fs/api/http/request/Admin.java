package cloud.fogbow.fs.api.http.request;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.api.parameters.Policy;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;
import io.swagger.annotations.ApiParam;

// TODO documentation
// TODO change positions of provider and userId parameters
@CrossOrigin
@RestController
@RequestMapping(value = Admin.ADMIN_ENDPOINT)
public class Admin {
	public static final String ADMIN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "admin";
    public static final String RELOAD_ENDPOINT = "/reload";

    // TODO documentation
    @RequestMapping(value = RELOAD_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<Boolean> reload(
    				@ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
    				@RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().reload(systemUserToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/policy", method = RequestMethod.POST)
    public ResponseEntity<Boolean> setPolicy(
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken, 
            @RequestBody Policy policy) throws FogbowException {
        ApplicationFacade.getInstance().setPolicy(systemUserToken, policy.getPolicy());
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/policy", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> udpatePolicy(
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @RequestBody Policy policy) throws FogbowException {
        ApplicationFacade.getInstance().updatePolicy(systemUserToken, policy.getPolicy());
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<Boolean> registerUser(
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @RequestBody cloud.fogbow.fs.api.parameters.User user) throws FogbowException {
        ApplicationFacade.getInstance().addUser(systemUserToken, user.getUserId(), user.getProvider(), user.getFinancePlanName());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/user", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> changeUserPlan(
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken, 
            @RequestBody cloud.fogbow.fs.api.parameters.User user) throws FogbowException {
        ApplicationFacade.getInstance().changeUserPlan(systemUserToken, 
                user.getUserId(), user.getProvider(), user.getFinancePlanName());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/user/unregister/{provider}/{userId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> unregisterUser(
            @PathVariable String userId,
            @PathVariable String provider,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().unregisterUser(systemUserToken, userId, provider);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    // TODO documentation
    @RequestMapping(value = "/user/{provider}/{userId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> removeUser(
            @PathVariable String userId,
            @PathVariable String provider,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().removeUser(systemUserToken, userId, provider);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
}
