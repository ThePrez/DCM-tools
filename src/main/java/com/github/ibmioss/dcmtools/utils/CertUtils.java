package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import javax.security.auth.x500.X500Principal;

import com.github.theprez.jcmdutils.AppLogger;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class CertUtils {

    public static boolean areCertsEqual(final AppLogger _logger, final Certificate _cert1, final Certificate _cert2) throws CertificateEncodingException {
        if (!(_cert1 instanceof X509Certificate) || !(_cert2 instanceof X509Certificate)) {
            return _cert1.equals(_cert2);
        }
        final X509Certificate x1 = (X509Certificate) _cert1;
        final X509Certificate x2 = (X509Certificate) _cert2;
        if (Arrays.equals(x1.getTBSCertificate(), x2.getTBSCertificate())) {
            _logger.println_verbose("TBC cert match");
            return true;
        }
        if (Arrays.equals(x1.getSignature(), x2.getSignature())) {
            _logger.println_verbose("signatures match");
            return true;
        }
        if (Arrays.equals(x1.getPublicKey().getEncoded(), x2.getPublicKey().getEncoded())) {
            _logger.println_verbose("public keys match");
            return true;
        }
        if (Arrays.equals(x1.getEncoded(), x2.getEncoded())) {
            _logger.println_verbose("DER-encoded information matches");
            return true;
        }
        if (x1.toString().equals(x2.toString())) {
            return true;
        }
        return x1.equals(x2);
    }
    // here for debugging purposes
    // private static void dumpCertToFile(X509Certificate _cert, String _file) throws IOException {
    // try(FileWriter fw = new FileWriter(_file)) {
    // fw.write(""+_cert);
    // }
    // }

    public static File exportDcmStore(final AppLogger _logger, final boolean _isYesMode, final String _dcmStore, final String _dcmStorePw, final String _dest) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {
        final File dest;
        if (null == _dest) {
            dest = TempFileManager.createTempFile();
            dest.delete();
        } else {
            dest = new File(_dest);
        }
        try (DcmApiCaller apiCaller = new DcmApiCaller(_isYesMode)) {
            apiCaller.callQykmExportKeyStore(_logger, _dcmStore, _dcmStorePw, dest.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
        }
        return dest;
    }

    public static String getCertInfoStr(final Certificate _cert, final String _linePrefix) {
        if (!(_cert instanceof X509Certificate)) {
            return _linePrefix + "NOT AN X.509 CERT!" + _cert;
        }
        String ret = "";
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ssZ");
        final X509Certificate x509 = (X509Certificate) _cert;
        ret += _linePrefix + "Issuer: " + x509.getIssuerX500Principal().getName(X500Principal.RFC1779);
        ret += "\n";
        ret += _linePrefix + "Subject: " + x509.getSubjectX500Principal().getName(X500Principal.RFC1779);
        ret += "\n";
        ret += _linePrefix + "Valid From: " + simpleDateFormat.format(x509.getNotBefore());
        ret += "\n";
        ret += _linePrefix + "Valid Until: " + simpleDateFormat.format(x509.getNotAfter());
        ret += "\n";
        ret += _linePrefix + "Is CA? " + isCA(_cert);
        return ret;
    }

    private static boolean isCA(final Certificate _cert) {
        if (!(_cert instanceof X509Certificate)) {
            return false;
        }
        return -1 != ((X509Certificate) _cert).getBasicConstraints();
    }

    public static KeyStore mergeKeyStore(final KeyStore _dest, final KeyStore _src) throws KeyStoreException {
        for (final String alias : Collections.list(_src.aliases())) {
            _dest.setCertificateEntry(alias, _src.getCertificate(alias));
        }
        return _dest;
    }

}
