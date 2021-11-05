package com.github.ibmioss.dcmtools;

import java.util.LinkedList;
import java.util.List;

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
public class DcmRenewAcmeCmd {

    public static void main(final String... _args) {
        final List<String> domains = new LinkedList<String>();
        final DcmUserOpts opts = new DcmUserOpts() {
        };
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                domains.add(arg);
            }
        }
        try {
            if (domains.isEmpty()) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: no domain specified", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
            final List<String> files = new LinkedList<String>();
            for (final String domain : domains) {
                final ProcessResult result = ProcessLauncher.exec("/QOpenSys/pkgs/bin/acme-client -F " + domain);
                result.prettyPrint();
                if (0 == result.getExitStatus()) {
                    files.add("");
                }
            }
            if (files.isEmpty()) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: no new certificate generated", TerminalColor.BRIGHT_RED));
            }
            final CertRenewer off = new CertRenewer(files);
            off.doRenew(opts);
            System.out.println(StringUtils.colorizeForTerminal("SUCCESS!!!", TerminalColor.GREEN));
        } catch (final Exception e) {
            System.err.println(StringUtils.colorizeForTerminal(e.getLocalizedMessage(), TerminalColor.BRIGHT_RED));
            TempFileManager.cleanup();
            System.exit(-1);
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
		final String usage = "Usage: dcmrenewacme [[domain] ..]\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                            Do not ask for confirmation\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
