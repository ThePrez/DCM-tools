package com.github.ibmioss.dcmtools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import com.github.ibmioss.dcmtools.DcmExportCmd.ExportOptions;
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
        final ExportOptions opts = new DcmExportCmd.ExportOptions();
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
            

            KeyStore ks = KeyStore.getInstance("IBMi5OSKeyStore");
            try (FileInputStream fis = new FileInputStream(opts.getDcmStore())) {
                ks.load(fis, opts.getDcmPassword().toCharArray());
            }
            try (FileOutputStream fos = new FileOutputStream(opts.getDcmStore())) {
                ks.store(fos, opts.getPasswordOrThrow());
            }

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
