package com.github.ibmioss.dcmtools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;

import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

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
                return m_label = ConsoleQuestionAsker.get().askUserOrThrow("Enter label: ");
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
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
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
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {
            final DcmChangeTracker tracker = new DcmChangeTracker(logger, opts);

            KeyStore ks = KeyStore.getInstance("IBMi5OSKeyStore");
            try (FileInputStream fis = new FileInputStream(opts.getDcmStore())) {
                ks.load(fis, opts.getDcmPassword().toCharArray());
            }
            ks.deleteEntry(opts.getLabel());
            try (FileOutputStream fos = new FileOutputStream(opts.getDcmStore())) {
                ks.store(fos, opts.getDcmPassword().toCharArray());
            }
            tracker.printChanges(logger);
            logger.println_success("SUCCESS!!!");
        } catch (final Exception e) {
            logger.printExceptionStack_verbose(e);
            logger.println_err(e.getLocalizedMessage());
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
