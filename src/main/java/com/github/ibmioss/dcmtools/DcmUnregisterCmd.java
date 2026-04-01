package com.github.ibmioss.dcmtools;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Unregister an application definition from DCM.
 */
public class DcmUnregisterCmd {

    private static class UnregisterOptions extends DcmUserOpts {
        private String appId = null;

        String getAppId() throws java.io.IOException {
            if (StringUtils.isNonEmpty(appId)) {
                return appId;
            }
            if (!isYesMode()) {
                return appId = ConsoleQuestionAsker.get().askUserOrThrow("Enter application ID: ");
            }
            throw new java.io.IOException("ERROR: Application ID is required");
        }

        void setAppId(final String _id) { appId = _id; }
    }

    public static void main(final String... _args) {
        final UnregisterOptions opts = new UnregisterOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--app=")) {
                opts.setAppId(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                opts.setAppId(arg);
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
            final String appId = opts.getAppId();
            if (!opts.isYesMode()) {
                final String confirm = ConsoleQuestionAsker.get().askUserWithDefault(
                        "Unregister application '" + appId + "'? [y/N] ", "N");
                if (!confirm.trim().toLowerCase().startsWith("y")) {
                    logger.println("Cancelled.");
                    return;
                }
            }
            logger.println("Unregistering application '" + appId + "'...");
            caller.callQycdUnregisterApplication(logger, appId);
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
        final String usage = "Usage: dcmunregister [options] [application_id]\n"
                           + "\n"
                           + "    Unregisters an application definition from DCM.\n"
                           + "\n"
                           + "    Valid options include:\n"
                           + "        -y:           Do not ask for confirmation\n"
                           + "        --app=<id>:   Application ID to unregister\n"
                           ;
        // @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
