package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
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

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.accs.core.models.orders.OrderState;
import cloud.fogbow.accs.core.models.specs.ComputeSpec;
import cloud.fogbow.accs.core.models.specs.NetworkAllocationMode;
import cloud.fogbow.accs.core.models.specs.NetworkSpec;
import cloud.fogbow.accs.core.models.specs.OrderSpec;
import cloud.fogbow.accs.core.models.specs.VolumeSpec;
import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.core.FsPublicKeysHolder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HttpRequestClient.class, ServiceAsymmetricKeysHolder.class, 
	FsPublicKeysHolder.class, TokenProtector.class, CryptoUtil.class })
public class AccountingServiceClientTest {
	
	// authentication fields
	private AuthenticationServiceClient authenticationServiceClient;
	private String publicKey = "publicKey";
	private String localProvider = "localProvider";
	private String managerUserName = "manager";
	private String managerPassword = "managerPassword";
	private String accountingServiceAddress = "http://localhost";
	private String accountingServicePort = "5001";
	private String adminToken = "adminToken";
	private String rewrapAdminToken = "rewrapAdminToken";

	// records fields
	private String userId = "user";
	private String requester = "requester";
	private String requestStartDate = "01-01-1970";
	private String requestEndDate = "01-01-2000";
	private String orderIdCompute1 = "orderIdCompute1";
	private String orderIdCompute2 = "orderIdCompute2";
	private String orderIdNetwork = "orderIdNetwork";
	private String orderIdVolume = "orderIdVolume";
	private String resourceTypeCompute = "compute";
	private String resourceTypeNetwork = "network";
	private String resourceTypeVolume = "volume";
	private OrderSpec orderSpecCompute = new ComputeSpec(1, 100);
	private OrderSpec orderSpecNetwork = new NetworkSpec("10.0.0.1/30", 
			NetworkAllocationMode.DYNAMIC);
	private OrderSpec orderSpecVolume = new VolumeSpec(1000);
	private Timestamp recordStartTime = new Timestamp(0);
	private Timestamp recordStartDate = new Timestamp(0);
	private Timestamp recordEndTime = new Timestamp(100);
	private Timestamp recordEndDate = new Timestamp(100);
	private long duration = 10L;
	private OrderState orderState = OrderState.FULFILLED;
	private Long idRecordCompute1 = 1L;
	private Long idRecordCompute2 = 2L;
	private Long idRecordNetwork = 3L;
	private Long idRecordVolume = 4L;
	
	// request / response fields
	private HttpResponse responseCompute;
	private HttpResponse responseNetwork;
	private HttpResponse responseVolume;
	private Map<String, String> headers;
	private Map<String, String> body;
	private String urlCompute;
	private String urlNetwork;
	private String urlVolume;
	private JsonUtils jsonUtils;
	private Record recordCompute1;
	private Record recordCompute2;
	private Record recordNetwork;
	private Record recordVolume;
	private ArrayList<Record> responseComputeRecords;
	private ArrayList<Record> responseNetworkRecords;
	private ArrayList<Record> responseVolumeRecords;
	private String responseComputeContent = "responseComputeContent";
	private String responseNetworkContent = "responseNetworkContent";
	private String responseVolumeContent = "responseVolumeContent";
	private int successCode = 200;
	private int errorCode = 500;

	// test case: When calling the method getUserRecords, it must set up 
	// one request for each resource type correctly and return the correct 
	// records for the user.
	@Test
	public void testGetUserRecords() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(successCode, successCode, successCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, jsonUtils);

		List<Record> userRecords = accsClient.getUserRecords(userId, requester, requestStartDate, requestEndDate);
		
		assertEquals(4, userRecords.size());
		assertTrue(userRecords.contains(recordCompute1));
		assertTrue(userRecords.contains(recordCompute2));
		assertTrue(userRecords.contains(recordNetwork));
		assertTrue(userRecords.contains(recordVolume));
	}

	// test case: When calling the method getUserRecords and the return code 
	// for the compute request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testGetUserRecordsErrorReturnCodeComputeRequest() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(errorCode, successCode, successCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, jsonUtils);

		accsClient.getUserRecords(userId, requester, requestStartDate, requestEndDate);
	}
	
	// test case: When calling the method getUserRecords and the return code
	// for the network request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testGetUserRecordsErrorReturnCodeNetworkRequest() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(successCode, errorCode, successCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, jsonUtils);

		accsClient.getUserRecords(userId, requester, requestStartDate, requestEndDate);
	}
	
	// test case: When calling the method getUserRecords and the return code
	// for the volume request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testGetUserRecordsErrorReturnCodeVolumeRequest() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRecords();
		setUpResponse(successCode, successCode, errorCode);
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, jsonUtils);

		accsClient.getUserRecords(userId, requester, requestStartDate, requestEndDate);
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
		BDDMockito.when(TokenProtector.rewrap(fsPrivateKey, accsPublicKey, adminToken, 
				FogbowConstants.TOKEN_STRING_SEPARATOR)).thenReturn(rewrapAdminToken);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		BDDMockito.when(CryptoUtil.toBase64(fsPublicKey)).thenReturn(publicKey);
	}

	private void setUpAuthentication() throws FogbowException {
		authenticationServiceClient = Mockito.mock(AuthenticationServiceClient.class);
		Mockito.when(authenticationServiceClient.getToken(publicKey, managerUserName, 
				managerPassword)).thenReturn(adminToken);
	}
	
	private void setUpRecords() {
		this.recordCompute1 = new Record(idRecordCompute1, orderIdCompute1, resourceTypeCompute, 
				orderSpecCompute, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		this.recordCompute2 = new Record(idRecordCompute2, orderIdCompute2, resourceTypeCompute, 
				orderSpecCompute, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		this.recordNetwork = new Record(idRecordNetwork, orderIdNetwork, resourceTypeNetwork, 
				orderSpecNetwork, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		this.recordVolume = new Record(idRecordVolume, orderIdVolume, resourceTypeVolume, 
				orderSpecVolume, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		
		this.responseComputeRecords = new ArrayList<Record>();
		this.responseComputeRecords.add(recordCompute1);
		this.responseComputeRecords.add(recordCompute2);
		
		this.responseNetworkRecords = new ArrayList<Record>();
		this.responseNetworkRecords.add(recordNetwork);
		
		this.responseVolumeRecords = new ArrayList<Record>();
		this.responseVolumeRecords.add(recordVolume);
	}
	
	private void setUpResponse(int returnCodeComputeRequest, int returnCodeNetworkRequest, int returnCodeVolumeRequest) {
		this.jsonUtils = Mockito.mock(JsonUtils.class);
		
		Mockito.when(this.jsonUtils.fromJson(responseComputeContent, ArrayList.class)).thenReturn(responseComputeRecords);
		Mockito.when(this.jsonUtils.fromJson(responseNetworkContent, ArrayList.class)).thenReturn(responseNetworkRecords);
		Mockito.when(this.jsonUtils.fromJson(responseVolumeContent, ArrayList.class)).thenReturn(responseVolumeRecords);
		
		responseCompute = Mockito.mock(HttpResponse.class);
		Mockito.when(responseCompute.getHttpCode()).thenReturn(returnCodeComputeRequest);
		Mockito.when(responseCompute.getContent()).thenReturn(responseComputeContent);
		
		responseNetwork = Mockito.mock(HttpResponse.class);
		Mockito.when(responseNetwork.getHttpCode()).thenReturn(returnCodeNetworkRequest);
		Mockito.when(responseNetwork.getContent()).thenReturn(responseNetworkContent);
		
		responseVolume = Mockito.mock(HttpResponse.class);
		Mockito.when(responseVolume.getHttpCode()).thenReturn(returnCodeVolumeRequest);
		Mockito.when(responseVolume.getContent()).thenReturn(responseVolumeContent);
	}

	private void setUpRequest() throws FogbowException {
		// http://{accs-address}:{accs-port}/accs/usage/{userId}/{requester-provider}/{local-provider}/{resource-type}/{start-date}/{end-date}
		urlCompute = String.format("%s:%s/%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				resourceTypeCompute, requestStartDate, requestEndDate);
		urlNetwork = String.format("%s:%s/%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				resourceTypeNetwork, requestStartDate, requestEndDate);
		urlVolume = String.format("%s:%s/%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				resourceTypeVolume, requestStartDate, requestEndDate);
		
		headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, AccountingServiceClient.RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken);
		body = new HashMap<String, String>();
		
		PowerMockito.mockStatic(HttpRequestClient.class);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, urlCompute, headers, body)).willReturn(responseCompute);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, urlNetwork, headers, body)).willReturn(responseNetwork);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, urlVolume, headers, body)).willReturn(responseVolume);
	}
}
