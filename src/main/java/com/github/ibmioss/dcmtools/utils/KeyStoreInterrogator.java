package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class KeyStoreInterrogator {
    public static KeyStoreInterrogator getFromDCM(final AppLogger _logger, final boolean _isYesMode, final String _dcmStore, final String _dcmStorePw)
            throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final File tmpFile = CertUtils.exportDcmStore(_logger, _isYesMode, _dcmStore, _dcmStorePw, null);
        final KeyStoreLoader loader = new KeyStoreLoader(_logger, Arrays.asList(tmpFile.getAbsolutePath()), TempFileManager.TEMP_KEYSTORE_PWD, null, false);
        return new KeyStoreInterrogator(loader.getKeyStore());
    }

    private final KeyStore m_keyStore;

    public KeyStoreInterrogator(final KeyStore _ks) {
        m_keyStore = _ks;
    }

    public boolean containsCert(final AppLogger _logger, final Certificate _cert) throws KeyStoreException, CertificateEncodingException {
        return null != getAliasOfCertOrNull(_logger, _cert);
    }

    public String getAliasOfCertOrNull(final AppLogger _logger, final Certificate _cert) throws KeyStoreException, CertificateEncodingException {
        final String apiAlias = m_keyStore.getCertificateAlias(_cert);
        if (StringUtils.isNonEmpty(apiAlias)) {
            return apiAlias;
        }
        for (final String alias : Collections.list(m_keyStore.aliases())) {
            final Certificate certInKeyStore = m_keyStore.getCertificate(alias);
            if (CertUtils.areCertsEqual(_logger, certInKeyStore, _cert)) {
                if (certInKeyStore instanceof X509Certificate && _cert instanceof X509Certificate) {
                    if (Arrays.equals(((X509Certificate) _cert).getSignature(), ((X509Certificate) _cert).getSignature())) {
                        return alias;
                    } else {
                        _logger.println_warn("WARNING: found matching certificate with different signature");
                        continue;
                    }
                } else {
                    return alias;
                }
            }
        }
        _logger.println("cert has no alias");
        return null;
    }

    public KeyStore getKeyStore() {
        return m_keyStore;
    }

}
