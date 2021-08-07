package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
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
public class DcmExportCmd {

    private static String fetchCerts(final boolean _isYesMode, final String _fetchFrom) throws IOException {
        final ProcessResult cmdResults = ProcessLauncher.exec("openssl s_client -connect " + _fetchFrom + " -showcerts");
        if (0 != cmdResults.getExitStatus()) {
            for (final String errLine : cmdResults.getStderr()) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        boolean isCertificateFetched = false;
        for (final String line : cmdResults.getStdout()) {
            if (line.contains("END CERTIFICATE")) {
                isCertificateFetched = true;
            }
            System.out.println(StringUtils.colorizeForTerminal(line, TerminalColor.CYAN));
        }
        if (!isCertificateFetched) {
            for (final String errLine : cmdResults.getStderr()) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        final String reply = _isYesMode ? "y" : ConsoleUtils.askUserWithDefault("Do you trust the certificate(s) listed above? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            throw new IOException("User Canceled");
        }
        final File destFile = TempFileManager.createTempFile(_fetchFrom + ".pem");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile, true), "UTF-8"))) {
            for (final String line : cmdResults.getStdout()) {
                bw.write(line);
                bw.write("\n");
            }
        }
        return destFile.getAbsolutePath();
    }

    public static void main(final String... _args) {
        String file = null;
        final ExportOptions opts = new ExportOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--password=")) {
                opts.setPasswordProtected(true);
                opts.setPassword(DcmUserOpts.extractValue(arg));
            } else if ("--password".equals(arg)) {
                opts.setPasswordProtected(true);
            } else if (arg.startsWith("--source=")) {
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
        if (null == file) {
            System.err.println(StringUtils.colorizeForTerminal("ERROR: target file not specified", TerminalColor.BRIGHT_RED));
            printUsageAndExit();
        }
        try {
            new CertFileExporter(file).doExport(opts);
            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } catch (final Exception e) {
            System.err.println(StringUtils.colorizeForTerminal(e.getLocalizedMessage(), TerminalColor.BRIGHT_RED));
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
                                + "        --source=<system/filename>:    Specify the target keystore, or specify 'system'\n"
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
