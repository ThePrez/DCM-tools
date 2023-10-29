package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.File;
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

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class CertRenewer {

    private final List<String> m_fileNames;

    public CertRenewer(final AppLogger _logger, final List<String> _fileNames) throws IOException {
        m_fileNames = new LinkedList<String>();
        for (final String fileName : _fileNames) {
            m_fileNames.add(null == fileName ? KeyStoreLoader.extractTrustFromInstalledCerts(_logger) : fileName);
        }
    }

    public void doRenew(final AppLogger _logger, final DcmUserOpts _opts) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode();
        // Initialize keystore from file of unknown type
        final KeyStore keyStore = new KeyStoreLoader(_logger, m_fileNames, null, null, false).getKeyStore();

        for (final String alias : Collections.list(keyStore.aliases())) {
            renewCert(_logger, keyStore.getCertificate(alias),_opts);
        }
    }

    private void renewCert(final AppLogger _logger, final Certificate _cert, final DcmUserOpts _opts) throws CertificateEncodingException, FileNotFoundException, IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {
        File tmpFile = TempFileManager.createTempFile();
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            fos.write(_cert.getEncoded());
        }
        new DcmApiCaller(_opts.isYesMode()).callQycdRenewCertificate_RNWC0300(_logger, tmpFile.getAbsolutePath());
        tmpFile.delete();
    }
}
