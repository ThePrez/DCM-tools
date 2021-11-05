package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.CertFileImporter.ImportOptions;
import com.github.ibmioss.dcmtools.utils.CertUtils;
import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.FileUtils;
import com.github.ibmioss.dcmtools.utils.KeyStoreInterrogator;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher.ProcessResult;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmRenameCertCmd {
    private static class CertRenameOpts extends DcmUserOpts {

        private String m_oldLabel;
        private String m_newLabel;

        public void setOldLabel(String _oldLabel) {
            m_oldLabel = _oldLabel;
        }

        public void setNewLabel(String _newLabel) {
            m_newLabel = _newLabel;
        }

        public String getNewLabel() throws IOException {

            if (StringUtils.isNonEmpty(m_newLabel)) {
                return m_newLabel;
            }
            if (!isYesMode()) {
                return m_newLabel = ConsoleUtils.askUserOrThrow("Enter new label: ");
            }
            throw new IOException("ERROR: new label is required");
        }
        public String getOldLabel() throws IOException {

            if (StringUtils.isNonEmpty(m_oldLabel)) {
                return m_oldLabel;
            }
            if (!isYesMode()) {
                return m_oldLabel = ConsoleUtils.askUserOrThrow("Enter old label: ");
            }
           throw new IOException("ERROR: old label is required");
        }
    }

    public static void main(final String... _args) {
        final CertRenameOpts opts = new CertRenameOpts();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--old-label=")) {
                opts.setOldLabel(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--new-label=")) {
                opts.setNewLabel(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--dcm-store=")) {
                final String target = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(target) || "*system".equalsIgnoreCase(target)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(target);
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
        }
        try {
            DcmChangeTracker tracker = new DcmChangeTracker(opts);
            KeyStoreInterrogator startingSnap = KeyStoreInterrogator.getFromDCM(opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword());
            KeyStore origKs = startingSnap.getKeyStore();
            final String oldLabel = opts.getOldLabel();
            Certificate cert = origKs.getCertificate(oldLabel);
            origKs.deleteEntry(oldLabel);
            origKs.setCertificateEntry(opts.getNewLabel(), cert);
            // At this point, we have a KeyStore object with the change made. Now, let's write it to a temp file
            File tmpFile = TempFileManager.createTempFile();
            try(FileOutputStream fos = new FileOutputStream(tmpFile)) {
                origKs.store(fos, TempFileManager.TEMP_KEYSTORE_PWD.toCharArray());
            }
            // Next, need to save it to .kdb format
            File tmpFileKdb = TempFileManager.createTempFile();
            FileUtils.delete(tmpFileKdb);
            try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
                caller.callQykmImportKeyStore(tmpFileKdb.getAbsolutePath(), new String(opts.getDcmPassword()), tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
            }

            // now, replace the original
            FileUtils.moveToWithBackup(tmpFileKdb.getAbsolutePath(), opts.getDcmStore(), true);
            tracker.printChanges();

            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println(StringUtils.colorizeForTerminal(e.getLocalizedMessage(), TerminalColor.BRIGHT_RED));
            TempFileManager.cleanup();
            System.exit(-1);
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
        final String usage = "Usage: dcmrenamecert  [options]\n"
                + "\n"
                + "    Valid options include:\n"
                + "        -y:                            Do not ask for confirmation\n"
                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                + "                                       to indicate the *SYSTEM store (default)\n"
                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                + "        --old-label=<label>:           Label of the certificate to rename\n"
                + "        --new-label=<label>:           Label of the certificate to rename\n"
                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}