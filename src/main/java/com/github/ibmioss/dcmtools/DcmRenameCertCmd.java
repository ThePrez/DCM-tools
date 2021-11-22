package com.github.ibmioss.dcmtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.FileUtils;
import com.github.ibmioss.dcmtools.utils.KeyStoreInterrogator;
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
public class DcmRenameCertCmd {
    private static class CertRenameOpts extends DcmUserOpts {

        private String m_newLabel;
        private String m_oldLabel;

        public String getNewLabel() throws IOException {

            if (StringUtils.isNonEmpty(m_newLabel)) {
                return m_newLabel;
            }
            if (!isYesMode()) {
                return m_newLabel = ConsoleQuestionAsker.get().askUserOrThrow("Enter new label: ");
            }
            throw new IOException("ERROR: new label is required");
        }

        public String getOldLabel() throws IOException {

            if (StringUtils.isNonEmpty(m_oldLabel)) {
                return m_oldLabel;
            }
            if (!isYesMode()) {
                return m_oldLabel = ConsoleQuestionAsker.get().askUserOrThrow("Enter old label: ");
            }
            throw new IOException("ERROR: old label is required");
        }

        public void setNewLabel(final String _newLabel) {
            m_newLabel = _newLabel;
        }

        public void setOldLabel(final String _oldLabel) {
            m_oldLabel = _oldLabel;
        }
    }

    public static void main(final String... _args) {
        final CertRenameOpts opts = new CertRenameOpts();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
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
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {
            final DcmChangeTracker tracker = new DcmChangeTracker(logger, opts);
            final KeyStoreInterrogator startingSnap = KeyStoreInterrogator.getFromDCM(logger, opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword());
            final KeyStore origKs = startingSnap.getKeyStore();
            final String oldLabel = opts.getOldLabel();
            final Certificate cert = origKs.getCertificate(oldLabel);
            origKs.deleteEntry(oldLabel);
            origKs.setCertificateEntry(opts.getNewLabel(), cert);
            // At this point, we have a KeyStore object with the change made. Now, let's write it to a temp file
            final File tmpFile = TempFileManager.createTempFile();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                origKs.store(fos, TempFileManager.TEMP_KEYSTORE_PWD.toCharArray());
            }
            // Next, need to save it to .kdb format
            final File tmpFileKdb = TempFileManager.createTempFile();
            FileUtils.delete(tmpFileKdb);
            try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
                caller.callQykmImportKeyStore(logger, tmpFileKdb.getAbsolutePath(), new String(opts.getDcmPassword()), tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
            }

            // now, replace the original
            FileUtils.moveToWithBackup(tmpFileKdb.getAbsolutePath(), opts.getDcmStore(), true);
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
