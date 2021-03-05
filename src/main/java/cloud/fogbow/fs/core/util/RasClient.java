package cloud.fogbow.fs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;

public class RasClient {
	
	private static final String RECORDS_REQUEST_CONTENT_TYPE = "application/json";
	
	private AuthenticationServiceClient authenticationServiceClient;
	private String publicKey;
	private String managerUsername;
	private String managerPassword;
	private String rasAddress;
	private String rasPort;
	
	public void pauseResourcesByUser(String userId) throws FogbowException {
		String token = authenticationServiceClient.getToken(publicKey, managerUsername, managerPassword);
		
		try {
			List<String> computeIds = getComputesList(userId, token);

			for (String computeId : computeIds) {
				pauseCompute(computeId, token);
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FogbowException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private List<String> getComputesList(String userId, String token) throws URISyntaxException, FogbowException {
		HttpResponse response = doComputesListRequestAndCheckStatus(token, userId);
		return getComputesListFromResponse(response);
	}

	private HttpResponse doComputesListRequestAndCheckStatus(String token, String userId) throws URISyntaxException, FogbowException {
		// TODO Add base endpoint
		String endpoint = getComputesListEndpoint("", userId);
		HttpResponse response = doComputesListRequest(token, endpoint, userId);
		if (response.getHttpCode() > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
			throw new UnavailableProviderException(e.getMessage());
		}

		return response;
	}

	private String getComputesListEndpoint(String computesListEndpoint, String userId) throws URISyntaxException {
		// TODO Currently this endpoint does not exist
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(computesListEndpoint).path("/").path(userId).
                build(true).toUri();
        return uri.toString();
	}

	private HttpResponse doComputesListRequest(String token, String endpoint, String userId) throws URISyntaxException, FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// TODO implement
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, body);
	}

	// TODO implement
	private List<String> getComputesListFromResponse(HttpResponse response) {
		return null;
	}
	
	private void pauseCompute(String computeId, String token) throws URISyntaxException, FogbowException {
		doPauseRequestAndCheckStatus(token, computeId);
	}
	
	private void doPauseRequestAndCheckStatus(String token, String computeId) throws URISyntaxException, FogbowException {
		// TODO Add base endpoint
		String endpoint = getPauseEndpoint("", computeId);
		HttpResponse response = doPauseRequest(token, endpoint);
		if (response.getHttpCode() > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
			throw new UnavailableProviderException(e.getMessage());
		}
	}

	private String getPauseEndpoint(String pauseApiBaseEndpoint, String computeId) throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(pauseApiBaseEndpoint).path("/").
        		// FIXME constant
        		path(computeId).path("/").path("pause").
                build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doPauseRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}

	public void resumeResourcesByUser(String userId) throws FogbowException {
		String token = authenticationServiceClient.getToken(publicKey, managerUsername, managerPassword);
		
		try {
			List<String> computeIds = getComputesList(userId, token);

			for (String computeId : computeIds) {
				resumeCompute(computeId, token);
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FogbowException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void resumeCompute(String computeId, String token) throws URISyntaxException, FogbowException {
		doResumeRequestAndCheckStatus(token, computeId);
	}
	
	private void doResumeRequestAndCheckStatus(String token, String computeId) throws URISyntaxException, FogbowException {
		// TODO Add base endpoint
		String endpoint = getResumeEndpoint("", computeId);
		HttpResponse response = doResumeRequest(token, endpoint);
		if (response.getHttpCode() > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
			throw new UnavailableProviderException(e.getMessage());
		}
	}
	
	private String getResumeEndpoint(String pauseApiBaseEndpoint, String computeId) throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(pauseApiBaseEndpoint).path("/").
        		// FIXME constant
        		path(computeId).path("/").path("resume").
                build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doResumeRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}
}
