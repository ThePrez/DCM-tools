package com.github.ibmioss.dcmtools;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.KeyStoreHelper;
import com.github.ibmioss.dcmtools.utils.MessageLookerUpper;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher.ProcessResult;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Bin4;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCodeParameter;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;

public class CertFileProcessor {
    public static class ImportOptions {
        public boolean isYesMode = false;
        public boolean isPasswordProtected = false;
        public String password = null;
        public String dcmPassword = null;
        public String dcmTarget;
    }

    private static final String TEMP_KEYSTORE_PWD = StringUtils.generateRandomString(10);
    static final String SYSTEM_DCM_STORE = "/QIBM/UserData/ICSS/Cert/Server/DEFAULT.KDB";

    private final String m_fileName;

    public CertFileProcessor(final String _fileName) throws IOException {
        m_fileName = null == _fileName ? KeyStoreHelper.extractTrustFromInstalledCerts() : _fileName;
    }

    public void doImport(final ImportOptions _opts) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode;
        // Initialize keystore from file of unknown type
        final KeyStore keyStore;
        if (_opts.isPasswordProtected) {
            if (StringUtils.isEmpty(_opts.password) && !isYesMode) {
                final String password = ConsoleUtils.askUserForPwd("Enter input file password: ");
                keyStore = new KeyStoreHelper(m_fileName, StringUtils.isEmpty(password) ? null : password).getKeyStore();
            } else {
                keyStore = new KeyStoreHelper(m_fileName, _opts.password).getKeyStore();
            }
        } else {
            keyStore = new KeyStoreHelper(m_fileName, null).getKeyStore();
        }
        System.out.println(StringUtils.colorizeForTerminal("Sanity check successful", TerminalColor.GREEN));

        // Ask user confirmation
        System.out.println("The following certificates will be processed:");
        for (final String alias : Collections.list(keyStore.aliases())) {
            final Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal(((X509Certificate) cert).getIssuerX500Principal().getName(X500Principal.RFC1779), TerminalColor.CYAN));
            } else {
                System.out.println("    " + alias + ": " + StringUtils.colorizeForTerminal("<unknown CN>", TerminalColor.BRIGHT_RED));
            }
        }
        final String reply = isYesMode ? "y" : ConsoleUtils.askUserWithDefault("Do you want to import ALL of the above certificates into DCM? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            System.out.println(StringUtils.colorizeForTerminal("OK, not importing", TerminalColor.YELLOW));
            return;
        }

        // Resolve the target DCM keystore
        final String dcmStore;
        if (StringUtils.isEmpty(_opts.dcmTarget)) {
            final String dcmStoreResp = isYesMode ? "*SYSTEM" : ConsoleUtils.askUserWithDefault("Target DCM keystore: (leave blank for *SYSTEM) ", "*SYSTEM");
            if ("*SYSTEM".equals(dcmStoreResp.trim().toUpperCase()) || "SYSTEM".equals(dcmStoreResp.trim().toUpperCase())) {
                dcmStore = SYSTEM_DCM_STORE;
            } else {
                dcmStore = dcmStoreResp;
            }
        } else {
            dcmStore = _opts.dcmTarget;
        }

        // Resolve the target DCM keystore password
        final String dcmStorePw;
        if (StringUtils.isEmpty(_opts.dcmPassword)) {
            if (isYesMode) {
                throw new IOException("DCM keystore password not specified");
            }
            dcmStorePw = ConsoleUtils.askUserForPwd("Enter DCM keystore password: ");
        } else {
            dcmStorePw = _opts.dcmPassword;
        }

        // Convert the KeyStore object to a file in the format needed by the DCM API
        final String dcmImportFile = new KeyStoreHelper(keyStore).saveToDcmApiFormatFile(TEMP_KEYSTORE_PWD);;

        // .... and... call the DCM API to do the import!
        try(DcmApiCaller caller = new DcmApiCaller(isYesMode)) {
            caller.callQykmImportKeyStore(dcmStore, dcmStorePw, dcmImportFile, TEMP_KEYSTORE_PWD);
        }
    }


}
