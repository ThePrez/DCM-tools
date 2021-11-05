package com.github.ibmioss.dcmtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;

import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.FileUtils;
import com.github.ibmioss.dcmtools.utils.KeyStoreInterrogator;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmRemoveCertCmd {
    private static class CertRemoveOpts extends DcmUserOpts {

        private String m_label = null;

        public String getLabel() throws IOException {

            if (StringUtils.isNonEmpty(m_label)) {
                return m_label;
            }
            if (!isYesMode()) {
                return m_label = ConsoleUtils.askUserOrThrow("Enter label: ");
            }
            throw new IOException("ERROR: label is required");
        }

        public void setLabel(final String _label) {
            m_label = _label;
        }
    }

    public static void main(final String... _args) {
        final CertRemoveOpts opts = new CertRemoveOpts();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--label=")) {
                opts.setLabel(DcmUserOpts.extractValue(arg));
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
            final DcmChangeTracker tracker = new DcmChangeTracker(opts);
            final KeyStoreInterrogator startingSnap = KeyStoreInterrogator.getFromDCM(opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword());
            final KeyStore origKs = startingSnap.getKeyStore();
            origKs.deleteEntry(opts.getLabel());

            // At this point, we have a KeyStore object with the change made. Now, let's write it to a temp file
            final File tmpFile = TempFileManager.createTempFile();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                origKs.store(fos, TempFileManager.TEMP_KEYSTORE_PWD.toCharArray());
            }
            // Next, need to save it to .kdb format
            final File tmpFileKdb = TempFileManager.createTempFile();
            FileUtils.delete(tmpFileKdb);
            try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
                caller.callQykmImportKeyStore(tmpFileKdb.getAbsolutePath(), new String(opts.getDcmPassword()), tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
            }

            // now, replace the original
            FileUtils.moveToWithBackup(tmpFileKdb.getAbsolutePath(), opts.getDcmStore(), true);
            tracker.printChanges();

            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } catch (final Exception e) {
            System.err.println(StringUtils.colorizeForTerminal(e.getLocalizedMessage(), TerminalColor.BRIGHT_RED));
            TempFileManager.cleanup();
            System.exit(-1);
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
        final String usage = "Usage: dcmemovecert  [options]\n"
                + "\n"
                + "    Valid options include:\n"
                + "        -y:                            Do not ask for confirmation\n"
                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                + "                                       to indicate the *SYSTEM store (default)\n"
                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                + "        --label=<label>:               Label of the certificate to remove\n"
                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
