package cloud.fogbow.fs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PropertiesHolder;

// TODO tests
public class AuthenticationServiceClient {

	public static final String AUTHENTICATION_REQUEST_CONTENT_TYPE = "application/json";
	private static final String TOKEN_RESPONSE_KEY = "token";
	private static final String PUBLIC_KEY_REQUEST_KEY = "publicKey";
	private static final String CREDENTIALS_REQUEST_KEY = "credentials";
	private static final String PASSWORD_REQUEST_KEY = "password";
	private static final String USERNAME_REQUEST_KEY = "username";
	
	private String authorizationServiceAddress;
	private String authorizationServicePort;

	public AuthenticationServiceClient() {
        this(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY), 
			 PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY));
	}
	
	public AuthenticationServiceClient(String authorizationServiceAddress, 
			String authorizationServicePort) {
		this.authorizationServiceAddress = authorizationServiceAddress;
		this.authorizationServicePort = authorizationServicePort;
	}
	
	public String getToken(String publicKey, String userName, String password) throws FogbowException {
		try {
			HttpResponse response = doRequestAndCheckStatus(publicKey, userName, password);
			return getTokenFromResponse(response);
		} catch (URISyntaxException e) {
			// TODO Improve
			throw new FogbowException(e.getMessage());
		}
	}

    private HttpResponse doRequestAndCheckStatus(String publicKey, 
    		String userName, String password) throws URISyntaxException, FogbowException {
        String endpoint = getAuthenticationEndpoint(cloud.fogbow.as.api.http.request.Token.TOKEN_ENDPOINT);
        HttpResponse response = doRequest(endpoint, publicKey, userName, password);

        if (response.getHttpCode() > HttpStatus.SC_CREATED) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }
    
    private String getAuthenticationEndpoint(String path) throws URISyntaxException {
        URI uri = new URI(authorizationServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(authorizationServicePort).
                path(path).
                build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doRequest(String endpoint, String publicKey, 
    		String userName, String password)
            throws URISyntaxException, FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHENTICATION_REQUEST_CONTENT_TYPE);
        
        // body
        Map<String, Object> body = asRequestBody(publicKey, userName, password);
        
        return HttpRequestClient.doGenericRequestGenericBody(HttpMethod.POST, endpoint, headers, body);
    }
    
    private Map<String, Object> asRequestBody(String publicKey, String userName, String password) {
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(USERNAME_REQUEST_KEY, userName);
		credentials.put(PASSWORD_REQUEST_KEY, password);
		
		Map<String, Object> body = new HashMap<String, Object>();
		body.put(CREDENTIALS_REQUEST_KEY, credentials);
		body.put(PUBLIC_KEY_REQUEST_KEY, publicKey);

		return body;
	}
    
    private String getTokenFromResponse(HttpResponse response) {
        Gson gson = new Gson();
        Map<String, String> jsonResponse = gson.fromJson(response.getContent(), HashMap.class);
        return jsonResponse.get(TOKEN_RESPONSE_KEY);
    }
}
