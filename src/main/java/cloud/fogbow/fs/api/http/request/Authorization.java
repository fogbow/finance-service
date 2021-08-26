package cloud.fogbow.fs.api.http.request;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationResponse;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.constants.ApiDocumentation;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin
@RestController
@RequestMapping(value = Authorization.AUTHORIZED_ENDPOINT)
@Api(description = ApiDocumentation.Authorization.API)
public class Authorization {
	public static final String AUTHORIZED_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "authorized";

	@ApiOperation(value = ApiDocumentation.Authorization.IS_AUTHORIZED)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<RemoteAuthorizationResponse> isAuthorized(
            @ApiParam(value = ApiDocumentation.Authorization.IS_AUTHORIZED_REQUEST_BODY)
            @RequestBody AuthorizableUser user) throws FogbowException {
        boolean authorized = ApplicationFacade.getInstance().isAuthorized(user.getUserToken(), user.getOperation());
        return new ResponseEntity<RemoteAuthorizationResponse>(new RemoteAuthorizationResponse(authorized), HttpStatus.OK);
    }
}
