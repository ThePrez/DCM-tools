package com.github.ibmioss.dcmtools;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmCreateCmd {
    public static void main(final String... _args) {
        final DcmUserOpts opts = new DcmUserOpts();
        opts.setDcmStore(null);
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--dcm-store=")) {
                final String target = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(target) || "*system".equalsIgnoreCase(target)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(new File(target).getAbsolutePath());
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                if (StringUtils.isNonEmpty(opts.getDcmStoreNonInteractive())) {
                    System.err.println(StringUtils.colorizeForTerminal("ERROR: More than one file specified", TerminalColor.BRIGHT_RED));
                    printUsageAndExit();
                }
                if ("system".equalsIgnoreCase(arg) || "*system".equalsIgnoreCase(arg)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(new File(arg).getAbsolutePath());
                }
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {
            final String dcmStore = opts.getDcmStore().trim();
            if (StringUtils.isEmpty(dcmStore)) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: no input files specified", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
            logger.println("Creating DCM store at " + dcmStore);
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, opts.getDcmPassword().toCharArray());
            final File tmpFile = TempFileManager.createTempFile();
            try (final FileOutputStream fos = new FileOutputStream(tmpFile)) {
                ks.store(fos, TempFileManager.TEMP_KEYSTORE_PWD.toCharArray());
            }
            try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
                caller.callQykmImportKeyStore(logger, opts.getDcmStore(), opts.getDcmPassword(), tmpFile.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD);
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
		final String usage = "Usage: dcmcreate  [options]\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                                + "                                       to indicate the *SYSTEM store (default)\n"
                                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
