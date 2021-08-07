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
        if(StringUtils.isEmpty(opts.m_app)) {
            System.err.println(StringUtils.colorizeForTerminal("ERROR: Application ID not specified", TerminalColor.BRIGHT_RED));
            printUsageAndExit();
        }
        if(StringUtils.isEmpty(opts.m_certId)) {
            System.err.println(StringUtils.colorizeForTerminal("ERROR: Certificate ID not specified", TerminalColor.BRIGHT_RED));
            printUsageAndExit();
        }
        try(DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())){
            caller.callQycdUpdateCertUsage(opts.m_app, opts.getDcmStore(), opts.m_certId);
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
		final String usage = "Usage: dcmassign [options] --app=<id> --cert=<id>\n"
		                        + "\n"
		                        + "    Valid options include:\n"
                                + "        -y:                              Do not ask for confirmation\n"
                                + "        --app=<id>:                      Specify the application ID to assign certificate\n"
                                + "                                         usage (required)\n"
                                + "        --cert=<id>:                     Certificate ID to assign (required)\n"
                                + "        --dcm-store=<system/filename>:   Specify the DCM certificate store, or specify 'system'\n"
                                + "                                         to indicate the *SYSTEM store (default)\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
