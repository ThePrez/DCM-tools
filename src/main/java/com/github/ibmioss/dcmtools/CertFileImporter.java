package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.utils.CertUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.KeyStoreInterrogator;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;
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
                final String resp = ConsoleQuestionAsker.get().askUserForPwd("Enter input file password: ");
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

    public CertFileImporter(final AppLogger _logger, final List<String> _fileNames) throws IOException {
        m_fileNames = new LinkedList<String>();
        for (final String fileName : _fileNames) {
            m_fileNames.add(null == fileName ? KeyStoreLoader.extractTrustFromInstalledCerts(_logger) : fileName);
        }
    }

    public CertFileImporter(final AppLogger _logger, final String... _fileNames) throws IOException {
        this(_logger, Arrays.asList(_fileNames));
    }

    public void doImport(final AppLogger _logger, final ImportOptions _opts, final DcmChangeTracker _tracker)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode();
        // Initialize keystore from file of unknown type
        final KeyStore keyStore = new KeyStoreLoader(_logger, m_fileNames, _opts.getPasswordOrNull(), _opts.getLabel(), _opts.isCasOnly()).getKeyStore();
        _logger.println_success("Sanity check successful");

        final KeyStoreInterrogator dcmChecker = _tracker.getStartingSnapshot();

        // Check for conflicting aliases, where the certificate is already in the store under a different alias
        for (final String alias : Collections.list(keyStore.aliases())) {
            _logger.println("checking for conflicting cert to the one with alias " + alias);
            final Certificate cert = keyStore.getCertificate(alias);
            final String conflictingAlias = dcmChecker.getAliasOfCertOrNull(_logger, cert);
            if (null != conflictingAlias) {
                _logger.println_warn("WARNING: The following certificate already exists in the keystore with certificate id '" + conflictingAlias + "'. Certificate will not be imported:\n" + CertUtils.getCertInfoStr(cert, "    "));
                keyStore.deleteEntry(alias);
            }
        }
        // check for the case where the alias already exists and we might overwrite it on import
        for (final String alias : Collections.list(keyStore.aliases())) {
            _logger.println("checking cert at alias " + alias);
            final Certificate cert = keyStore.getCertificate(alias);
            final Certificate preExistingCert = dcmChecker.getKeyStore().getCertificate(alias);
            if (null != preExistingCert) {
                _logger.println(StringUtils.colorizeForTerminal("WARNING: The following already exists with certificate id '" + alias + "' and will not be imported. Perhaps you mean to use the 'dcmrenew' tool instead?:\n" + CertUtils.getCertInfoStr(preExistingCert, "    "), TerminalColor.YELLOW));
            }
        }
        if (!keyStore.aliases().hasMoreElements()) {
            throw new IOException("No certificates to import");
        }
        // Ask user confirmation
        _logger.println("The following certificates will be processed:");
        for (final String alias : Collections.list(keyStore.aliases())) {
            final Certificate cert = keyStore.getCertificate(alias);
            _logger.println("    Certificate ID '" + alias + "':");
            _logger.println(StringUtils.colorizeForTerminal(CertUtils.getCertInfoStr(cert, "        "), TerminalColor.CYAN));
        }
        final String reply = isYesMode ? "y" : ConsoleQuestionAsker.get().askUserWithDefault("Do you want to import ALL of the above certificates into DCM? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            throw new IOException("No certificates to import");
        }

        // Convert the KeyStore object to a file in the format needed by the DCM API
        final String dcmImportFile = new KeyStoreLoader(keyStore).saveToDcmApiFormatFile(TempFileManager.TEMP_KEYSTORE_PWD);
        ;

        // .... and... call the DCM API to do the import!
        try (DcmApiCaller caller = new DcmApiCaller(isYesMode)) {
            caller.callQykmImportKeyStore(_logger, _opts.getDcmStore(), _opts.getDcmPassword(), dcmImportFile, TempFileManager.TEMP_KEYSTORE_PWD);
        }
    }
}
