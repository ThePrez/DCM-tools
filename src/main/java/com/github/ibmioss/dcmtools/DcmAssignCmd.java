package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher;
import com.github.ibmioss.dcmtools.utils.ProcessLauncher.ProcessResult;
import com.github.ibmioss.dcmtools.utils.StringUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.github.ibmioss.dcmtools.utils.TempFileManager;

public class DcmAssignCmd {
    private static class AssignOptions extends DcmUserOpts {

        private String m_certId;
        private String m_app;

        public void setCertId(String _id) {
            m_certId = _id;
        }

        public void setApp(String _app) {
            m_app = _app;
        }
        String getAppId() throws IOException {
            if (null != m_app) {
                return m_app;
            }
            if (StringUtils.isEmpty(m_app) && !isYesMode()) {
                final String resp = ConsoleUtils.askUserOrThrow("Enter application ID: ");
                return m_app = resp;
            }
            throw new IOException("ERROR: Application ID is required");
        }
        String getCertId() throws IOException {
            if (null != m_certId) {
                return m_certId;
            }
            if (StringUtils.isEmpty(m_certId) && !isYesMode()) {
                final String resp = ConsoleUtils.askUserOrThrow("Enter certificate ID: ");
                return m_certId = resp;
            }
            throw new IOException("ERROR: Certificate ID is required");
        }

    }

    public static void main(final String... _args) {
        final AssignOptions opts = new AssignOptions();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--cert=")) {
                opts.setCertId(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--app=")) {
                opts.setApp(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("--dcm-store=")) {
                final String dcmStore = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(dcmStore) || "*system".equalsIgnoreCase(dcmStore)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(dcmStore);
                }
            } else {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
        }
        try(DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())){
            caller.callQycdUpdateCertUsage(opts.getAppId(), opts.getDcmStore(), opts.getCertId());
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
		final String usage = "Usage: dcmassign [options]\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                              Do not ask for confirmation\n"
                                + "        --app=<id>:                      Specify the application ID to assign certificate\n"
                                + "                                         usage\n"
                                + "        --cert=<id>:                     Certificate ID to assign\n"
                                + "        --dcm-store=<system/filename>:   Specify the DCM certificate store, or specify 'system'\n"
                                + "                                         to indicate the *SYSTEM store (default)\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
