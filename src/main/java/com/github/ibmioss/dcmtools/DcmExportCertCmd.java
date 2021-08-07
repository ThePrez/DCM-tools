package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Encoder;

import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.KeyStoreLoader;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmExportCertCmd {
    private static class ExportCertOptions extends DcmUserOpts {
        public enum OutputFormat {
            DER, PEM
        }

        private OutputFormat m_format = null;
        private String m_label = null;

        public void setOutputFileFormat(OutputFormat _fmt) {
            m_format = _fmt;
        }

        public void setLabel(String _lbl) {
            m_label = _lbl;
        }

        public OutputFormat getFormat() throws IOException {
            if (null != m_format)
                return m_format;
            if (isYesMode()) {
                return m_format = OutputFormat.PEM;
            }

            final String resp = ConsoleUtils.askUserWithDefault("Whic output format, 'der' or 'pem'? (default: pem) ", "pem");

            try {
                return m_format = OutputFormat.valueOf(resp.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IOException("ERROR: output format '" + resp + "' is not supported");
            }
        }

        public String getLabel() throws IOException {
            if (null != m_label)
                return m_label;
            return m_label = ConsoleUtils.askUserOrThrow("Enter the certificate Id: ");
        }

    }

    public static void main(final String... _args) {
        String file = null;
        final ExportCertOptions opts = new ExportCertOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
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
            } else if (arg.startsWith("--format=")) {
                String formatStr = DcmUserOpts.extractValue(arg);
                try {
                    opts.setOutputFileFormat(ExportCertOptions.OutputFormat.valueOf(formatStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println("ERROR: output format '" + formatStr + "' is not supported");
                }
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
            File dcmStore = CertFileExporter.exportDcmStore(opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword(), null);
            KeyStoreLoader loader = new KeyStoreLoader(dcmStore.getAbsolutePath(), TempFileManager.TEMP_KEYSTORE_PWD, opts.getLabel(), false);
            KeyStore keyStore = loader.getKeyStore();
            Certificate cert = keyStore.getCertificate(opts.getLabel());

            if (ExportCertOptions.OutputFormat.PEM == opts.getFormat()) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                    Encoder encoder = Base64.getEncoder();
                    String base64 =encoder.encodeToString(cert.getEncoded());
                    bw.write("-----BEGIN CERTIFICATE-----\n");
                    int charsWritten = 0;
                    for (char b : base64.toCharArray()) {
                        bw.write(b);
                        charsWritten++;
                        if (charsWritten % 64 == 0)
                            bw.write("\n");
                    }
                    bw.write("\n-----END CERTIFICATE-----\n");
                }
            } else {
                if (!(cert instanceof X509Certificate)) {
                    throw new IOException("ERROR: Only x.509 certificates are supported for this operation");
                }
                byte[] der = ((X509Certificate) cert).getTBSCertificate();
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(der);
                }
            }
            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } catch (final Exception e) {
            System.err.println(StringUtils.colorizeForTerminal(e.getLocalizedMessage(), TerminalColor.BRIGHT_RED));
            e.printStackTrace();
            TempFileManager.cleanup();
            System.exit(-1); // TODO: allow skip on nonfatal
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
		final String usage = "Usage: dcmexportcert [options] <filename>\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                                + "                                       to indicate the *SYSTEM store (default)\n"
                                + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                                + "        --cert=<id>:                   ID of the certificate to export\n"
                                + "        --format=<format>:             Format of the output file (PEM/DER).\n"
                                +"                                        (default: PEM)\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
