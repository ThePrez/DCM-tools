package com.github.ibmioss.dcmtools;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Collections;

import com.github.ibmioss.dcmtools.utils.CertUtils;
import com.github.ibmioss.dcmtools.utils.FileUtils;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmViewCmd {

    public static void main(final String... _args) {
        final DcmUserOpts opts = new DcmUserOpts();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--dcm-store=")) {
                final String source = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(source) || "*system".equalsIgnoreCase(source)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(source);
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
        }
        try {
            final File tmpFile = TempFileManager.createTempFile();
            FileUtils.delete(tmpFile);
            CertUtils.exportDcmStore(opts.isYesMode(), opts.getDcmStore(), opts.getDcmPassword(), tmpFile.getAbsolutePath());
            KeyStore fileKs = null;
            try (FileInputStream fis = new FileInputStream(tmpFile)) {
                fileKs = KeyStore.getInstance("pkcs12");
                fileKs.load(fis, TempFileManager.TEMP_KEYSTORE_PWD.toCharArray());
            }
            for (final String label : Collections.list(fileKs.aliases())) {
                System.out.println("label '" + label + "'");
                System.out.println(StringUtils.colorizeForTerminal(CertUtils.getCertInfoStr(fileKs.getCertificate(label), "    "), TerminalColor.CYAN));
            }
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
		final String usage = "Usage: dcmview [options]\n"
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
