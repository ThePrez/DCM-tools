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
import com.github.ibmioss.dcmtools.utils.MessageLookerUpper;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
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

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cleanup();
            }
        });
    }

    private static final String PKCS_12 = "PKCS12";
    private static final String TEMP_KEYSTORE_PWD = StringUtils.generateRandomString(10);
    static final String SYSTEM_DCM_STORE = "/QIBM/UserData/ICSS/Cert/Server/DEFAULT.KDB";
    private static final List<File> s_filesToCleanup = new LinkedList<File>();

    public static void cleanup() {
        for (final File f : s_filesToCleanup) {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    private static File createTempFile(final String _fileName) throws IOException {
        final File dotDir = new File(System.getProperty("user.home", "~"), ".dcmimport");
        dotDir.mkdirs();
        final File ret;
        if (null == _fileName) {
            ret = File.createTempFile(".dcmimport", ".file", dotDir);
        } else {
            ret = new File(dotDir, _fileName);
            ret.createNewFile();
        }
        ret.deleteOnExit();
        s_filesToCleanup.add(ret);
        return ret;
    }

    private static String extractTrustFromInstalledCerts() throws IOException {
        final File destFile = createTempFile(null);
        destFile.delete();
        final Process p = Runtime.getRuntime().exec("/QOpenSys/pkgs/bin/trust extract --format=java-cacerts --purpose=server-auth -v " + destFile.getAbsolutePath());
        final SimpleEntry<List<String>, List<String>> output = getStdoutAndStderr(p);
        final List<String> stdout = output.getKey();
        final List<String> stderr = output.getValue();
        int rc;
        try {
            rc = p.waitFor();
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }
        if (0 != rc) {
            for (final String errLine : stderr) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        System.out.println(StringUtils.colorizeForTerminal("Successfully extracted installed certificates", TerminalColor.GREEN));
        return destFile.getAbsolutePath(); // TODO: delete this file!
    }

    public static SimpleEntry<List<String>, List<String>> getStdoutAndStderr(final Process _p) throws UnsupportedEncodingException, IOException {
        final List<String> stdout = new LinkedList<String>();
        final List<String> stderr = new LinkedList<String>();
        final Thread stderrThread = new Thread() {
            @Override
            public void run() {

                try (BufferedReader br = new BufferedReader(new InputStreamReader(_p.getErrorStream(), "UTF-8"))) {
                    String line;
                    while (null != (line = br.readLine())) {
                        stderr.add(line);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            };
        };
        stderrThread.setDaemon(true);
        stderrThread.start();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(_p.getInputStream(), "UTF-8"))) {
            String line;
            while (null != (line = br.readLine())) {
                stdout.add(line);
            }
        }
        return new AbstractMap.SimpleEntry<List<String>, List<String>>(stdout, stderr);
    }

    private static KeyStore initializeKeyStoreObj(final String _file, final String _pw) throws IOException {
        // Try to load as keystore file
        final String[] keystoreTypes = new String[] { KeyStore.getDefaultType(), "JKS", "pkcs12", "jceks" };
        KeyStore loaded = null;
        for (final String keystoreType : keystoreTypes) {
            try (FileInputStream fis = new FileInputStream(_file)) {
                final KeyStore keyStore = KeyStore.getInstance(keystoreType);
                keyStore.load(fis, null == _pw ? null : _pw.toCharArray());
                loaded = keyStore;
                break;
            } catch (final Throwable e) {
            }
        }

        // That didn't work! Try to load as a certificate file
        if (null == loaded) {
            try (FileInputStream fis = new FileInputStream(_file)) {
                final Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(fis);
                if (!certs.isEmpty()) {
                    final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    int counter = 1;
                    for (final Certificate cert : certs) {
                        final String alias = new File(_file).getName().replaceFirst("[.][^.]+$", "") + "." + (counter++);
                        keyStore.setCertificateEntry(alias, cert);
                    }
                    loaded = keyStore;
                }
            } catch (final Throwable e) {
            }
        }

        // Out of ideas
        if (null == loaded) {
            throw new IOException("Couldn't load certificates from file");
        }
        System.out.println(StringUtils.colorizeForTerminal("Successfully loaded certificates", TerminalColor.GREEN));
        return loaded;
    }

    private static String saveToDcmApiFormatFile(final KeyStore _keyStore) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final File dcmFile = createTempFile(null);
        final KeyStore tgt = KeyStore.getInstance(PKCS_12);
        tgt.load(null, TEMP_KEYSTORE_PWD.toCharArray());
        for (final String alias : Collections.list(_keyStore.aliases())) {
            tgt.setCertificateEntry(alias, _keyStore.getCertificate(alias));
        }
        try (FileOutputStream fos = new FileOutputStream(dcmFile, true)) {
            tgt.store(fos, TEMP_KEYSTORE_PWD.toCharArray());
        }
        return dcmFile.getAbsolutePath();
    }

    private final String m_fileName;

    public CertFileProcessor(final String _fileName) throws IOException {
        m_fileName = null == _fileName ? extractTrustFromInstalledCerts() : _fileName;
    }

    public void doImport(final ImportOptions _opts) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {

        final boolean isYesMode = _opts.isYesMode;
        // Initialize keystore
        final KeyStore keyStore;
        if (_opts.isPasswordProtected) {
            if (StringUtils.isEmpty(_opts.password) && !isYesMode) {
                final String password = ConsoleUtils.askUserForPwd("Enter input file password: ");
                keyStore = initializeKeyStoreObj(m_fileName, StringUtils.isEmpty(password) ? null : password);
            } else {
                keyStore = initializeKeyStoreObj(m_fileName, _opts.password);
            }
        } else {
            keyStore = initializeKeyStoreObj(m_fileName, null);
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
        final String dcmImportFile = saveToDcmApiFormatFile(keyStore);
        final AS400 as400;
        final String osName = System.getProperty("os.name", "");
        if (osName.equalsIgnoreCase("OS/400")) { // Running on IBM i, using JV1
            as400 = new AS400("localhost", "*CURRENT", "*CURRENT");
        } else if (osName.equalsIgnoreCase("OS400")) { // Running on i, OpenJDK
            if (isYesMode) {
                throw new IOException("IBM i password not specified. Run in interactive mode or with JV1 Java for this to work.");
            }
            as400 = new AS400("localhost", System.getProperty("user.name", "*CURRENT"), ConsoleUtils.askUserOrThrow("Enter IBM i password: "));
        } else {
            if (isYesMode) {
                throw new IOException("Not allowed with '-y'");
            }
            as400 = new AS400(ConsoleUtils.askUserOrThrow("Enter IBM i system name: "), ConsoleUtils.askUserOrThrow("Enter IBM i user name: "), ConsoleUtils.askUserForPwd("Enter IBM i password: "));
        }

        // .... and... call the DCM API to do the import!
        final ProgramCall program = new ProgramCall(as400);
        try {
            // Initialize the name of the program to run.
            final String programName = "/QSYS.LIB/QYKMIMPK.PGM";
            // Set up the parms
            final ProgramParameter[] parameterList = new ProgramParameter[14];
            // 1 Certificate store path and file Name Input Char(*)
            parameterList[0] = new ProgramParameter(new AS400Text(dcmStore.length()).toBytes(dcmStore));
            // 2 Length of certificate store path and file Name Input Binary(4)
            parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(dcmStore.length()));
            // 3 Format of certificate store path and file Name Input Char(8)
            parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
            // 4 Certificate store password Input Char(*)
            parameterList[3] = new ProgramParameter(new AS400Text(dcmStorePw.length(), 1208).toBytes(dcmStorePw));
            // 5 Length of certificate store password Input Binary(4)
            parameterList[4] = new ProgramParameter(new AS400Bin4().toBytes(dcmStorePw.length()));
            // 6 CCSID of certificate store password Input Binary(4)
            parameterList[5] = new ProgramParameter(new AS400Bin4().toBytes(1208));
            // 7 Import path and file name Input Char(*)
            parameterList[6] = new ProgramParameter(new AS400Text(dcmImportFile.length()).toBytes(dcmImportFile));
            // 8 Length of import path and file name Input Binary(4)
            parameterList[7] = new ProgramParameter(new AS400Bin4().toBytes(dcmImportFile.length()));
            // 9 Format of import path and file name Input Char(8)
            parameterList[8] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
            // 10 Version of import file Input Char(10)
            parameterList[9] = new ProgramParameter(new AS400Text(10).toBytes("*PKCS12V3 "));
            // 11 Import file password Input Char(*)
            parameterList[10] = new ProgramParameter(new AS400Text(TEMP_KEYSTORE_PWD.length(), 1208).toBytes(TEMP_KEYSTORE_PWD));
            // 12 Length of import file password Input Binary(4)
            parameterList[11] = new ProgramParameter(new AS400Bin4().toBytes(TEMP_KEYSTORE_PWD.length()));
            // 13 CCSID of import file password Input Binary(4)
            parameterList[12] = new ProgramParameter(new AS400Bin4().toBytes(1208));
            // 14 Error code I/O Char(*)
            final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
            parameterList[13] = ec;

            program.setProgram(programName, parameterList);
            // Run the program.
            if (!program.run()) {
                for (final AS400Message msg : program.getMessageList()) {
                    // Show each message.
                    System.err.println(StringUtils.colorizeForTerminal("" + msg, TerminalColor.BRIGHT_RED));
                }
                throw new IOException("DCM API Call failure");
            }
            final String errorMessageId = ec.getMessageID();
            for (final AS400Message msg : program.getMessageList()) {
                // Show each message.
                System.out.println(StringUtils.colorizeForTerminal("" + msg, TerminalColor.CYAN));
            }
            if (!StringUtils.isEmpty(errorMessageId)) {
                throw new IOException("API gave error message " + new MessageLookerUpper(errorMessageId.trim()));
            }
            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } finally {
            as400.disconnectAllServices();
        }
    }

    public static String fetchCert(final ImportOptions _opts, final String _fetchFrom) throws IOException {
        final Process p = Runtime.getRuntime().exec("openssl s_client -connect " + _fetchFrom + " -showcerts");
        p.getOutputStream().close();
        final SimpleEntry<List<String>, List<String>> output = getStdoutAndStderr(p);
        final List<String> stdout = output.getKey();
        final List<String> stderr = output.getValue();
        int rc;
        try {
            rc = p.waitFor();
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }
        if (0 != rc) {
            for (final String errLine : stderr) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        boolean isCertificateFetched = false;
        for (final String line : stdout) {
            if (line.contains("END CERTIFICATE")) {
                isCertificateFetched = true;
            }
            System.out.println(StringUtils.colorizeForTerminal(line, TerminalColor.CYAN));
        }
        if (!isCertificateFetched) {
            for (final String errLine : stderr) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        final String reply = _opts.isYesMode ? "y" : ConsoleUtils.askUserWithDefault("Do you trust the certificate(s) listed above? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            throw new IOException("User Canceled");
        }
        final File destFile = createTempFile(_fetchFrom + ".pem");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile, true), "UTF-8"))) {
            for (final String line : stdout) {
                bw.write(line);
                bw.write("\n");
            }
        }
        return destFile.getAbsolutePath();
    }
}
