package cloud.fogbow.fs.api.http.request;

import java.util.HashMap;

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
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;

//TODO documentation
@CrossOrigin
@RestController
@RequestMapping(value = Resources.RESOURCES_ENDPOINT)
public class Resources {
	public static final String RESOURCES_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "resources";
	
	// TODO documentation
	@RequestMapping(value = "/{provider}/{userId}", method = RequestMethod.PUT)
	public ResponseEntity<Boolean> updateFinanceState(
			@PathVariable String userId,
			@PathVariable String provider,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@RequestBody HashMap<String, String> financeState) throws FogbowException {
		ApplicationFacade.getInstance().updateFinanceState(systemUserToken, userId, provider, financeState);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
	
	// TODO documentation
	@RequestMapping(value = "/{provider}/{userId}/{property}", method = RequestMethod.GET)
	public ResponseEntity<String> getFinanceStateProperty(
			@PathVariable String userId,
			@PathVariable String provider,
			@PathVariable String property,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		String propertyValue = ApplicationFacade.getInstance().getFinanceStateProperty(systemUserToken, userId, provider, property);
		return new ResponseEntity<String>(propertyValue, HttpStatus.OK);
	}
}
