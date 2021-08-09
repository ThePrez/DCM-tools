package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import com.github.ibmioss.dcmtools.utils.CertUtils;
import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker.DcmChange;
import com.github.ibmioss.dcmtools.utils.KeyStoreInterrogator;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class CertFileImporter {
    public static class ImportOptions extends DcmUserOpts {
        private boolean isCasOnly = false;
        private boolean isPasswordProtected = false;
        private String label = null;
        private String password = null;

        public String getLabel() {
            return this.label;
        }

        public String getPasswordOrNull() throws IOException {
            if (!isPasswordProtected) {
                return null;
            }
            if (StringUtils.isEmpty(password) && !isYesMode()) {
                final String resp = ConsoleUtils.askUserForPwd("Enter input file password: ");
                return password = resp;
            } else {
                return password;
            }
        }

        public boolean isCasOnly() {
            return this.isCasOnly;
        }

        public boolean isPasswordProtected() {
            return isPasswordProtected;
        }

        public void setCasOnly(final boolean _casOnly) {
            this.isCasOnly = _casOnly;
        }

        public void setLabel(final String _label) {
            this.label = _label;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public void setPasswordProtected(final boolean isPasswordProtected) {
            this.isPasswordProtected = isPasswordProtected;
        }
    }

    private final List<String> m_fileNames;

    public CertFileImporter(final List<String> _fileNames) throws IOException {
        m_fileNames = new LinkedList<String>();
        for (final String fileName : _fileNames) {
            m_fileNames.add(null == fileName ? KeyStoreLoader.extractTrustFromInstalledCerts() : fileName);
        }
    }

    public void doImport(final ImportOptions _opts) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode();
        // Initialize keystore from file of unknown type
        final KeyStore keyStore = new KeyStoreLoader(m_fileNames, _opts.getPasswordOrNull(), _opts.getLabel(), _opts.isCasOnly()).getKeyStore();
        System.out.println(StringUtils.colorizeForTerminal("Sanity check successful", TerminalColor.GREEN));

        final DcmChangeTracker dcmTracker = new DcmChangeTracker(_opts);
        final KeyStoreInterrogator dcmChecker = dcmTracker.getStartingSnapshot();

        // Check for conflicting aliases, where the certificate is already in the store under a different alias
        for (final String alias : Collections.list(keyStore.aliases())) {
            System.out.println("checking for conflicting cert to the one with alias " + alias);
            final Certificate cert = keyStore.getCertificate(alias);
            final String conflictingAlias = dcmChecker.getAliasOfCertOrNull(cert);
            if (null != conflictingAlias) {
                System.out.println(StringUtils.colorizeForTerminal("WARNING: The following certificate already exists in the keystore with certificate id '" + conflictingAlias + "'. Certificate will not be imported:\n" + CertUtils.getCertInfoStr(cert, "    "), TerminalColor.YELLOW));
                keyStore.deleteEntry(alias);
            }
        }
        // check for the case where the alias already exists and we might overwrite it on import
        for (final String alias : Collections.list(keyStore.aliases())) {
            System.out.println("checking cert at alias " + alias);
            final Certificate cert = keyStore.getCertificate(alias);
            final Certificate preExistingCert = dcmChecker.getKeyStore().getCertificate(alias);
            if (null != preExistingCert) {
                System.out.println(StringUtils.colorizeForTerminal("WARNING: The following already exists with certificate id '" + alias + "' and will not be imported. Perhaps you mean to use the 'dcmrenew' tool instead?:\n" + CertUtils.getCertInfoStr(preExistingCert, "    "), TerminalColor.YELLOW));
            }
        }
        if (!keyStore.aliases().hasMoreElements()) {
            throw new IOException("No certificates to import");
        }
        // Ask user confirmation
        System.out.println("The following certificates will be processed:");
        for (final String alias : Collections.list(keyStore.aliases())) {
            final Certificate cert = keyStore.getCertificate(alias);
            System.out.println("    Certificate ID '" + alias + "':");
            System.out.println(StringUtils.colorizeForTerminal(CertUtils.getCertInfoStr(cert, "        "), TerminalColor.CYAN));
        }
        final String reply = isYesMode ? "y" : ConsoleUtils.askUserWithDefault("Do you want to import ALL of the above certificates into DCM? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            throw new IOException("No certificates to import");
        }

        // Convert the KeyStore object to a file in the format needed by the DCM API
        final String dcmImportFile = new KeyStoreLoader(keyStore).saveToDcmApiFormatFile(TempFileManager.TEMP_KEYSTORE_PWD);
        ;

        // .... and... call the DCM API to do the import!
        try (DcmApiCaller caller = new DcmApiCaller(isYesMode)) {
            caller.callQykmImportKeyStore(_opts.getDcmStore(), _opts.getDcmPassword(), dcmImportFile, TempFileManager.TEMP_KEYSTORE_PWD);
        }

        List<DcmChange> changes = dcmTracker.getChanges();
        if (changes.isEmpty()) {
            throw new IOException("No changes were made to the DCM keystore!");
        }
        System.out.println("The following changes were made on the DCM keystore:");
        for (final DcmChange change : dcmTracker.getChanges()) {
            System.out.println(StringUtils.colorizeForTerminal(change.getFormattedExplanation("    "), TerminalColor.GREEN));
        }

    }

}
