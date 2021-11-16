package cloud.fogbow.fs.core.util.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
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

public class RasClient {
    /**
     * Key used in the header of the requests to the Resource Allocation Service
     * to represent the content type.
     */
    @VisibleForTesting
	static final String RAS_REQUEST_CONTENT_TYPE = "application/json";
	
	private AuthenticationServiceClient authenticationServiceClient;
	private String publicKey;
	private String managerUsername;
	private String managerPassword;
	private String rasAddress;
	private String rasPort;
    private String token;
	
    // TODO add parameters validation
	public RasClient() throws ConfigurationErrorException {
		this(new AuthenticationServiceClient(),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_URL_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_PORT_KEY));
	}
	
	public RasClient(AuthenticationServiceClient authenticationServiceClient, String managerUserName, 
			String managerPassword, String rasAddress, String rasPort) throws ConfigurationErrorException {
		this.authenticationServiceClient = authenticationServiceClient;
		this.managerUsername = managerUserName;
		this.managerPassword = managerPassword;
		this.rasAddress = rasAddress;
		this.rasPort = rasPort;
		
		try {
			this.publicKey = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
		} catch (InternalServerErrorException e) {
			throw new ConfigurationErrorException(e.getMessage());
		} catch (GeneralSecurityException e) {
			throw new ConfigurationErrorException(e.getMessage());
		}
	}
	
	/*
	 * 
	 * PAUSE
	 * 
	 */
	
	public void pauseResourcesByUser(String userId, String provider) throws FogbowException {
		try {
		    if (this.token == null) {
		        this.token = getToken();
		    }

		    doPauseRequestAndCheckStatus(userId, provider);
		} catch (URISyntaxException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}
	
    private String getToken() throws FogbowException {
        String token = authenticationServiceClient.getToken(publicKey, managerUsername, managerPassword);
        Key keyToDecrypt = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
        Key keyToEncrypt = FsPublicKeysHolder.getInstance().getRasPublicKey(); 
        
        String newToken = TokenProtector.rewrap(keyToDecrypt, keyToEncrypt, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
        return newToken;
    }

	private void doPauseRequestAndCheckStatus(String userId, String provider) throws URISyntaxException, FogbowException {
		String endpoint = getPauseEndpoint(cloud.fogbow.ras.api.http.request.Compute.PAUSE_COMPUTE_ENDPOINT, 
		        userId, provider);
		HttpResponse response = doPauseRequest(this.token, endpoint);
		
		// If the token expired, authenticate and try again
		if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
		    this.token = getToken();
		    response = doPauseRequest(this.token, endpoint);
		}

        treatReturnCode(response.getHttpCode(), response.getContent(), endpoint);
	}

    private void treatReturnCode(int responseCode, String content, String endpoint)
            throws NotImplementedOperationException, UnavailableProviderException {
        if (responseCode == HttpStatus.SC_NOT_IMPLEMENTED) {
            throw new NotImplementedOperationException(
                    String.format(Messages.Exception.OPERATION_NOT_IMPLEMENTED, endpoint));
        }
		
		if (responseCode > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(responseCode, content);
			throw new UnavailableProviderException(e.getMessage());
		}
    }

	private String getPauseEndpoint(String pauseApiBaseEndpoint, String userId, String provider) 
	        throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(pauseApiBaseEndpoint).
        		path("/").path(userId).path("/").path(provider).build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doPauseRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RAS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}

	/*
	 * 
	 * HIBERNATE
	 * 
	 */
	
    public void hibernateResourcesByUser(String userId, String provider) throws FogbowException {
        try {
            if (this.token == null) {
                this.token = getToken();
            }

            doHibernateRequestAndCheckStatus(userId, provider);
        } catch (URISyntaxException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    private void doHibernateRequestAndCheckStatus(String userId, String provider)
            throws URISyntaxException, FogbowException {
        String endpoint = getHibernateEndpoint(cloud.fogbow.ras.api.http.request.Compute.HIBERNATE_COMPUTE_ENDPOINT, userId,
                provider);
        HttpResponse response = doHibernateRequest(this.token, endpoint);

        // If the token expired, authenticate and try again
        if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
            this.token = getToken();
            response = doHibernateRequest(this.token, endpoint);
        }

        treatReturnCode(response.getHttpCode(), response.getContent(), endpoint);
    }

    private String getHibernateEndpoint(String hibernateApiBaseEndpoint, String userId, String provider)
            throws URISyntaxException {
        URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(hibernateApiBaseEndpoint).path("/").path(userId)
                .path("/").path(provider).build(true).toUri();
        return uri.toString();
    }

    private HttpResponse doHibernateRequest(String token, String endpoint) throws FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, RAS_REQUEST_CONTENT_TYPE);
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);

        // body
        Map<String, String> body = new HashMap<String, String>();

        return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
    }
	
	/*
	 * 
	 * STOP
	 * 
	 */
	
    public void stopResourcesByUser(String userId, String provider) throws FogbowException {
        try {
            if (this.token == null) {
                this.token = getToken();
            }

            doStopRequestAndCheckStatus(userId, provider);
        } catch (URISyntaxException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    private void doStopRequestAndCheckStatus(String userId, String provider)
            throws URISyntaxException, FogbowException {
        String endpoint = getStopEndpoint(cloud.fogbow.ras.api.http.request.Compute.STOP_COMPUTE_ENDPOINT, userId,
                provider);
        HttpResponse response = doStopRequest(this.token, endpoint);

        // If the token expired, authenticate and try again
        if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
            this.token = getToken();
            response = doStopRequest(this.token, endpoint);
        }
        
        treatReturnCode(response.getHttpCode(), response.getContent(), endpoint);
    }

    private String getStopEndpoint(String stopApiBaseEndpoint, String userId, String provider)
            throws URISyntaxException {
        URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(stopApiBaseEndpoint).path("/").path(userId)
                .path("/").path(provider).build(true).toUri();
        return uri.toString();
    }

    private HttpResponse doStopRequest(String token, String endpoint) throws FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, RAS_REQUEST_CONTENT_TYPE);
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);

        // body
        Map<String, String> body = new HashMap<String, String>();

        return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
    }
	
	/*
	 * 
	 * RESUME
	 * 
	 */
	    
	public void resumeResourcesByUser(String userId, String provider) throws FogbowException {
		try {
            if (this.token == null) {
                this.token = getToken();
            }
            
            doResumeRequestAndCheckStatus(userId, provider);
		} catch (URISyntaxException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}
	
	private void doResumeRequestAndCheckStatus(String userId, String provider) throws URISyntaxException, FogbowException {
		String endpoint = getResumeEndpoint(cloud.fogbow.ras.api.http.request.Compute.RESUME_COMPUTE_ENDPOINT, 
		        userId, provider);
		HttpResponse response = doResumeRequest(this.token, endpoint);
		
		// If the token expired, authenticate and try again
		if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
		    this.token = getToken();
		    response = doResumeRequest(this.token, endpoint);
		}

		treatReturnCode(response.getHttpCode(), response.getContent(), endpoint);
	}
	
	private String getResumeEndpoint(String resumeApiBaseEndpoint, String userId, String provider) 
	        throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(resumeApiBaseEndpoint).path("/").
        		path(userId).path("/").path(provider).build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doResumeRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RAS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}
	
	/*
	 * 
	 * PURGE
	 * 
	 */
	
	public void purgeUser(String userId, String provider) throws FogbowException {
        try {
            if (this.token == null) {
                this.token = getToken();
            }

            purgeUserAndCheckStatus(userId, provider);
        } catch (URISyntaxException e) {
            throw new InvalidParameterException(e.getMessage());
        }
	}

    private void purgeUserAndCheckStatus(String userId, String provider) throws URISyntaxException, FogbowException {
        String endpoint = getPurgeUserEndpoint(cloud.fogbow.ras.api.http.request.Admin.PURGE_USER_ENDPOINT, 
                userId, provider);
        HttpResponse response = doPurgeUserRequest(this.token, endpoint);
        
        // If the token expired, authenticate and try again
        if (response.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
            this.token = getToken();
            response = doPurgeUserRequest(this.token, endpoint);
        }
        
        treatReturnCode(response.getHttpCode(), response.getContent(), endpoint); 
    }
    
    private String getPurgeUserEndpoint(String purgeUserApiBaseEndpoint, String userId, String provider) 
            throws URISyntaxException {
        URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(purgeUserApiBaseEndpoint).path("/").
                path(userId).path("/").path(provider).build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doPurgeUserRequest(String token, String endpoint) throws FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, RAS_REQUEST_CONTENT_TYPE);
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
        
        // body
        Map<String, String> body = new HashMap<String, String>();

        return HttpRequestClient.doGenericRequest(HttpMethod.DELETE, endpoint, headers, body);
    }
}
