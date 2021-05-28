package cloud.fogbow.fs.api.http.request;

import java.util.HashMap;
import java.util.Map;

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
import cloud.fogbow.fs.api.parameters.FinanceOptions;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;

//TODO documentation
@CrossOrigin
@RestController
@RequestMapping(value = Plan.PLAN_ENDPOINT)
public class Plan {
	public static final String PLAN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "plan";
	public static final String PLAN_USER_SUFFIX = "/user";
	
	// TODO documentation
	@RequestMapping(value = PLAN_USER_SUFFIX + "/{provider}/{userId}", method = RequestMethod.PUT)
	public ResponseEntity<Boolean> updateFinanceState(
			@PathVariable String userId,
			@PathVariable String provider,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@RequestBody HashMap<String, String> financeState) throws FogbowException {
		ApplicationFacade.getInstance().updateFinanceState(systemUserToken, userId, provider, financeState);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
	
	// TODO documentation
	@RequestMapping(value = PLAN_USER_SUFFIX + "/{provider}/{userId}/{property}", method = RequestMethod.GET)
	public ResponseEntity<String> getFinanceStateProperty(
			@PathVariable String userId,
			@PathVariable String provider,
			@PathVariable String property,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		String propertyValue = ApplicationFacade.getInstance().getFinanceStateProperty(systemUserToken, userId, provider, property);
		return new ResponseEntity<String>(propertyValue, HttpStatus.OK);
	}
	
	// TODO documentation
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Boolean> createFinancePlan(
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@RequestBody cloud.fogbow.fs.api.parameters.FinancePlan financePlan) throws FogbowException {
		ApplicationFacade.getInstance().createFinancePlan(systemUserToken, financePlan.getPluginClassName(), 
		        financePlan.getFinancePlanName(), financePlan.getPlanInfo());
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
	
	// TODO documentation
	@RequestMapping(value = "/{planName}", method = RequestMethod.GET)
	public ResponseEntity<cloud.fogbow.fs.api.http.response.FinancePlan> getFinancePlan(
			@PathVariable String planName,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		Map<String, String> planInfo = ApplicationFacade.getInstance().getFinancePlan(systemUserToken, planName);
		return new ResponseEntity<cloud.fogbow.fs.api.http.response.FinancePlan>(
		        new cloud.fogbow.fs.api.http.response.FinancePlan(planName, planInfo), HttpStatus.OK);
	}
	
	// TODO documentation
    @RequestMapping(value = "/{planName}", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> changeFinancePlanOptions(
            @PathVariable String planName,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @RequestBody FinanceOptions financeOptions) throws FogbowException {
        ApplicationFacade.getInstance().changePlanOptions(systemUserToken, planName, financeOptions.getFinanceOptions());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }

	// TODO documentation
	@RequestMapping(value = "/{planName}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> removeFinancePlan(
			@PathVariable String planName,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		ApplicationFacade.getInstance().removeFinancePlan(systemUserToken, planName);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
}
