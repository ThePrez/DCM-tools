package com.github.ibmioss.dcmtools;

import java.util.Arrays;
import java.util.LinkedList;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Main entry point for the application
 *
 * @author Jesse Gorzinski
 */
public class DcmCmd {

    public static void main(final String... _args) {
        final LinkedList<String> args = new LinkedList<String>(Arrays.asList(_args));
        if (args.isEmpty()) {
            printUsageAndExit();
        }
        final String operation = args.remove();
        final String[] passOnArgs = args.toArray(new String[args.size()]);
        switch (operation) {
            case "import":
                DcmImportCmd.main(passOnArgs);
                return;
            case "export":
                DcmExportCmd.main(passOnArgs);
                return;
            case "exportCert":
                DcmExportCmd.main(passOnArgs);
                return;
            case "assign":
                DcmAssignCmd.main(passOnArgs);
                return;
            case "renew":
                DcmRenewCmd.main(passOnArgs);
                return;
            case "checkexpiry":
                DcmCheckExpiryCmd.main(passOnArgs);
                return;
            case "listapps":
                DcmListAppsCmd.main(passOnArgs);
                return;
            case "register":
                DcmRegisterCmd.main(passOnArgs);
                return;
            case "unregister":
                DcmUnregisterCmd.main(passOnArgs);
                return;
            case "csr":
                DcmCsrCmd.main(passOnArgs);
                return;
        }
        System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown operation '" + operation + "'", TerminalColor.BRIGHT_RED));
        printUsageAndExit();
    }

    private static void printUsageAndExit() {
        // @formatter:off
		final String usage = "Usage: dcm <operation> [options]\n"
		                        + "    Valid operations include:\n"
                                + "        import             Import certificates into DCM\n"
                                + "        export             Export certificates from DCM\n"
                                + "        assign             Assign a certificate to an application\n"
                                + "        renew              Renew a certificate\n"
                                + "        checkexpiry        Check for certificates expiring soon\n"
                                + "        listapps           List applications and their assigned certificates\n"
                                + "        register           Register an application in DCM\n"
                                + "        unregister         Unregister an application from DCM\n"
                                + "        csr                Generate a Certificate Signing Request\n"
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
