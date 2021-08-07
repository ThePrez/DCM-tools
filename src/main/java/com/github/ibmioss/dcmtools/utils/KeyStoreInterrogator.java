package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
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

    public boolean containsCert(final Certificate _cert) throws KeyStoreException, CertificateEncodingException {
        return null != getAliasOfCertOrNull(_cert);
    }


    private static boolean areCertsEqual(final Certificate _cert1, final Certificate _cert2) throws CertificateEncodingException {
        if (!(_cert1 instanceof X509Certificate) || !(_cert2 instanceof X509Certificate)) {
            return _cert1.equals(_cert2);
        }
        X509Certificate x1 = (X509Certificate) _cert1;
        X509Certificate x2 = (X509Certificate) _cert2;
        if (Arrays.equals(x1.getTBSCertificate(), x2.getTBSCertificate())) {
            System.out.println("TBC cert match");
            return true;
        }
        if (Arrays.equals(x1.getSignature(), x2.getSignature())) {
            System.out.println("signatures match");
            return true;
        }
//        if (Arrays.equals(x1.getPublicKey().getEncoded(), x2.getPublicKey().getEncoded())) {
//            System.out.println("public keys match");
//            return true;
//        }
        if (x1.toString().equals(x2.toString())) {
            return true;
        }
        return x1.equals(x2);
    }

//    private static void dumpCertToFile(X509Certificate x1, String string) {
//        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(string), "UTF-8")) {
//            osw.write("" + x1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public String getAliasOfCertOrNull(final Certificate _cert) throws KeyStoreException, CertificateEncodingException {
        for (String alias : Collections.list(m_keyStore.aliases())) {
            Certificate certInKeyStore = m_keyStore.getCertificate(alias);
            if (areCertsEqual(certInKeyStore, _cert)) {
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

    public KeyStore getKeyStore() {
        return m_keyStore;
    }

    public static KeyStoreInterrogator getFromDCM(boolean _isYesMode, String _dcmStore, String _dcmStorePw) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {
        File tmpFile = CertFileExporter.exportDcmStore(_isYesMode, _dcmStore, _dcmStorePw, null);
        KeyStoreLoader loader = new KeyStoreLoader(tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD, null, false);
        return new KeyStoreInterrogator(loader.getKeyStore());
    }
}
