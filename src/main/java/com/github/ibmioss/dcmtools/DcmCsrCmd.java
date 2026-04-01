package com.github.ibmioss.dcmtools;

import com.github.ibmioss.dcmtools.utils.DcmApiCaller;
import com.github.ibmioss.dcmtools.utils.TempFileManager;
import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;

/**
 * Generate a Certificate Signing Request (CSR) via the DCM QYCDCSRC API.
 * The resulting CSR file can be submitted to a Certificate Authority for signing.
 */
public class DcmCsrCmd {

    private static class CsrOptions extends DcmUserOpts {
        private String commonName    = null;
        private String org           = "";
        private String orgUnit       = "";
        private String locality      = "";
        private String state         = "";
        private String country       = "";
        private int    keySize       = 2048;
        private String outputFile    = null;

        String getCommonName() throws java.io.IOException {
            if (StringUtils.isNonEmpty(commonName)) return commonName;
            if (!isYesMode()) {
                return commonName = ConsoleQuestionAsker.get().askUserOrThrow("Enter Common Name (CN): ");
            }
            throw new java.io.IOException("ERROR: Common Name (--cn) is required");
        }

        String getOutputFile() throws java.io.IOException {
            if (StringUtils.isNonEmpty(outputFile)) return outputFile;
            if (!isYesMode()) {
                return outputFile = ConsoleQuestionAsker.get().askUserOrThrow("Enter output file path for CSR: ");
            }
            throw new java.io.IOException("ERROR: Output file (--output) is required");
        }

        String getOrg()      { return org; }
        String getOrgUnit()  { return orgUnit; }
        String getLocality() { return locality; }
        String getState()    { return state; }
        String getCountry()  { return country; }
        int    getKeySize()  { return keySize; }

        void setCommonName(final String _v)  { commonName = _v; }
        void setOrg(final String _v)         { org = _v; }
        void setOrgUnit(final String _v)     { orgUnit = _v; }
        void setLocality(final String _v)    { locality = _v; }
        void setState(final String _v)       { state = _v; }
        void setCountry(final String _v)     { country = _v; }
        void setOutputFile(final String _v)  { outputFile = _v; }

        void setKeySize(final String _v) throws java.io.IOException {
            try {
                keySize = Integer.parseInt(_v);
            } catch (final NumberFormatException e) {
                throw new java.io.IOException("ERROR: --key-size must be a number (e.g. 2048, 4096)");
            }
        }
    }

    public static void main(final String... _args) {
        final CsrOptions opts = new CsrOptions();
        for (final String arg : _args) {
            try {
                if ("-y".equals(arg)) {
                    opts.setYesMode(true);
                } else if ("-v".equals(arg)) {
                    opts.setVerbose(true);
                } else if ("-h".equals(arg) || "--help".equals(arg)) {
                    printUsageAndExit();
                } else if (arg.startsWith("--dcm-store=")) {
                    final String source = DcmUserOpts.extractValue(arg);
                    if ("system".equalsIgnoreCase(source) || "*system".equalsIgnoreCase(source)) {
                        opts.setDcmStore(DcmUserOpts.SYSTEM_DCM_STORE);
                    } else {
                        opts.setDcmStore(source);
                    }
                } else if (arg.startsWith("--dcm-password=")) {
                    opts.setDcmPassword(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--cn=")) {
                    opts.setCommonName(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--org=")) {
                    opts.setOrg(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--org-unit=")) {
                    opts.setOrgUnit(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--locality=")) {
                    opts.setLocality(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--state=")) {
                    opts.setState(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--country=")) {
                    opts.setCountry(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--key-size=")) {
                    opts.setKeySize(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("--output=")) {
                    opts.setOutputFile(DcmUserOpts.extractValue(arg));
                } else if (arg.startsWith("-")) {
                    System.err.println(StringUtils.colorizeForTerminal("ERROR: Unknown option '" + arg + "'", TerminalColor.BRIGHT_RED));
                    printUsageAndExit();
                }
            } catch (final java.io.IOException e) {
                System.err.println(StringUtils.colorizeForTerminal("ERROR: " + e.getMessage(), TerminalColor.BRIGHT_RED));
                printUsageAndExit();
            }
        }
        final AppLogger logger = AppLogger.getSingleton(opts.isVerbose());
        try (DcmApiCaller caller = new DcmApiCaller(opts.isYesMode())) {
            final String cn         = opts.getCommonName();
            final String outputFile = opts.getOutputFile();
            logger.println("Generating CSR for CN='" + cn + "' to file: " + outputFile);
            caller.callQycdCreateCertRequest(logger,
                    opts.getDcmStore(),
                    opts.getDcmPassword(),
                    cn,
                    opts.getOrg(),
                    opts.getOrgUnit(),
                    opts.getLocality(),
                    opts.getState(),
                    opts.getCountry(),
                    opts.getKeySize(),
                    outputFile);
            logger.println_success("SUCCESS!!! CSR written to: " + outputFile);
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
        final String usage = "Usage: dcmcsr [options]\n"
                           + "\n"
                           + "    Generates a Certificate Signing Request (CSR) using the DCM key store.\n"
                           + "    The CSR can be submitted to a Certificate Authority for signing.\n"
                           + "\n"
                           + "    Valid options include:\n"
                           + "        -y:                            Do not ask for confirmation\n"
                           + "        --cn=<name>:                   Common Name (required)\n"
                           + "        --org=<name>:                  Organization name\n"
                           + "        --org-unit=<name>:             Organizational unit\n"
                           + "        --locality=<city>:             City/locality\n"
                           + "        --state=<state>:               State/province\n"
                           + "        --country=<cc>:                Two-letter country code (e.g. US)\n"
                           + "        --key-size=<bits>:             RSA key size (default: 2048)\n"
                           + "        --output=<file>:               Output file path for the CSR (required)\n"
                           + "        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'\n"
                           + "                                       to indicate the *SYSTEM store (default)\n"
                           + "        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)\n"
                           ;
        // @formatter:on
        System.err.println(usage);
        System.exit(-1);
    }
}
