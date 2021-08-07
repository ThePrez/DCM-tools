package com.github.ibmioss.dcmtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;

import com.github.ibmioss.dcmtools.CertFileExporter.ExportOptions;
import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
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
public class DcmCmd {

    public static void main(final String... _args) {
        LinkedList<String> args = new LinkedList<String>(Arrays.asList(_args));
        if(args.isEmpty()) {
            printUsageAndExit();
        }
        String operation = args.remove();
        String[] passOnArgs = args.toArray(new String[args.size()]);
        switch(operation) {
            case "import": DcmImportCmd.main(passOnArgs);return;
            case "export": DcmExportCmd.main(passOnArgs);return;
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
                                ;
		// @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
