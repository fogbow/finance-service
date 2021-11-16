package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthenticationUtil.class, ServiceAsymmetricKeysHolder.class, 
    CryptoUtil.class})
public class FinanceDataProtectorTest {

    private static final String PROPERTY_VALUE = "propertyValue";
    private static final String PUBLIC_KEY_STRING = "publicKeyString";
    private static final String ENCRYPTED_VALUE = "encryptedValue";
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private FinanceDataProtector financeDataProtector;
    
    @Before
    public void setUp() throws InternalServerErrorException, GeneralSecurityException {
        this.publicKey = Mockito.mock(RSAPublicKey.class);
        this.privateKey = Mockito.mock(RSAPrivateKey.class);
        
        PowerMockito.mockStatic(CryptoUtil.class);
        BDDMockito.given(CryptoUtil.getPublicKeyFromString(PUBLIC_KEY_STRING)).willReturn(this.publicKey);
        
        PowerMockito.mockStatic(AuthenticationUtil.class);
        BDDMockito.given(AuthenticationUtil.encryptToken(PROPERTY_VALUE, this.privateKey, this.publicKey)).
        willReturn(ENCRYPTED_VALUE);
        
        this.financeDataProtector = new FinanceDataProtector(this.privateKey);
    }
    
    // test case: When calling the encrypt method, it must call the CryptoUtil to get the correct public key,
    // then call the AuthenticationUtil to encrypt the value and return the encrypted value.
    @Test
    public void testEncryptSuccessfully() throws InternalServerErrorException, GeneralSecurityException {
        String returnedValue = this.financeDataProtector.encrypt(PROPERTY_VALUE, PUBLIC_KEY_STRING);
        
        assertEquals(ENCRYPTED_VALUE, returnedValue);
        
        PowerMockito.verifyStatic(CryptoUtil.class);
        CryptoUtil.getPublicKeyFromString(PUBLIC_KEY_STRING);
        
        PowerMockito.verifyStatic(AuthenticationUtil.class);
        AuthenticationUtil.encryptToken(PROPERTY_VALUE, privateKey, publicKey);
    }
    
    // test case: When calling the encrypt method and it catches a GeneralSecurityException when
    // getting the public key, it must throw an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testEncryptGetPublicKeyFromStringFails() throws InternalServerErrorException, GeneralSecurityException {
        BDDMockito.given(CryptoUtil.getPublicKeyFromString(PUBLIC_KEY_STRING)).willThrow(new GeneralSecurityException());
        
        this.financeDataProtector.encrypt(PROPERTY_VALUE, PUBLIC_KEY_STRING);
    }
    
    // test case: When creating a new FinanceDataProtector, the constructor must call the 
    // ServiceAsymmetricKeysHolder to get the FinanceService private key.
    @Test
    public void testFinanceDataProtectorConstructor() throws InternalServerErrorException {
        ServiceAsymmetricKeysHolder keysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.when(keysHolder.getPrivateKey()).thenReturn(privateKey);
        
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(keysHolder);
        
        new FinanceDataProtector();
        
        PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class);
        ServiceAsymmetricKeysHolder.getInstance();
        Mockito.verify(keysHolder).getPrivateKey();
    }
    
    // test case: When creating a new FinanceDataProtector and the constructor catches an
    // InternalServerErrorException when acquiring the private key, it must throw a FatalErrorException.
    @Test(expected = FatalErrorException.class)
    public void testFinanceDataProtectorConstructorErrorOnReadingPrivateKey() throws InternalServerErrorException {
        ServiceAsymmetricKeysHolder keysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.when(keysHolder.getPrivateKey()).thenThrow(new InternalServerErrorException());
        
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(keysHolder);
        
        new FinanceDataProtector();
    }
}
