package cloud.fogbow.fs.core.util;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.constants.Messages;

public class FinanceDataProtector {

    private RSAPrivateKey privateKey;
    
    public FinanceDataProtector() {
        try {
            this.privateKey = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
        } catch (InternalServerErrorException e) {
            throw new FatalErrorException(Messages.Exception.ERROR_READING_PRIVATE_KEY_FILE, e);
        }
    }
    
    public FinanceDataProtector(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }
    
    public String encrypt(String propertyValue, String publicKeyString) throws InternalServerErrorException {
        RSAPublicKey publicKey;
        
        try {
            publicKey = CryptoUtil.getPublicKeyFromString(publicKeyString);
        } catch (GeneralSecurityException e) {
            throw new InternalServerErrorException();
        }
        
        return AuthenticationUtil.encryptToken(propertyValue, this.privateKey, publicKey);
    }
}
