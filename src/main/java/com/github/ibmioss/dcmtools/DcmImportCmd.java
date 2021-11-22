package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.CertFileImporter.ImportOptions;
import com.github.ibmioss.dcmtools.utils.DcmChangeTracker;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.ProcessLauncher;
import com.github.theprez.jcmdutils.ProcessLauncher.ProcessResult;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmImportCmd {

    private static String fetchCerts(final AppLogger _logger, final boolean _isYesMode, final String _fetchFrom) throws IOException {
        final ProcessResult cmdResults = ProcessLauncher.exec("openssl s_client -connect " + _fetchFrom + " -showcerts");
        if (0 != cmdResults.getExitStatus()) {
            for (final String errLine : cmdResults.getStderr()) {
                _logger.println_err(errLine);
            }
            throw new IOException("Error extracting trusted certificates");
        }
        boolean isCertificateFetched = false;
        for (final String line : cmdResults.getStdout()) {
            if (line.contains("END CERTIFICATE")) {
                isCertificateFetched = true;
            }
            if (!_isYesMode) {
                _logger.println(StringUtils.colorizeForTerminal(line, TerminalColor.CYAN));
            }
        }
        if (!isCertificateFetched) {
            for (final String errLine : cmdResults.getStderr()) {
                _logger.println_err(errLine);
            }
            throw new IOException("Error extracting trusted certificates");
        }
        final String reply = _isYesMode ? "y" : ConsoleQuestionAsker.get().askUserWithDefault("Do you trust the certificate(s) listed above? [y/N] ", "N");
        if (!reply.toLowerCase().trim().startsWith("y")) {
            throw new IOException("User Canceled");
        }
        final File destFile = TempFileManager.createTempFile(_fetchFrom + ".pem");
        boolean isCertLine = false;
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile, true), "UTF-8"))) {
            for (final String line : cmdResults.getStdout()) {
                if (line.contains("BEGIN CERTIFICATE")) {
                    isCertLine = true;
                }
                if (isCertLine) {
                    bw.write(line);
                    bw.write("\n");
                }
                if (line.contains("END CERTIFICATE")) {
                    isCertLine = false;
                }
            }
            bw.write("\n");
        }
        return destFile.getAbsolutePath();
    }

    public static void main(final String... _args) {
        final List<String> files = new LinkedList<String>();
        final List<String> fetchFroms = new LinkedList<String>();
        final ImportOptions opts = new ImportOptions();
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
                final String target = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(target) || "*system".equalsIgnoreCase(target)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(target);
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--cert=")) {
                opts.setLabel(DcmUserOpts.extractValue(arg));
            } else if ("--ca-only".equals(arg)) {
                opts.setCasOnly(true);
            } else if (arg.startsWith("--fetch-from=")) {
                String fetchFrom = DcmUserOpts.extractValue(arg);
                if (!fetchFrom.contains(":")) {
                    fetchFrom += ":443";
                }
                fetchFroms.add(fetchFrom);
            } else if ("--installed-certs".equals(arg)) {
                files.add(null);
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                files.add(arg);
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {// TODO: handle multi-file better
            if (!files.isEmpty() && !fetchFroms.isEmpty()) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Cannot specify file(s) when using '--fetch-from'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
            for (final String fetchFrom : fetchFroms) {
                files.add(fetchCerts(logger, opts.isYesMode(), fetchFrom));
            }
            if (files.isEmpty()) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: no input files specified", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }

            final DcmChangeTracker dcmTracker = new DcmChangeTracker(logger, opts);
            final CertFileImporter off = new CertFileImporter(logger, files);
            off.doImport(logger, opts, dcmTracker);
            dcmTracker.printChanges(logger);
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
		final String usage = "Usage: dcmimport  [options] [[filename] ..]\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                + "        --password[=password]:         Indicate that the input file is password-protected,\n"
                                + "                                       and optionally provide a password\n"
                                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                                + "                                       to indicate the *SYSTEM store (default)\n"
                                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                                + "        --fetch-from=<hostname>[:port] Fetch CA certificate(s) from the given hostname/port\n"
                                + "        --ca-only                      Only import CA Certificates\n"
                                + "        --cert=<id>                    Recommend a certificate ID when imported into DCM\n"
                                + "        --installed-certs:             import all certificates that are installed into PASE\n"
                                + "                                       environment, for instance, certificates in the\n"
                                + "                                       ca-certificates-mozilla package\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
