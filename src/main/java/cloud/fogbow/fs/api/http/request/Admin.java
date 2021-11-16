package cloud.fogbow.fs.api.http.request;

import org.apache.log4j.Logger;
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
import cloud.fogbow.fs.constants.ApiDocumentation;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;
import cloud.fogbow.fs.constants.Messages;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin
@RestController
@RequestMapping(value = Admin.ADMIN_ENDPOINT)
@Api(description = ApiDocumentation.Admin.API)
public class Admin {
	public static final String ADMIN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "admin";
    public static final String RELOAD_ENDPOINT = "/reload";

    private final Logger LOGGER = Logger.getLogger(Admin.class);
    
    @ApiOperation(value = ApiDocumentation.Admin.RELOAD_OPERATION)
    @RequestMapping(value = RELOAD_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<Boolean> reload(
    				@ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
    				@RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        LOGGER.info(Messages.Log.RECEIVING_RELOAD_CONFIGURATION_REQUEST);
        ApplicationFacade.getInstance().reload(systemUserToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.SET_POLICY_OPERATION)
    @RequestMapping(value = "/policy", method = RequestMethod.POST)
    public ResponseEntity<Boolean> setPolicy(
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken, 
            @ApiParam(value = ApiDocumentation.Admin.SET_POLICY_REQUEST_BODY)
            @RequestBody Policy policy) throws FogbowException {
        ApplicationFacade.getInstance().setPolicy(systemUserToken, policy.getPolicy());
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.UPDATE_POLICY_OPERATION)
    @RequestMapping(value = "/policy", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> udpatePolicy(
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.Admin.UPDATE_POLICY_REQUEST_BODY)
            @RequestBody Policy policy) throws FogbowException {
        ApplicationFacade.getInstance().updatePolicy(systemUserToken, policy.getPolicy());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = ApiDocumentation.Admin.REGISTER_USER_OPERATION)
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<Boolean> registerUser(
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.Admin.REGISTER_USER_REQUEST_BODY)
            @RequestBody cloud.fogbow.fs.api.parameters.User user) throws FogbowException {
        ApplicationFacade.getInstance().addUser(systemUserToken, user.getUserId(), user.getProvider(), user.getFinancePlanName());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.CHANGE_USER_PLAN_OPERATION)
    @RequestMapping(value = "/user", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> changeUserPlan(
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.Admin.CHANGE_USER_PLAN_REQUEST_BODY)
            @RequestBody cloud.fogbow.fs.api.parameters.User user) throws FogbowException {
        ApplicationFacade.getInstance().changeUserPlan(systemUserToken, 
                user.getUserId(), user.getProvider(), user.getFinancePlanName());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.UNREGISTER_USER)
    @RequestMapping(value = "/user/unregister/{userId}/{provider:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> unregisterUser(
            @ApiParam(value = ApiDocumentation.Admin.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Admin.PROVIDER)
            @PathVariable String provider,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().unregisterUser(systemUserToken, userId, provider);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.REMOVE_USER)
    @RequestMapping(value = "/user/{userId}/{provider:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> removeUser(
            @ApiParam(value = ApiDocumentation.Admin.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Admin.PROVIDER)
            @PathVariable String provider,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().removeUser(systemUserToken, userId, provider);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
}
