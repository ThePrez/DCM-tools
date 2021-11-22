package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
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

import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;
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
                final String resp = ConsoleQuestionAsker.get().askUserForPwd("Enter output file password: ");
                return password = resp.toCharArray();
            } else {
                return password;
            }
        }

        public char[] getPasswordOrThrow() throws IOException {
            if (null != password) {
                return password;
            }
            if (StringUtils.isEmpty(password) && !isYesMode()) {
                final String resp = ConsoleQuestionAsker.get().askUserForPwd("Enter output file password: ");
                return password = resp.toCharArray();
            }
            throw new IOException("ERROR: Password is required");
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

    private final String m_fileName;

    public CertFileExporter(final String _fileName) {
        m_fileName = _fileName;
    }

    public void doExport(final ExportOptions _opts) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final DcmChangeTracker changeTracker = new DcmChangeTracker(_opts);
        final KeyStore destKs = KeyStore.getInstance(StringUtils.isEmpty(_opts.outputFileFormat) ? "pkcs12" : _opts.outputFileFormat);
        destKs.load(null, null);
        for (final String alias : Collections.list(changeTracker.getStartingSnapshot().getKeyStore().aliases())) {
            final Certificate cert = changeTracker.getStartingSnapshot().getKeyStore().getCertificate(alias);
            if (cert instanceof X509Certificate) {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal(((X509Certificate) cert).getIssuerX500Principal().getName(X500Principal.RFC1779), TerminalColor.CYAN));
            } else {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal("<unknown CN>", TerminalColor.BRIGHT_RED));
            }
            destKs.setCertificateEntry(alias, cert);
        }
        try (FileOutputStream out = new FileOutputStream(m_fileName)) {
            destKs.store(out, _opts.getPasswordOrThrow());
        }
    }

}
