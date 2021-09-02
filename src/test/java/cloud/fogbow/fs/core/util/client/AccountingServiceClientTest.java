package cloud.fogbow.fs.core.util.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.accs.api.http.response.AccsApiUtils;
import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.FsPublicKeysHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.util.TimeUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HttpRequestClient.class, ServiceAsymmetricKeysHolder.class, 
	FsPublicKeysHolder.class, TokenProtector.class, CryptoUtil.class, 
	PropertiesHolder.class })
public class AccountingServiceClientTest {
    // authentication fields
	private AuthenticationServiceClient authenticationServiceClient;
	private String publicKey = "publicKey";
	private String localProvider = "localProvider";
	private String managerUserName = "manager";
	private String managerPassword = "managerPassword";
	private String accountingServiceAddress = "http://localhost";
	private String accountingServicePort = "5001";
	private String adminToken1 = "adminToken1";
	private String adminToken2 = "adminToken2";
	private String rewrapAdminToken1 = "rewrapAdminToken1";
	private String rewrapAdminToken2 = "rewrapAdminToken2";

	// records fields
	private long startTime = 100;
	private long endTime = 200;
	private String userId = "user";
	private String requester = "requester";
	private String requestStartDate = "01-01-1970";
	private String requestEndDate = "01-01-2000";
	
	// request / response fields
	private HttpResponse responseCompute;
	private Map<String, String> headers1;
	private Map<String, String> headers2;
	private Map<String, String> body;
	private String accsUrl;
	private AccsApiUtils recordUtils;
	private Record recordCompute1;
	private Record recordCompute2;
	private Record recordVolume;
	private Record recordNetwork;
	private ArrayList<Record> response;
	private String responseContent = "responseContent";
	private int successCode = 200;
	private int errorCode = 500;
	private int expiredTokenCode = 401;
    private TimeUtils timeUtils;
    private PropertiesHolder propertiesHolder;

	// test case: When calling the method getUserRecords, it must set up 
	// the request correctly and return only compute and volume records.
	@Test
	public void testGetUserRecords() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(successCode, successCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, recordUtils, timeUtils);

		List<Record> userRecords = accsClient.getUserRecords(userId, requester, startTime, endTime);
		
		assertEquals(3, userRecords.size());
		assertTrue(userRecords.contains(recordCompute1));
		assertTrue(userRecords.contains(recordCompute2));
		assertTrue(userRecords.contains(recordVolume));
		assertFalse(userRecords.contains(recordNetwork));
	}

	// test case: When calling the method getUserRecords and the return code 
	// for the request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testGetUserRecordsErrorReturnCodeComputeRequest() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(errorCode, successCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, recordUtils, timeUtils);

		accsClient.getUserRecords(userId, requester, startTime, endTime);
	}
	
	// test case: When calling the method getUserRecords and the return code
	// for any request is 401, it must reacquire the token and perform the request again.
	@Test
	public void testGetUserRecordsTokenExpired()
	        throws InternalServerErrorException, UnauthenticatedUserException, FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRecords();
        setUpResponse(successCode, successCode);
        setUpRequest();
        
        Mockito.when(responseCompute.getHttpCode()).thenReturn(expiredTokenCode, successCode);
        
        AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
                localProvider, managerUserName, managerPassword, 
                accountingServiceAddress, accountingServicePort, recordUtils, timeUtils);
        
        accsClient.getUserRecords(userId, requester, startTime, endTime);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.GET, accsUrl, headers1, body);
        HttpRequestClient.doGenericRequest(HttpMethod.GET, accsUrl, headers2, body);
	}
	
	// test case: When creating a new AccountingServiceClient instance, the constructor must read
	// the configuration parameters properly.
	@Test
	public void testConstructor() 
	        throws FogbowException, GeneralSecurityException {
	    setUpKeys();
	    setUpConfiguration(localProvider, managerUserName, managerPassword, accountingServiceAddress, accountingServicePort);

	    new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
	    
	    Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
	    Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY);
	    Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY);
	    Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY);
	    Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY);
	}
	
	// test case: When creating a new AccountingServiceClient instance, if the providerId is null, 
	// the constructor must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
    public void testConstructorThrowsConfigurationErrorExceptionIfProviderIdIsNull() 
            throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpConfiguration(null, managerUserName, managerPassword, accountingServiceAddress,
                accountingServicePort);

        new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
    }
    
	// test case: When creating a new AccountingServiceClient instance, if the managerUserName is null, 
    // the constructor must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorThrowsConfigurationErrorExceptionIfManagerUsernameIsNull()
            throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpConfiguration(localProvider, null, managerPassword, accountingServiceAddress,
                accountingServicePort);

        new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
    }
    
    // test case: When creating a new AccountingServiceClient instance, if the managerPassword is null, 
    // the constructor must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorThrowsConfigurationErrorExceptionIfManagerPasswordIsNull()
            throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpConfiguration(localProvider, managerUserName, null, accountingServiceAddress,
                accountingServicePort);

        new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
    }
    
    // test case: When creating a new AccountingServiceClient instance, if the accountingServiceAddress is null, 
    // the constructor must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorThrowsConfigurationErrorExceptionIfAccountingServiceAddressIsNull() 
            throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpConfiguration(localProvider, managerUserName, managerPassword, null,
                accountingServicePort);

        new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
    }
    
    // test case: When creating a new AccountingServiceClient instance, if the accountingServicePort is null, 
    // the constructor must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorThrowsConfigurationErrorExceptionIfAccountingServicePortIsNull()
            throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpConfiguration(localProvider, managerUserName, managerPassword, accountingServiceAddress,
                null);

        new AccountingServiceClient(authenticationServiceClient, recordUtils, timeUtils);
    }
	
	private void setUpConfiguration(String localProvider, String managerUserName, 
	        String managerPassword, String accountingServiceAddress, String accountingServicePort) {
	    this.propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY)).thenReturn(localProvider);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY)).thenReturn(managerUserName);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY)).thenReturn(managerPassword);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY)).thenReturn(accountingServiceAddress);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY)).thenReturn(accountingServicePort);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
	}
	
	private void setUpKeys() throws InternalServerErrorException, FogbowException, UnauthenticatedUserException,
			GeneralSecurityException {
		RSAPublicKey fsPublicKey = Mockito.mock(RSAPublicKey.class);
		RSAPrivateKey fsPrivateKey = Mockito.mock(RSAPrivateKey.class);
		
		PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
		ServiceAsymmetricKeysHolder serviceAsymmetricKeysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
		Mockito.when(serviceAsymmetricKeysHolder.getPublicKey()).thenReturn(fsPublicKey);
		Mockito.when(serviceAsymmetricKeysHolder.getPrivateKey()).thenReturn(fsPrivateKey);
		BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(serviceAsymmetricKeysHolder);

		
		RSAPublicKey accsPublicKey = Mockito.mock(RSAPublicKey.class);
		
		
		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		FsPublicKeysHolder fsPublicKeysHolder = Mockito.mock(FsPublicKeysHolder.class);
		Mockito.when(fsPublicKeysHolder.getAccsPublicKey()).thenReturn(accsPublicKey);
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(fsPublicKeysHolder);
		
		
		PowerMockito.mockStatic(TokenProtector.class);
		BDDMockito.when(TokenProtector.rewrap(fsPrivateKey, accsPublicKey, adminToken1, 
				FogbowConstants.TOKEN_STRING_SEPARATOR)).thenReturn(rewrapAdminToken1);
	      BDDMockito.when(TokenProtector.rewrap(fsPrivateKey, accsPublicKey, adminToken2, 
	                FogbowConstants.TOKEN_STRING_SEPARATOR)).thenReturn(rewrapAdminToken2);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		BDDMockito.when(CryptoUtil.toBase64(fsPublicKey)).thenReturn(publicKey);
	}

	private void setUpAuthentication() throws FogbowException {
		authenticationServiceClient = Mockito.mock(AuthenticationServiceClient.class);
		Mockito.when(authenticationServiceClient.getToken(publicKey, managerUserName, 
				managerPassword)).thenReturn(adminToken1, adminToken2);
	}
	
	private void setUpRecords() {
		this.recordCompute1 = Mockito.mock(Record.class);
		Mockito.when(this.recordCompute1.getResourceType()).thenReturn(
		        AccountingServiceClient.COMPUTE_RESOURCE);
		
		this.recordCompute2 = Mockito.mock(Record.class);
	    Mockito.when(this.recordCompute2.getResourceType()).thenReturn(
	                AccountingServiceClient.COMPUTE_RESOURCE);
		
		this.recordVolume = Mockito.mock(Record.class);
	    Mockito.when(this.recordVolume.getResourceType()).thenReturn(
                  AccountingServiceClient.VOLUME_RESOURCE);
	    
	    this.recordNetwork = Mockito.mock(Record.class);
        Mockito.when(this.recordNetwork.getResourceType()).thenReturn(
                  "network");

		this.response = new ArrayList<Record>();
		this.response.add(recordCompute1);
		this.response.add(recordCompute2);
		this.response.add(recordVolume);
		this.response.add(recordNetwork);
	}
	
	private void setUpResponse(int returnCodeComputeRequest, int returnCodeVolumeRequest) throws InvalidParameterException {
	    this.timeUtils = Mockito.mock(TimeUtils.class);
	    
	    Mockito.when(this.timeUtils.toDate(
	            cloud.fogbow.accs.constants.SystemConstants.COMPLETE_DATE_FORMAT, startTime)).
	            thenReturn(requestStartDate);
	    Mockito.when(this.timeUtils.toDate(
	            cloud.fogbow.accs.constants.SystemConstants.COMPLETE_DATE_FORMAT, endTime)).
	            thenReturn(requestEndDate);
	    
		this.recordUtils = Mockito.mock(AccsApiUtils.class);
		
		Mockito.when(this.recordUtils.getRecordsFromString(responseContent)).thenReturn(response);

		responseCompute = Mockito.mock(HttpResponse.class);
		Mockito.when(responseCompute.getHttpCode()).thenReturn(returnCodeComputeRequest);
		Mockito.when(responseCompute.getContent()).thenReturn(responseContent);
	}

	private void setUpRequest() throws FogbowException {
		// http://{accs-address}:{accs-port}/accs/usage/{userId}/{requester-provider}/{local-provider}/{start-date}/{end-date}
		accsUrl = String.format("%s:%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				requestStartDate, requestEndDate);

		headers1 = new HashMap<String, String>();
		headers1.put(CommonKeys.CONTENT_TYPE_KEY, AccountingServiceClient.RECORDS_REQUEST_CONTENT_TYPE);
		headers1.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken1);
		
		headers2 = new HashMap<String, String>();
        headers2.put(CommonKeys.CONTENT_TYPE_KEY, AccountingServiceClient.RECORDS_REQUEST_CONTENT_TYPE);
        headers2.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken2);
		
		body = new HashMap<String, String>();
		
		PowerMockito.mockStatic(HttpRequestClient.class);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, accsUrl, headers1, body)).willReturn(responseCompute);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, accsUrl, headers2, body)).willReturn(responseCompute);
	}
}
