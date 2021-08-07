package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

import com.github.ibmioss.dcmtools.CertFileExporter;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class KeyStoreInterrogator {
    private KeyStore m_keyStore;

    public KeyStoreInterrogator(final KeyStore _ks) {
        m_keyStore = _ks;
    }

    public boolean containsCert(final Certificate _cert) throws KeyStoreException {
        return null != getAliasOfCertOrNull(_cert);
    }

    public String getAliasOfCertOrNull(final Certificate _cert) throws KeyStoreException {
        for (String alias : Collections.list(m_keyStore.aliases())) {
            Certificate certInKeyStore = m_keyStore.getCertificate(alias);

            if (certInKeyStore.equals(_cert)) {
                // the .equals() can return true even with mismatched signatures....
                if (certInKeyStore instanceof X509Certificate && _cert instanceof X509Certificate) {
                    if (Arrays.equals(((X509Certificate) _cert).getSignature(), ((X509Certificate) _cert).getSignature())) {
                        return alias;
                    } else {
                        System.out.println(StringUtils.colorizeForTerminal("WARNING: found matching certificate with different signature", TerminalColor.YELLOW));
                        continue;
                    }
                } else {
                    return alias;
                }
            }
        }
        return null;
    }
    public static KeyStoreInterrogator getFromDCM(boolean _isYesMode, String _dcmStore, String _dcmStorePw) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException { 
        File tmpFile = CertFileExporter.exportDcmStore(_isYesMode, _dcmStore, _dcmStorePw, null);
        KeyStoreLoader loader= new KeyStoreLoader(tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD, null, false);
        return new KeyStoreInterrogator(loader.getKeyStore());
    }
}
