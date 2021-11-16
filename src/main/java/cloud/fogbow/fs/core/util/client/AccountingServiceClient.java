package cloud.fogbow.fs.core.util.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.accs.api.http.response.AccsApiUtils;
import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.FsPublicKeysHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.util.TimeUtils;

public class AccountingServiceClient {
    /**
     * String used to represent the resource type compute in 
     * the records provided by the Accounting Service.
     */
    @VisibleForTesting
	static final String COMPUTE_RESOURCE = "compute";
    /**
     * String used to represent the resource type volume in 
     * the records provided by the Accounting Service.
     */
    @VisibleForTesting
	static final String VOLUME_RESOURCE = "volume";
    /**
     * Key used in the header of the resources usage request to
     * represent the content type.
     */
	@VisibleForTesting
	static final String RECORDS_REQUEST_CONTENT_TYPE = "application/json";

	private AuthenticationServiceClient authenticationServiceClient;
	private String managerUserName;
	private String managerPassword;
	private String accountingServiceAddress;
	private String accountingServicePort;
	private String localProvider;
	private String publicKeyString;
	private AccsApiUtils recordUtil;
	private TimeUtils timeUtils;
	private String token;
	
	public AccountingServiceClient() throws ConfigurationErrorException {
		this(new AuthenticationServiceClient(), new AccsApiUtils(), new TimeUtils());
	}
	
	public AccountingServiceClient(AuthenticationServiceClient authenticationServiceClient, 
	        AccsApiUtils recordUtil, TimeUtils timeUtils) throws ConfigurationErrorException, FatalErrorException {
	    this(authenticationServiceClient, PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY),
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY),
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY),
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY),
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY), 
                recordUtil, timeUtils);
	}
	
	public AccountingServiceClient(AuthenticationServiceClient authenticationServiceClient, 
			String localProvider, String managerUserName, String managerPassword, 
			String accountingServiceAddress, String accountingServicePort, AccsApiUtils recordUtil, 
			TimeUtils timeUtils) 
					throws ConfigurationErrorException {
	    checkNotNull(localProvider, ConfigurationPropertyKeys.PROVIDER_ID_KEY);
	    checkNotNull(managerUserName, ConfigurationPropertyKeys.MANAGER_USERNAME_KEY);
	    checkNotNull(managerPassword, ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY);
	    checkNotNull(accountingServiceAddress, ConfigurationPropertyKeys.ACCS_URL_KEY);
	    checkNotNull(accountingServicePort, ConfigurationPropertyKeys.ACCS_PORT_KEY);
	    
		this.authenticationServiceClient = authenticationServiceClient;
		this.localProvider = localProvider;
		this.managerUserName = managerUserName;
		this.managerPassword = managerPassword;
		this.accountingServiceAddress = accountingServiceAddress;
		this.accountingServicePort = accountingServicePort;
		this.recordUtil = recordUtil;
		this.timeUtils = timeUtils;
		
		try {
			this.publicKeyString = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
		} catch (InternalServerErrorException e) {
			throw new ConfigurationErrorException(e.getMessage());
		} catch (GeneralSecurityException e) {
			throw new ConfigurationErrorException(e.getMessage());
		}
	}
	
    private void checkNotNull(String parameter, String parameterName) throws ConfigurationErrorException {
        if (parameter == null) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.INVALID_CONFIGURATION_VALUE, 
                    parameterName, parameter));
        }
    }

    public List<Record> getUserRecords(String userId, String requester, long startTime, long endTime) throws FogbowException {
        try {
            String requestStartDate = this.timeUtils.toDate(
                    cloud.fogbow.accs.constants.SystemConstants.COMPLETE_DATE_FORMAT, startTime);
            String requestEndDate = this.timeUtils.toDate(
                    cloud.fogbow.accs.constants.SystemConstants.COMPLETE_DATE_FORMAT, endTime);
            
            if (this.token == null) {
                this.token = getToken();
            }

            HttpResponse response = doRequestAndCheckStatus(userId, requester, localProvider, requestStartDate, 
                    requestEndDate);
            List<Record> userRecords = getRecordsFromResponse(response);
            return filterUsefulRecords(userRecords);
        } catch (URISyntaxException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private String getToken() throws FogbowException {
        String token = authenticationServiceClient.getToken(publicKeyString, managerUserName, managerPassword);
        Key keyToDecrypt = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
        Key keyToEncrypt = FsPublicKeysHolder.getInstance().getAccsPublicKey(); 
        
        String newToken = TokenProtector.rewrap(keyToDecrypt, keyToEncrypt, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
        return newToken;
    }

    private HttpResponse doRequestAndCheckStatus(String userId, String requester, 
    		String localProvider, String startDate, String endDate) throws URISyntaxException, FogbowException {
        String endpoint = getAccountingEndpoint(cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, 
        		userId, requester, localProvider, startDate, endDate);
        HttpResponse response = doRequest(this.token, endpoint);
        
        // If the token expired, authenticate and try again
        if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
            this.token = getToken();
            response = doRequest(this.token, endpoint);
        }
        
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }

    // http://{accs-address}:{accs-port}/accs/usage/{userId}/{requester-provider}/{local-provider}/{start-date}/{end-date}
    private String getAccountingEndpoint(String path, String userId, String requester, 
            String localProvider, String startDate, String endDate) throws URISyntaxException {
        URI uri = new URI(accountingServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(accountingServicePort).path(path).path("/").
                path(userId).path("/").path(requester).path("/").path(localProvider).path("/").
                path(startDate).path("/").path(endDate).build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doRequest(String token, String endpoint) throws URISyntaxException, FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
        // body
        Map<String, String> body = new HashMap<String, String>();
        
        return HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, body);
    }
    
    private List<Record> getRecordsFromResponse(HttpResponse response) throws InvalidParameterException {
    	return recordUtil.getRecordsFromString(response.getContent());
    }
    
    private List<Record> filterUsefulRecords(List<Record> userRecords) {
        List<Record> filteredRecords = new ArrayList<Record>();
        
        for (Record record : userRecords) {
            if (record.getResourceType().equals(COMPUTE_RESOURCE) || 
                    record.getResourceType().equals(VOLUME_RESOURCE)) {
                filteredRecords.add(record);
            }
        }
        
        return filteredRecords;
    }
}
