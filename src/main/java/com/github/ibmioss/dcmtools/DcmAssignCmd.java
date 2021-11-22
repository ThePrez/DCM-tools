package com.github.ibmioss.dcmtools;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

public class DcmAssignCmd {
    private static class AssignOptions extends DcmUserOpts {

        private final Set<String> m_apps = new HashSet<String>();
        private String m_certId;

        public void addApp(final String _app) {
            m_apps.add(_app);
        }

        Set<String> getApps() throws IOException {
            if (m_apps.isEmpty() && !isYesMode()) {
                final String resp = ConsoleQuestionAsker.get().askUserOrThrow("Enter application ID: ");
                m_apps.add(resp);
                return m_apps;
            }
            throw new IOException("ERROR: Application ID is required");
        }

        Set<String> getAppsWithShorthandsProcessed() throws IOException {
            final Set<String> apps = getApps();
            final Set<String> ret = new HashSet<String>();
            for (final String app : apps) {
                final String[] shorthands = s_shortHands.get(app.toUpperCase());
                if (null != shorthands) {
                    for (final String shorthand : shorthands) {
                        ret.add(shorthand);
                    }
                } else {
                    ret.add(app);
                }
            }
            return ret;
        }

        String getCertId() throws IOException {
            if (null != m_certId) {
                return m_certId;
            }
            if (StringUtils.isEmpty(m_certId) && !isYesMode()) {
                final String resp = ConsoleQuestionAsker.get().askUserOrThrow("Enter certificate ID: ");
                return m_certId = resp;
            }
            throw new IOException("ERROR: Certificate ID is required");
        }

        public void setCertId(final String _id) {
            m_certId = _id;
        }

    }

    private static final String[] s_commonApplications;
    //@formatter:on
    private static final Map<String, String[]> s_shortHands;

    static {
//@formatter:off
        s_commonApplications = new String[] {
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
                "QIBM_OS400_QZBS_SVR",
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
                "QIBM_QSVR_OBJC_CLIENT"};
//@formatter:on
        s_shortHands = new LinkedHashMap<String, String[]>();
        s_shortHands.put("5250", new String[] { "QIBM_QTV_TELNET_SERVER" });
        s_shortHands.put("TELNET", new String[] { "QIBM_QTV_TELNET_SERVER" });
        final String[] hostServers = new String[] { "QIBM_OS400_QZBS_SVR_CENTRAL", "QIBM_OS400_QZBS_SVR_DATABASE", "QIBM_OS400_QZBS_SVR_DTAQ", "QIBM_OS400_QZBS_SVR_NETPRT", "QIBM_OS400_QZBS_SVR_RMTCMD", "QIBM_OS400_QZBS_SVR_SIGNON", "QIBM_OS400_QZBS_SVR_FILE", "QIBM_OS400_QRW_SVR_DDM_DRDA" };
        s_shortHands.put("HOSTSERVERS", hostServers);
        s_shortHands.put("HOSTSERVER", hostServers);
        s_shortHands.put("HOSTSVR", hostServers);
        for (final String commonApp : s_commonApplications) {
            if (commonApp.startsWith("QIBM_OS400_QZBS_SVR_")) {
                s_shortHands.put(commonApp.replace("QIBM_OS400_QZBS_SVR_", ""), new String[] { commonApp });
            } else if (commonApp.endsWith("_SERVER")) {
                final String shortName = commonApp.replace("_SERVER", "").replaceAll(".*_", "");
                // System.out.println("shortname for '" + commonApp + "' is '" + shortName + "'");
                s_shortHands.put(shortName, new String[] { commonApp });
            }
        }
    }

    public static void main(final String... _args) {
        final AssignOptions opts = new AssignOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--cert=")) {
                opts.setCertId(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--dcm-store=")) {
                final String dcmStore = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(dcmStore) || "*system".equalsIgnoreCase(dcmStore)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(dcmStore);
                }
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            } else {
                opts.addApp(arg);
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
            for (final String app : opts.getAppsWithShorthandsProcessed()) {
                logger.println("Assigning to " + app + "...");
                caller.callQycdUpdateCertUsage(logger, app, opts.getDcmStore(), opts.getCertId());
            }
            logger.println_success("SUCCESS!!!");
        } catch (final Exception e) {
            logger.printExceptionStack_verbose(e);
            logger.println_err(e.getLocalizedMessage());
            TempFileManager.cleanup();
            System.exit(-1); // TODO: allow skip on nonfatal
        } finally {
            TempFileManager.cleanup();
        }

    }

    private static void printUsageAndExit() {
        // @formatter:off
		String usage = "Usage: dcmassign [options] <application_id>...\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                              Do not ask for confirmation\n"
                                + "        --cert=<id>:                     Certificate ID to assign\n"
                                + "        --dcm-store=<system/filename>:   Specify the DCM certificate store, or specify 'system'\n"
                                + "                                         to indicate the *SYSTEM store (default)\n"
                                + "\n"
                                + "    For application id, specify the id as defined in DCM, or a 'shorthand' identifier.\n"
                                + "    Valid shorthand identifiers include:\n"
                                ;
		for(final String s : s_shortHands.keySet()) {
		    usage += "        "+s+"\n";
		}
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
