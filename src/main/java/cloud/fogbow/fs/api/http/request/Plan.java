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
import cloud.fogbow.fs.api.parameters.PublicKey;
import cloud.fogbow.fs.constants.ApiDocumentation;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin
@RestController
@RequestMapping(value = Plan.PLAN_ENDPOINT)
@Api(description = ApiDocumentation.Plan.API)
public class Plan {
	public static final String PLAN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "plan";
	public static final String PLAN_USER_SUFFIX = "/user";
	
	@ApiOperation(value = ApiDocumentation.Plan.UPDATE_FINANCE_STATE_OPERATION)
	@RequestMapping(value = PLAN_USER_SUFFIX + "/{userId}/{provider:.+}", method = RequestMethod.PUT)
	public ResponseEntity<Boolean> updateFinanceState(
	        @ApiParam(value = ApiDocumentation.Plan.USER_ID)
			@PathVariable String userId,
			@ApiParam(value = ApiDocumentation.Plan.PROVIDER)
			@PathVariable String provider,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@ApiParam(value = ApiDocumentation.Plan.UPDATE_FINANCE_STATE_REQUEST_BODY)
			@RequestBody HashMap<String, String> financeState) throws FogbowException {
		ApplicationFacade.getInstance().updateFinanceState(systemUserToken, userId, provider, financeState);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
	
	@ApiOperation(value = ApiDocumentation.Plan.GET_FINANCE_STATE_PROPERTY_OPERATION)
	@RequestMapping(value = PLAN_USER_SUFFIX + "/{userId}/{provider}/{property:.+}", method = RequestMethod.GET)
	public ResponseEntity<String> getFinanceStateProperty(
	        @ApiParam(value = ApiDocumentation.Plan.USER_ID)
			@PathVariable String userId,
			@ApiParam(value = ApiDocumentation.Plan.PROVIDER)
			@PathVariable String provider,
			@ApiParam(value = ApiDocumentation.Plan.PROPERTY)
			@PathVariable String property,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@ApiParam(value = ApiDocumentation.Plan.GET_FINANCE_STATE_PROPERTY_REQUEST_BODY)
			@RequestBody PublicKey publicKey) throws FogbowException {
		String propertyValue = ApplicationFacade.getInstance().getFinanceStateProperty(systemUserToken, userId, provider, 
		        property, publicKey.getPublicKey());
		return new ResponseEntity<String>(propertyValue, HttpStatus.OK);
	}
	
	@ApiOperation(value = ApiDocumentation.Plan.CREATE_FINANCE_PLAN_OPERATION)
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Boolean> createFinancePlan(
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@ApiParam(value = ApiDocumentation.Plan.CREATE_FINANCE_PLAN_REQUEST_BODY)
			@RequestBody cloud.fogbow.fs.api.parameters.FinancePlan financePlan) throws FogbowException {
		ApplicationFacade.getInstance().createFinancePlan(systemUserToken, financePlan.getPluginClassName(), 
		        financePlan.getFinancePlanName(), financePlan.getPlanInfo());
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
	
	@ApiOperation(value = ApiDocumentation.Plan.GET_FINANCE_PLAN_OPERATION)
	@RequestMapping(value = "/{planName:.+}", method = RequestMethod.GET)
	public ResponseEntity<cloud.fogbow.fs.api.http.response.FinancePlan> getFinancePlan(
	        @ApiParam(value = ApiDocumentation.Plan.PLAN_NAME)
			@PathVariable String planName,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		Map<String, String> planInfo = ApplicationFacade.getInstance().getFinancePlan(systemUserToken, planName);
		return new ResponseEntity<cloud.fogbow.fs.api.http.response.FinancePlan>(
		        new cloud.fogbow.fs.api.http.response.FinancePlan(planName, planInfo), HttpStatus.OK);
	}
	
	@ApiOperation(value = ApiDocumentation.Plan.CHANGE_FINANCE_PLAN_OPTIONS_OPERATION)
    @RequestMapping(value = "/{planName:.+}", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> changeFinancePlanOptions(
            @ApiParam(value = ApiDocumentation.Plan.PLAN_NAME)
            @PathVariable String planName,
            @RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.Plan.CHANGE_FINANCE_PLAN_OPTIONS_REQUEST_BODY)
            @RequestBody FinanceOptions financeOptions) throws FogbowException {
        ApplicationFacade.getInstance().changePlanOptions(systemUserToken, planName, financeOptions.getFinanceOptions());
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }

	@ApiOperation(value = ApiDocumentation.Plan.REMOVE_FINANCE_PLAN_OPERATION)
	@RequestMapping(value = "/{planName:.+}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> removeFinancePlan(
	        @ApiParam(value = ApiDocumentation.Plan.PLAN_NAME)
			@PathVariable String planName,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
		ApplicationFacade.getInstance().removeFinancePlan(systemUserToken, planName);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
}
