package com.github.ibmioss.dcmtools;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmExportCmd {

    public static void main(final String... _args) {
        String file = null;
        final ExportOptions opts = new ExportOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--password=")) {
                opts.setPasswordProtected(true);
                opts.setPassword(DcmUserOpts.extractValue(arg));
            } else if ("--password".equals(arg)) {
                opts.setPasswordProtected(true);
            } else if (arg.startsWith("--dcm-store=")) {
                final String source = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(source) || "*system".equalsIgnoreCase(source)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(source);
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--format=")) {
                opts.setOutputFileFormat(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                if (null != file) {
                    System.err.println(StringUtils.colorizeForTerminal("ERROR: target file specified more than once", TerminalColor.BRIGHT_RED));
                    printUsageAndExit();
                }
                file = arg;
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        if (null == file) {
            System.err.println(StringUtils.colorizeForTerminal("ERROR: target file not specified", TerminalColor.BRIGHT_RED));
            printUsageAndExit();
        }
        try {
            new CertFileExporter(file).doExport(logger, opts);
            logger.println_success("SUCCESS!!!");
        } catch (final Exception e) {
            logger.printExceptionStack_verbose(e);
            logger.println_err(e.getLocalizedMessage());
            e.printStackTrace();
            TempFileManager.cleanup();
            System.exit(-1); // TODO: allow skip on nonfatal
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
		final String usage = "Usage: dcmexport <filename>\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                + "        --password[=password]:         Indicate that the output file is password-protected,\n"
                                + "                                       and optionally provide a password\n"
                                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                                + "                                       to indicate the *SYSTEM store (default)\n"
                                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                                + "        --format=<format>              Format of the output file (jceks, pks, pkcs12).\n"
                                +"                                        (default: pkcs12)\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
