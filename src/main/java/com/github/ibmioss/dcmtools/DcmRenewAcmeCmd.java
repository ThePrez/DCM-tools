package com.github.ibmioss.dcmtools;

import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ProcessLauncher;
import com.github.theprez.jcmdutils.ProcessLauncher.ProcessResult;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

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
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                domains.add(arg);
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {
            if (domains.isEmpty()) {
                logger.println_err("ERROR: no domain specified");
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
                logger.println_err("ERROR: no new certificate generated");
            }
            final CertRenewer off = new CertRenewer(logger, files);
            off.doRenew(logger, opts);
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
