package com.github.ibmioss.dcmtools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.security.auth.x500.X500Principal;

import com.github.ibmioss.dcmtools.utils.CertUtils;
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
            final KeyStore sourceKs = CertUtils.exportDcmStoreToKeystoreObj(logger, opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword());
            final KeyStore destKs = KeyStore.getInstance(StringUtils.isEmpty(opts.outputFileFormat) ? "pkcs12" : opts.outputFileFormat);
            destKs.load(null, null);
            for (final String alias : Collections.list(sourceKs.aliases())) {
                final Certificate cert = sourceKs.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    logger.println("    " + alias + ": " + StringUtils.colorizeForTerminal(((X509Certificate) cert).getIssuerX500Principal().getName(X500Principal.RFC1779), TerminalColor.CYAN));
                } else {
                    logger.println_err("    " + alias + ": " + StringUtils.colorizeForTerminal("<unknown CN>", TerminalColor.BRIGHT_RED));
                }
                destKs.setCertificateEntry(alias, cert);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                destKs.store(out, opts.getPasswordOrThrow());
            }
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

    public static class ExportOptions extends DcmUserOpts {
        public boolean isPasswordProtected = false;
        public String outputFileFormat = null;
        public char[] password = null;

        public String getOutputFileFormat() {
            return outputFileFormat;
        }

        public char[] getPasswordOrNull() throws IOException {
            if (!isPasswordProtected) {
                return null;
            }
            if (StringUtils.isEmpty(password) && !isYesMode()) {
                final String resp = ConsoleQuestionAsker.get().askUserForPwd("Enter output file password: ");
                return password = resp.toCharArray();
            } else {
                return password;
            }
        }

        public char[] getPasswordOrThrow() throws IOException {
            if (null != password) {
                return password;
            }
            if (StringUtils.isEmpty(password) && !isYesMode()) {
                final String resp = ConsoleQuestionAsker.get().askUserForPwd("Enter output file password: ");
                return password = resp.toCharArray();
            }
            throw new IOException("ERROR: Password is required");
        }

        public boolean isPasswordProtected() {
            return isPasswordProtected;
        }

        public void setOutputFileFormat(final String outputFileFormat) {
            this.outputFileFormat = outputFileFormat;
        }

        public void setPassword(final String password) {
            this.password = password.toCharArray();
        }

        public void setPasswordProtected(final boolean isPasswordProtected) {
            this.isPasswordProtected = isPasswordProtected;
        }
    }
}
