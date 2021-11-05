package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class CertRenewer {

    private final List<String> m_fileNames;

    public CertRenewer(final List<String> _fileNames) throws IOException {
        m_fileNames = new LinkedList<String>();
        for (final String fileName : _fileNames) {
            m_fileNames.add(null == fileName ? KeyStoreLoader.extractTrustFromInstalledCerts() : fileName);
        }
    }

    public void doRenew(final DcmUserOpts _opts) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode();
        // Initialize keystore from file of unknown type
        final KeyStore keyStore = new KeyStoreLoader(m_fileNames, null, null, false).getKeyStore();

        for (final String alias : Collections.list(keyStore.aliases())) {
            renewCert(keyStore.getCertificate(alias));
        }
    }

    private void renewCert(final Certificate _cert) throws CertificateEncodingException, FileNotFoundException, IOException {
        try (FileOutputStream fos = new FileOutputStream(TempFileManager.createTempFile())) {
            fos.write(_cert.getEncoded());
        }
    }
}
