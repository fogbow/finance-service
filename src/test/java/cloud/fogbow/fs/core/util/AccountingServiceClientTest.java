package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import cloud.fogbow.accs.core.models.specs.OrderSpec;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpRequestClient.class})
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

	// records fields
	private String userId = "user";
	private String requester = "requester";
	private String requestStartDate = "01-01-1970";
	private String requestEndDate = "01-01-2000";
	private String orderIdCompute1 = "orderIdCompute1";
	private String orderIdCompute2 = "orderIdCompute2";
	private String orderIdNetwork = "orderIdNetwork";
	private String resourceTypeCompute = "compute";
	private String resourceTypeNetwork = "network";
	private OrderSpec orderSpecCompute = new ComputeSpec(1, 100);
	private Timestamp recordStartTime = new Timestamp(0);
	private Timestamp recordStartDate = new Timestamp(0);
	private Timestamp recordEndTime = new Timestamp(100);
	private Timestamp recordEndDate = new Timestamp(100);
	private long duration = 10L;
	private OrderState orderState = OrderState.FULFILLED;
	private Long idRecordCompute1 = 1L;
	private Long idRecordCompute2 = 2L;
	private Long idRecordNetwork = 3L;
	
	// request / response fields
	private HttpResponse responseCompute;
	private HttpResponse responseNetwork;
	private Map<String, String> headers;
	private Map<String, String> body;
	private String urlCompute;
	private String urlNetwork;
	private JsonUtils jsonUtils;
	private Record recordCompute1;
	private Record recordCompute2;
	private Record recordNetwork;
	private ArrayList<Record> responseComputeRecords;
	private ArrayList<Record> responseNetworkRecords;
	private String responseComputeContent = "responseComputeContent";
	private String responseNetworkContent = "responseNetworkContent";
	private int successCode = 200;

	@Test
	public void testGetUserRecords() throws FogbowException {
		setUpAuthentication();
		setUpRecords();
		setUpResponse();
		setUpRequest();
		
		AccountingServiceClient accsClient = new AccountingServiceClient(authenticationServiceClient, 
				publicKey, localProvider, managerUserName, managerPassword, 
				accountingServiceAddress, accountingServicePort, jsonUtils);

		List<Record> userRecords = accsClient.getUserRecords(userId, requester, requestStartDate, requestEndDate);
		
		assertEquals(3, userRecords.size());
		assertTrue(userRecords.contains(recordCompute1));
		assertTrue(userRecords.contains(recordCompute2));
		assertTrue(userRecords.contains(recordNetwork));
	}

	private void setUpAuthentication() throws FogbowException {
		authenticationServiceClient = Mockito.mock(AuthenticationServiceClient.class);
		Mockito.when(authenticationServiceClient.getToken(publicKey, managerUserName, managerPassword)).thenReturn(adminToken);
	}
	
	private void setUpRecords() {
		this.recordCompute1 = new Record(idRecordCompute1, orderIdCompute1, resourceTypeCompute, 
				orderSpecCompute, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		this.recordCompute2 = new Record(idRecordCompute2, orderIdCompute2, resourceTypeCompute, 
				orderSpecCompute, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		this.recordNetwork = new Record(idRecordNetwork, orderIdNetwork, resourceTypeNetwork, 
				orderSpecCompute, requester, recordStartTime, recordStartDate, 
				recordEndTime, recordEndDate, duration, orderState);
		
		this.responseComputeRecords = new ArrayList<Record>();
		this.responseComputeRecords.add(recordCompute1);
		this.responseComputeRecords.add(recordCompute2);
		
		this.responseNetworkRecords = new ArrayList<Record>();
		this.responseNetworkRecords.add(recordNetwork);
	}
	
	private void setUpResponse() {
		this.jsonUtils = Mockito.mock(JsonUtils.class);
		
		Mockito.when(this.jsonUtils.fromJson(responseComputeContent, ArrayList.class)).thenReturn(responseComputeRecords);
		Mockito.when(this.jsonUtils.fromJson(responseNetworkContent, ArrayList.class)).thenReturn(responseNetworkRecords);
		
		responseCompute = Mockito.mock(HttpResponse.class);
		Mockito.when(responseCompute.getHttpCode()).thenReturn(successCode);
		Mockito.when(responseCompute.getContent()).thenReturn(responseComputeContent);
		
		responseNetwork = Mockito.mock(HttpResponse.class);
		Mockito.when(responseNetwork.getHttpCode()).thenReturn(successCode);
		Mockito.when(responseNetwork.getContent()).thenReturn(responseNetworkContent);
	}

	private void setUpRequest() throws FogbowException {
		// http://{accs-address}:{accs-port}/accs/usage/{userId}/{requester-provider}/{local-provider}/{resource-type}/{start-date}/{end-date}
		urlCompute = String.format("%s:%s/%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				resourceTypeCompute, requestStartDate, requestEndDate);
		urlNetwork = String.format("%s:%s/%s/%s/%s/%s/%s/%s/%s", accountingServiceAddress, accountingServicePort,
				cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, userId, requester, localProvider,
				resourceTypeNetwork, requestStartDate, requestEndDate);

		headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, AccountingServiceClient.RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, adminToken);
		body = new HashMap<String, String>();
		
		PowerMockito.mockStatic(HttpRequestClient.class);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, urlCompute, headers, body)).willReturn(responseCompute);
		BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.GET, urlNetwork, headers, body)).willReturn(responseNetwork);
	}
}
