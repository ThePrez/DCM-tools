package com.github.ibmioss.dcmtools;

import java.io.File;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.utils.CertUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.FileUtils;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmChangePwCmd {

    public static void main(final String... _args) {
        final ExportOptions opts = new CertFileExporter.ExportOptions();
        opts.setDcmStore(null);
        opts.setPasswordProtected(true);
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--password=")) {
                opts.setPassword(DcmUserOpts.extractValue(arg));
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
            final File tmpFileOld = TempFileManager.createTempFile();
            FileUtils.delete(tmpFileOld);
            CertUtils.exportDcmStore(logger, opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword(), tmpFileOld.getAbsolutePath());
            // At this point, we've exported to a temp file with the temp file password. Import that into a new temp DCM store
            // .... and now we import that into a NEW temp file that has the new password
            final File tmpFileNew = TempFileManager.createTempFile();
            FileUtils.delete(tmpFileNew);
            try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
                caller.callQykmImportKeyStore(logger, tmpFileNew.getAbsolutePath(), new String(opts.getPasswordOrThrow()), tmpFileOld.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
            }

            // now, replace the original
            FileUtils.moveToWithBackup(tmpFileNew.getAbsolutePath(), opts.getDcmStore(), true);

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
        final String usage = "Usage: dcmchangepw  [options]\n"
                + "\n"
                + "    Valid options include:\n"
                + "        -y:                            Do not ask for confirmation\n"
                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                + "                                       to indicate the *SYSTEM store (default)\n"
                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                + "        --password[=password]:         Provide new password (not recommended)\n"
                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
