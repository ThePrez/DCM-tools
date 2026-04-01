package com.github.ibmioss.dcmtools;

import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller.AppCertInfo;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * List DCM-registered applications and their assigned certificates.
 * When no application IDs are given, lists the common well-known IBM i applications.
 */
public class DcmListAppsCmd {

    private static final String[] s_commonApplications = {
        "QIBM_OS400_QZBS_SVR_CENTRAL",
        "QIBM_OS400_QZBS_SVR_DATABASE",
        "QIBM_OS400_QZBS_SVR_DTAQ",
        "QIBM_OS400_QZBS_SVR_NETPRT",
        "QIBM_OS400_QZBS_SVR_RMTCMD",
        "QIBM_OS400_QZBS_SVR_SIGNON",
        "QIBM_OS400_QZBS_SVR_FILE",
        "QIBM_OS400_QRW_SVR_DDM_DRDA",
        "QIBM_OS400_QRW_CLT_DDM_DRDA",
        "QIBM_QTV_TELNET_SERVER",
        "QIBM_QTV_TELNET_CLIENT",
        "QIBM_QCST_CLUSTER_SECURITY",
        "QIBM_GLD_DIRSRV_SERVER",
        "QIBM_GLD_DIRSRV_PUBLISHING",
        "QIBM_GLD_DIRSRV_CLIENT",
        "QIBM_QTOK_VPN_KEYMGR",
        "QIBM_QSY_EIM_CLIENT",
        "QIBM_QSM_SERVICE",
        "QIBM_QP0A_FILESYS_CLIENT",
        "QIBM_QJO_RMT_JRN_TGT",
        "QIBM_QJO_RMT_JRN_SRC",
        "QIBM_QTMS_SMTP_SERVER",
        "QIBM_QTMS_SMTP_CLIENT",
        "QIBM_QTMF_FTP_SERVER",
        "QIBM_QTMF_FTP_CLIENT",
        "QIBM_QTMM_POP_SERVER",
        "QIBM_DIRECTORY_SERVER_QUSRDIR",
        "QIBM_QZHB_HTTP_SERVER_MONITOR",
        "QIBM_QSVR_OBJC_SERVER",
        "QIBM_QSVR_OBJC_CLIENT"
    };

    public static void main(final String... _args) {
        final List<String> appIds = new LinkedList<String>();
        final DcmUserOpts opts = new DcmUserOpts();
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
                appIds.add(arg);
            }
        }

        final List<String> toQuery = appIds.isEmpty()
                ? java.util.Arrays.asList(s_commonApplications)
                : appIds;

        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
            for (final String appId : toQuery) {
                try {
                    final AppCertInfo info = caller.callQycdRetrieveCertUsageInfo(logger, appId);
                    final boolean assigned = StringUtils.isNonEmpty(info.getCertId());
                    logger.println(StringUtils.colorizeForTerminal(
                            info.toString(),
                            assigned ? TerminalColor.CYAN : TerminalColor.YELLOW));
                    logger.println("");
                } catch (final Exception e) {
                    logger.println(StringUtils.colorizeForTerminal(
                            "Application ID  : " + appId + "\n" +
                            "  (not registered or error: " + e.getLocalizedMessage() + ")",
                            TerminalColor.YELLOW));
                    logger.println("");
                }
            }
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
        final String usage = "Usage: dcmlistapps [options] [application_id ...]\n"
                           + "\n"
                           + "    Lists DCM-registered applications and their assigned certificates.\n"
                           + "    If no application IDs are specified, queries all common IBM i applications.\n"
                           + "\n"
                           + "    Valid options include:\n"
                           + "        -y:   Do not ask for confirmation\n"
                           + "        -v:   Verbose output\n"
                           ;
        // @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
