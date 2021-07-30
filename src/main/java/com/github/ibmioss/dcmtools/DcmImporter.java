package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.CertFileImporter.ImportOptions;
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
public class DcmImporter {
    private static String extractValue(final String _str) {
        final int equalsSign = _str.indexOf('=');
        return (-1 == equalsSign) ? _str : _str.substring(1 + equalsSign);
    }

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
        final List<String> files = new LinkedList<String>();
        final List<String> fetchFroms = new LinkedList<String>();
        final ImportOptions opts = new ImportOptions();
        final boolean isImportingInstalledCerts;
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.isYesMode = true;
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--password=")) {
                opts.isPasswordProtected = true;
                opts.password = extractValue(arg);
            } else if ("--password".equals(arg)) {
                opts.isPasswordProtected = true;
            } else if (arg.startsWith("--target=")) {
                final String target = extractValue(arg);
                if ("system".equalsIgnoreCase(target) || "*system".equalsIgnoreCase(target)) {
                    opts.dcmTarget = CertFileImporter.SYSTEM_DCM_STORE;
                } else {
                    opts.dcmTarget = target;
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.dcmPassword = extractValue(arg);
            } else if (arg.startsWith("--fetch-from=")) {
                fetchFroms.add(extractValue(arg));
            } else if ("--installed-certs".equals(arg)) {
                files.add(null);
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                files.add(arg);
            }
        }
        try {// TODO: handle multi-file better
            for (final String fetchFrom : fetchFroms) {
                files.add(fetchCerts(opts.isYesMode, fetchFrom));
            }
            if (files.isEmpty()) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: no input files specified", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
            for (final String file : files) {
                final CertFileImporter off = new CertFileImporter(file);
                off.doImport(opts);
            }

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
		final String usage = "Usage: dcmimport  [options] [[filename] ..]\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                + "        --password[=password]:         Indicate that the input file is password-protected,\n"
                                + "                                       and optionally provide a password\n"
                                + "        --target=<system/filename>:    Specify the target keystore, or specify 'system'\n"
                                + "                                       to indicate the *SYSTEM store (default)\n"
                                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                                + "        --fetch-from=<hostname>[:port] Fetch the certificate from the given hostname/port\n"
                                + "        --installed-certs:             import all certificates that are installed into PASE\n"
                                + "                                       environment, for instance, certificates in the\n"
                                + "                                       ca-certificates-mozilla package\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
