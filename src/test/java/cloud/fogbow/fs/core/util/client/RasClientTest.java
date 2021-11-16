package cloud.fogbow.fs.core.util.client;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
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
public class RasClientTest {

	private AuthenticationServiceClient authenticationServiceClient;
	private String rasAddress = "http://localhost";
	private String rasPort = "5001";
	private String managerUserName = "managerUsername";
	private String managerPassword = "managerPassword";
	private String adminToken1 = "adminToken1";
	private String adminToken2 = "adminToken2";
	private String rewrapAdminToken1 = "rewrapAdminToken1";
	private String rewrapAdminToken2 = "rewrapAdminToken2";
	private String publicKey = "publicKey";
	private String rasPauseEndpoint;
	private String rasHibernateEndpoint;
	private String rasStopEndpoint;
	private String rasResumeEndpoint;
	private String rasPurgeUserEndpoint;
	private String userId = "userId";
	private String provider = "provider";
	private HashMap<String, String> headers1;
	private HashMap<String, String> headers2;
	private HashMap<String, String> body;
	private HttpResponse responsePause;
	private HttpResponse responseHibernate;
	private HttpResponse responseStop;
	private HttpResponse responseResume;
	private HttpResponse responsePurge;
	
	// test case: When calling the method pauseResourcesByUser, it must set up
	// a request properly and call the HttpRequestClient to make the request to RAS.
	@Test
	public void testPauseResourcesByUser() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndOkResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.pauseResourcesByUser(userId, provider);
		
		PowerMockito.verifyStatic(HttpRequestClient.class);
		HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers1, body);
	}
	
	// test case: When calling the method pauseResourcesByUser and the return code
    // for the request is 501, it must throw an NotImplementedOperationException.
    @Test(expected = NotImplementedOperationException.class)
    public void testNotImplementedPauseResourcesByUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndNotImplementedResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.pauseResourcesByUser(userId, provider);
    }
	
	// test case: When calling the method pauseResourcesByUser and the return code
	// for the request is not 200 nor 501, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testPauseResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndErrorResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.pauseResourcesByUser(userId, provider);
	}

	// test case: When calling the method hibernateResourcesByUser, it must set up
    // a request properly and call the HttpRequestClient to make the request to RAS.
    @Test
    public void testHibernateResourcesByUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.hibernateResourcesByUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class);
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasHibernateEndpoint, headers1, body);
    }
    
    // test case: When calling the method hibernateResourcesByUser and the return code
    // for the request is 501, it must throw an NotImplementedOperationException.
    @Test(expected = NotImplementedOperationException.class)
    public void testNotImplementedHibernateResourcesByUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndNotImplementedResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.hibernateResourcesByUser(userId, provider);
    }
	
    // test case: When calling the method hibernateResourcesByUser and the return code
    // for the request is not 200 nor 501, it must throw an UnavailableProviderException.
    @Test(expected = UnavailableProviderException.class)
    public void testHibernateResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndErrorResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.hibernateResourcesByUser(userId, provider);
    }
    
    // test case: When calling the method stopResourcesByUser, it must set up
    // a request properly and call the HttpRequestClient to make the request to RAS.
    @Test
    public void testStopResourcesByUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.stopResourcesByUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class);
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasStopEndpoint, headers1, body);
    }
    
    // test case: When calling the method stopResourcesByUser and the return code
    // for the request is 501, it must throw an NotImplementedOperationException.
    @Test(expected = NotImplementedOperationException.class)
    public void testNotImplementedStopResourcesByUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndNotImplementedResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.stopResourcesByUser(userId, provider);
    }
    
    // test case: When calling the method stopResourcesByUser and the return code
    // for the request is not 200 nor 501, it must throw an UnavailableProviderException.
    @Test(expected = UnavailableProviderException.class)
    public void testStopResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndErrorResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.stopResourcesByUser(userId, provider);
    }
	
	// test case: When calling the method resumeResourcesByUser, it must set up
	// a request properly and call the HttpRequestClient to make the request to RAS.
	@Test
	public void testResumeResourcesByUser() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndOkResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.resumeResourcesByUser(userId, provider);
		
		PowerMockito.verifyStatic(HttpRequestClient.class);
		HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers1, body);
	}
	
	// test case: When calling the method resumeResourcesByUser and the return code
	// for the request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testResumeResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndErrorResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.resumeResourcesByUser(userId, provider);
	}
	
	// test case: When calling the method purgeUser, it must set up
    // a request properly and call the HttpRequestClient to make the request to RAS.
    @Test
    public void testPurgeUser() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.purgeUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class);
        HttpRequestClient.doGenericRequest(HttpMethod.DELETE, rasPurgeUserEndpoint, headers1, body);
    }
    
    // test case: When calling the method purgeUser and the return code
    // for the request is not 200, it must throw an UnavailableProviderException.
    @Test(expected = UnavailableProviderException.class)
    public void testPurgeUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndErrorResponse();
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.purgeUser(userId, provider);
    }
	
	// test case: When calling the method pauseResourcesByUser, if the token expired, 
	// the method must reacquire the token and perform the request again.
    @Test
    public void testPauseResourcesTokenExpired()
            throws InternalServerErrorException, UnauthenticatedUserException, FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        Mockito.when(responsePause.getHttpCode()).thenReturn(401, 200);
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.pauseResourcesByUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers1, body);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers2, body);
    }
    
    // test case: When calling the method resumeResourcesByUser, if the token expired, 
    // the method must reacquire the token and perform the request again.
    @Test
    public void testResumeResourcesTokenExpired()
            throws InternalServerErrorException, UnauthenticatedUserException, FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        Mockito.when(responseResume.getHttpCode()).thenReturn(401, 200);
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.resumeResourcesByUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers1, body);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers2, body);
    }
    
    // test case: When calling the method purgeUser, if the token expired, 
    // the method must reacquire the token and perform the request again.
    @Test
    public void testPurgeUserTokenExpired()
            throws InternalServerErrorException, UnauthenticatedUserException, FogbowException, GeneralSecurityException {
        setUpKeys();
        setUpAuthentication();
        setUpRequestAndOkResponse();
        
        Mockito.when(responsePurge.getHttpCode()).thenReturn(401, 200);
        
        RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
                managerPassword, rasAddress, rasPort);
        
        rasClient.purgeUser(userId, provider);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.DELETE, rasPurgeUserEndpoint, headers1, body);
        
        PowerMockito.verifyStatic(HttpRequestClient.class, Mockito.times(1));
        HttpRequestClient.doGenericRequest(HttpMethod.DELETE, rasPurgeUserEndpoint, headers2, body);
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

		RSAPublicKey rasPublicKey = Mockito.mock(RSAPublicKey.class);

		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		FsPublicKeysHolder fsPublicKeysHolder = Mockito.mock(FsPublicKeysHolder.class);
		Mockito.when(fsPublicKeysHolder.getRasPublicKey()).thenReturn(rasPublicKey);
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(fsPublicKeysHolder);

		PowerMockito.mockStatic(TokenProtector.class);
		BDDMockito.when(
				TokenProtector.rewrap(fsPrivateKey, rasPublicKey, adminToken1, FogbowConstants.TOKEN_STRING_SEPARATOR))
				.thenReturn(rewrapAdminToken1);
		BDDMockito.when(
                TokenProtector.rewrap(fsPrivateKey, rasPublicKey, adminToken2, FogbowConstants.TOKEN_STRING_SEPARATOR))
                .thenReturn(rewrapAdminToken2);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		BDDMockito.when(CryptoUtil.toBase64(fsPublicKey)).thenReturn(publicKey);
	}
	
	private void setUpAuthentication() throws FogbowException {
		authenticationServiceClient = Mockito.mock(AuthenticationServiceClient.class);
		Mockito.when(authenticationServiceClient.getToken(publicKey, managerUserName, 
				managerPassword)).thenReturn(adminToken1, adminToken2);
	}
	
	private void setUpRequestAndResponse(int returnCode) throws FogbowException {
		headers1 = new HashMap<String, String>();
		headers1.put(CommonKeys.CONTENT_TYPE_KEY, RasClient.RAS_REQUEST_CONTENT_TYPE);
		headers1.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken1);
		
		headers2 = new HashMap<String, String>();
        headers2.put(CommonKeys.CONTENT_TYPE_KEY, RasClient.RAS_REQUEST_CONTENT_TYPE);
        headers2.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken2);
        
		body = new HashMap<String, String>();
		
		rasPauseEndpoint = String.format("%s:%s/%s/%s/%s", rasAddress, rasPort,
				cloud.fogbow.ras.api.http.request.Compute.PAUSE_COMPUTE_ENDPOINT, userId, provider);
		rasHibernateEndpoint = String.format("%s:%s/%s/%s/%s", rasAddress, rasPort,
                cloud.fogbow.ras.api.http.request.Compute.HIBERNATE_COMPUTE_ENDPOINT, userId, provider);
		rasStopEndpoint = String.format("%s:%s/%s/%s/%s", rasAddress, rasPort,
                cloud.fogbow.ras.api.http.request.Compute.STOP_COMPUTE_ENDPOINT, userId, provider);
		rasResumeEndpoint = String.format("%s:%s/%s/%s/%s", rasAddress, rasPort,
				cloud.fogbow.ras.api.http.request.Compute.RESUME_COMPUTE_ENDPOINT, userId, provider);
		rasPurgeUserEndpoint = String.format("%s:%s/%s/%s/%s", rasAddress, rasPort,
                cloud.fogbow.ras.api.http.request.Admin.PURGE_USER_ENDPOINT, userId, provider);
		
		responsePause = Mockito.mock(HttpResponse.class);
		Mockito.when(responsePause.getHttpCode()).thenReturn(returnCode);
		
		responseHibernate = Mockito.mock(HttpResponse.class);
        Mockito.when(responseHibernate.getHttpCode()).thenReturn(returnCode);
		
        responseStop = Mockito.mock(HttpResponse.class);
        Mockito.when(responseStop.getHttpCode()).thenReturn(returnCode);
        
		responseResume = Mockito.mock(HttpResponse.class);
		Mockito.when(responseResume.getHttpCode()).thenReturn(returnCode);
		
		responsePurge = Mockito.mock(HttpResponse.class);
        Mockito.when(responsePurge.getHttpCode()).thenReturn(returnCode);

		PowerMockito.mockStatic(HttpRequestClient.class);
		
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers1, body)).thenReturn(responsePause);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers2, body)).thenReturn(responsePause);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasHibernateEndpoint, headers1, body)).thenReturn(responseHibernate);
        BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasHibernateEndpoint, headers2, body)).thenReturn(responseHibernate);
        BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasStopEndpoint, headers1, body)).thenReturn(responseStop);
        BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasStopEndpoint, headers2, body)).thenReturn(responseStop);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers1, body)).thenReturn(responseResume);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers2, body)).thenReturn(responseResume);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.DELETE, rasPurgeUserEndpoint, headers1, body)).thenReturn(responsePurge);
        BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.DELETE, rasPurgeUserEndpoint, headers2, body)).thenReturn(responsePurge);
	}

	private void setUpRequestAndOkResponse() throws FogbowException {
		setUpRequestAndResponse(200);
	}
	
	private void setUpRequestAndErrorResponse() throws FogbowException {
		setUpRequestAndResponse(500);
	}
	
	private void setUpRequestAndNotImplementedResponse() throws FogbowException {
        setUpRequestAndResponse(501);
    }
}
