package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.security.auth.x500.X500Principal;

import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class CertFileExporter {
    public static class ExportOptions extends DcmUserOpts {
        public boolean isPasswordProtected = false;
        public String outputFileFormat = null;
        public char[] password = null;

        public String getOutputFileFormat() {
            return outputFileFormat;
        }

        public char[] getPasswordOrNull() throws IOException {
            if (!isPasswordProtected) {
                return null;
            }
            if (StringUtils.isEmpty(password) && !isYesMode()) {
                final String resp = ConsoleUtils.askUserForPwd("Enter input file password: ");
                return password = resp.toCharArray();
            } else {
                return password;
            }
        }

        public boolean isPasswordProtected() {
            return isPasswordProtected;
        }

        public void setOutputFileFormat(final String outputFileFormat) {
            this.outputFileFormat = outputFileFormat;
        }

        public void setPassword(final String password) {
            this.password = password.toCharArray();
        }

        public void setPasswordProtected(final boolean isPasswordProtected) {
            this.isPasswordProtected = isPasswordProtected;
        }

    }

    private static final String TEMP_KEYSTORE_PWD = StringUtils.generateRandomString(10);

    private final String m_fileName;

    public CertFileExporter(final String _fileName) {
        m_fileName = _fileName;
    }

    public void doExport(final ExportOptions _opts) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        final boolean isYesMode = _opts.isYesMode();
        final File tmpFile = exportDcmStore(_opts.isYesMode(), _opts.getDcmStore(), _opts.getDcmPassword(), null);

        final KeyStoreLoader loader = new KeyStoreLoader(tmpFile.getAbsolutePath(), TEMP_KEYSTORE_PWD);
        final KeyStore tempKs = loader.getKeyStore();
        final KeyStore destKs = KeyStore.getInstance(StringUtils.isEmpty(_opts.outputFileFormat) ? "pkcs12" : _opts.outputFileFormat);
        for (final String alias : Collections.list(tempKs.aliases())) {
            final Certificate cert = tempKs.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal(((X509Certificate) cert).getIssuerX500Principal().getName(X500Principal.RFC1779), TerminalColor.CYAN));
            } else {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal("<unknown CN>", TerminalColor.BRIGHT_RED));
            }
            destKs.setCertificateEntry(alias, cert);
        }
        try (FileOutputStream out = new FileOutputStream(m_fileName)) {
            destKs.store(out, _opts.getPasswordOrNull());
        }
    }

    public static File exportDcmStore(final boolean _isYesMode, final String _dcmStore, final String _dcmStorePw, final String _dest) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {
        final File dest;
        if (null == _dest) {
            dest = TempFileManager.createTempFile();
            dest.delete();
        } else {
            dest = new File(_dest);
        }
        try (DcmApiCaller apiCaller = new DcmApiCaller(_isYesMode)) {
            apiCaller.callQykmExportKeyStore(_dcmStore, _dcmStorePw, dest.getAbsolutePath(), TEMP_KEYSTORE_PWD);
        }
        return dest;
    }

}
