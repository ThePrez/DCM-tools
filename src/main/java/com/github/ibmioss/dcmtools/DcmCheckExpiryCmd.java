package com.github.ibmioss.dcmtools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Check a DCM certificate store for certificates expiring within a given number of days.
 * Requires IBM i 7.3 or later (uses QSYS2.CERTIFICATE_INFO table function).
 * Exits with return code 1 if any expiring certificates are found (suitable for scripting).
 */
public class DcmCheckExpiryCmd {

    public static void main(final String... _args) {
        int days = 30;
        final DcmUserOpts opts = new DcmUserOpts();
        for (final String arg : _args) {
            if ("-y".equals(arg)) {
                opts.setYesMode(true);
            } else if ("-v".equals(arg)) {
                opts.setVerbose(true);
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsageAndExit();
            } else if (arg.startsWith("--days=")) {
                try {
                    days = Integer.parseInt(DcmUserOpts.extractValue(arg));
                } catch (final NumberFormatException e) {
                    System.err.println(StringUtils.colorizeForTerminal("ERROR: --days must be a number", TerminalColor.BRIGHT_RED));
                    printUsageAndExit();
                }
            } else if (arg.startsWith("--dcm-store=")) {
                final String source = DcmUserOpts.extractValue(arg);
                if ("system".equalsIgnoreCase(source) || "*system".equalsIgnoreCase(source)) {
                    opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                } else {
                    opts.setDcmStore(source);
                }
            } else if (arg.startsWith("--dcm-password=")) {
                opts.setDcmPassword(DcmUserOpts.extractValue(arg));
            } else if (arg.startsWith("-")) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try {
            final String dcmStorePw = opts.getDcmPassword();
            final int daysToCheck = days;
            final String sql =
                "SELECT CERTIFICATE_LABEL, SUBJECT_COMMON_NAME, VALIDITY_END " +
                "FROM TABLE(QSYS2.CERTIFICATE_INFO(CERTIFICATE_STORE_PASSWORD => ?)) " +
                "WHERE VALIDITY_END < CURRENT DATE + ? DAYS " +
                "ORDER BY VALIDITY_END ASC";

            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
            try (Connection conn = DriverManager.getConnection("jdbc:as400://localhost");
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dcmStorePw);
                stmt.setInt(2, daysToCheck);
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        final String label   = rs.getString("CERTIFICATE_LABEL");
                        final String cn      = rs.getString("SUBJECT_COMMON_NAME");
                        final String expiry  = rs.getString("VALIDITY_END");
                        logger.println(StringUtils.colorizeForTerminal(
                            "EXPIRING: label='" + label + "'  CN='" + cn + "'  expires=" + expiry,
                            TerminalColor.BRIGHT_RED));
                    }
                    if (!found) {
                        logger.println_success("No certificates expiring within " + daysToCheck + " days.");
                    } else {
                        TempFileManager.cleanup();
                        System.exit(1);
                    }
                }
            }
        } catch (final ClassNotFoundException e) {
            logger.println_err("AS400JDBCDriver not found. Ensure jt400.jar is on the classpath.");
            TempFileManager.cleanup();
            System.exit(-1);
        } catch (final SQLException e) {
            logger.printExceptionStack_verbose(e);
            logger.println_err(e.getLocalizedMessage());
            TempFileManager.cleanup();
            System.exit(-1);
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
        final String usage = "Usage: dcmcheckexpiry [options]\n"
                           + "\n"
                           + "    Checks a DCM certificate store for certificates expiring soon.\n"
                           + "    Requires IBM i 7.3 or later.\n"
                           + "    Exits with return code 1 if expiring certificates are found.\n"
                           + "\n"
                           + "    Valid options include:\n"
                           + "        -y:                            Do not ask for confirmation\n"
                           + "        --days=<n>:                    Warn about certs expiring within n days (default: 30)\n"
                           + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                           + "                                       to indicate the *SYSTEM store (default)\n"
                           + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                           ;
        // @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
