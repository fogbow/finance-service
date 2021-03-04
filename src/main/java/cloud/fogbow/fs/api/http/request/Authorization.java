package cloud.fogbow.fs.api.http.request;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.api.http.response.Authorized;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.constants.ApiDocumentation;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;
import io.swagger.annotations.Api;

@CrossOrigin
@RestController
@RequestMapping(value = Authorization.AUTHORIZED_ENDPOINT)
@Api(description = ApiDocumentation.Authorization.API)
public class Authorization {
	public static final String AUTHORIZED_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "authorized";

	// TODO documentation
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Authorized> isAuthorized(
            @RequestBody AuthorizableUser user) throws InvalidParameterException {
        boolean authorized = ApplicationFacade.getInstance().isAuthorized(user);
        return new ResponseEntity<Authorized>(new Authorized(authorized), HttpStatus.OK);
    }
}
