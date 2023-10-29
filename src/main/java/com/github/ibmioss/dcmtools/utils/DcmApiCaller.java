package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.Closeable;
import java.io.IOException;

import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Bin4;
import com.ibm.as400.access.AS400DataType;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Structure;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCodeParameter;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.ServiceProgramCall;
import com.ibm.as400.access.Trace;

public class DcmApiCaller implements Closeable {

    private final AS400 m_conn;

    public DcmApiCaller(final boolean _isYesMode) throws IOException {
        final String osName = System.getProperty("os.name", "");
        if (osName.equalsIgnoreCase("OS/400")) { // Running on IBM i, using JV1
            m_conn = new AS400("localhost", "*CURRENT", "*CURRENT");
        } else if (osName.equalsIgnoreCase("OS400")) { // Running on i, OpenJDK
            if (_isYesMode) {
                throw new IOException("IBM i password not specified. Run in interactive mode or with JV1 Java for this to work.");
            }
            m_conn = new AS400("localhost", System.getProperty("user.name", "*CURRENT"), ConsoleQuestionAsker.get().askUserOrThrow("Enter IBM i password: "));
        } else {
            if (_isYesMode) {
                throw new IOException("Not allowed with '-y'");
            }
            m_conn = new AS400(ConsoleQuestionAsker.get().askUserOrThrow("Enter IBM i system name: "), ConsoleQuestionAsker.get().askUserOrThrow("Enter IBM i user name: "), ConsoleQuestionAsker.get().askUserForPwd("Enter IBM i password: "));
        }
    }

    public void callQycdAddCACertTrust(final AppLogger _logger, final String _dcmStore, final String _dcmStorePw, final String _appId, final String _alias) throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ServiceProgramCall program = new ServiceProgramCall(m_conn);
        // Initialize the name of the program to run.
        final String programName = "/QSYS.LIB/QICSS.LIB/QYCDCUSG.SRVPGM";
        final ProgramParameter[] parameterList = new ProgramParameter[6];
        // 1 Application ID Input Char(*)
        parameterList[0] = new ProgramParameter(new AS400Text(_appId.length()).toBytes(_appId));
        // 2 Length of application ID Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(_appId.length()));
        // 3 Trusted CA certificate ID type Input Char(1)
        parameterList[2] = new ProgramParameter(new AS400Text(1).toBytes("1"));
        // 4 Trusted CA certificate ID Input Char(*)
        parameterList[3] = new ProgramParameter(new AS400Text(_alias.length()).toBytes(_alias));
        // 5 Length of trusted CA certificate ID Input Binary(4)
        parameterList[4] = new ProgramParameter(new AS400Bin4().toBytes(_alias.length()));
        // 6 Error code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[5] = ec;

        program.setProgram(programName, parameterList);
        program.setProcedureName("QycdAddCACertTrust");
        // Run the program.
        runProgram(_logger, program, ec);
    }

    public void callQycdRemoveCertUsage(final AppLogger _logger, final String _dcmStore, final String _dcmStorePw, final String _appId, final String _alias) throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ServiceProgramCall program = new ServiceProgramCall(m_conn);
        // Initialize the name of the program to run.
        final String programName = "/QSYS.LIB/QICSS.LIB/QYCDCUSG.SRVPGM";
        final ProgramParameter[] parameterList = new ProgramParameter[7];
        // 1 Application ID Input Char(*)
        parameterList[0] = new ProgramParameter(new AS400Text(_appId.length()).toBytes(_appId));
        // 2 Length of application ID Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(_appId.length()));
        // 3 Certificate store name Input Char(*)
        parameterList[2] = new ProgramParameter(new AS400Text(_dcmStore.length()).toBytes(_dcmStore));
        // 4 Length of certificate store name Input Binary(4)
        parameterList[3] = new ProgramParameter(new AS400Bin4().toBytes(_dcmStore.length()));
        // 5 Certificate ID Input Char(*)
        parameterList[4] = new ProgramParameter(new AS400Text(_alias.length()).toBytes(_alias));
        // 6 Length of certificate ID Input Binary(4)
        parameterList[5] = new ProgramParameter(new AS400Bin4().toBytes(_alias.length()));
        // 7 Error code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[6] = ec;

        program.setProgram(programName, parameterList);
        program.setProcedureName("QycdRemoveCertUsage");
        // Run the program.
        runProgram(_logger, program, ec);
    }

    public void callQycdRenewCertificate_RNWC0300(final AppLogger _logger, final String _file) throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ProgramCall program = new ServiceProgramCall(m_conn);
        // Initialize the name of the program to run.
        final String programName = "/QSYS.LIB/QICSS.LIB/QYCDRNWC.SRVPGM";
        final String apiFormat = "RNWC0300";

        final AS400Structure arg0 = new AS400Structure(new AS400DataType[] {
                // 0 0 Binary (4) Offset to certificate path and file name
                new AS400Bin4(),
                // 4 4 Binary (4) Length of certificate path and file name
                new AS400Bin4(),
                // Char (*) Certificate path and file name
                new AS400Text(_file.length()) }); // TODO

        // Set up the parms
        final ProgramParameter[] parameterList = new ProgramParameter[4];

        // 1 Certificate request data Input Char(*)
        parameterList[0] = new ProgramParameter(arg0.toBytes(new Object[] { 8, _file.length(), _file }));
        // 2 Length of certificate request data Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(arg0.getByteLength()));
        // 3 Format name Input Char(8)
        parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes(apiFormat));
        // 4 Error Code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[3] = ec;

        program.setProgram(programName, parameterList);
        program.setProcedureName("QycdRenewCertificate");

        // TODO: temp trace data
        Trace.setTraceOn(true);
        Trace.setTraceAllOn(true);

        // Run the program.
        runProgram(_logger, program, ec);
    }

    public void callQycdRetrieveCertUsageInfo(final AppLogger _logger, final String _selectionCriteria) {
        // TODO

    }

    public void callQycdUpdateCertUsage(final AppLogger _logger, final String _appId, final String _certStoreName, final String _certId) throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ServiceProgramCall program = new ServiceProgramCall(m_conn);
        final String programName = "/QSYS.LIB/QICSS.LIB/QYCDCUSG.SRVPGM";
        final ProgramParameter[] parameterList = new ProgramParameter[8];
        // 1 Application ID Output Char(*)
        parameterList[0] = new ProgramParameter(new AS400Text(_appId.length()).toBytes(_appId));
        parameterList[0].setParameterType(ProgramParameter.PASS_BY_REFERENCE);
        // 2 Length of application ID Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(_appId.length()));
        // 3 Certificate store name Input Char(*)
        parameterList[2] = new ProgramParameter(new AS400Text(_certStoreName.length()).toBytes(_certStoreName));
        // 4 Length of certificate store name Input Binary(4)
        parameterList[3] = new ProgramParameter(new AS400Bin4().toBytes(_certStoreName.length()));
        // 5 Certificate ID type Input Char(*)
        parameterList[4] = new ProgramParameter(new AS400Text(1).toBytes("1"));
        // 6 Certificate ID Input Char(*)
        parameterList[5] = new ProgramParameter(new AS400Text(_certId.length()).toBytes(_certId));
        // 7 Length of certificate ID Input Binary(4)
        parameterList[6] = new ProgramParameter(new AS400Bin4().toBytes(_certId.length()));
        // 8 Error code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[7] = ec;

        program.setProgram(programName, parameterList);
        program.setProcedureName("QycdUpdateCertUsage");
        // Run the program.
        runProgram(_logger, program, ec);
    }

    public void callQykmExportKeyStore(final AppLogger _logger, final String _dcmStore, final String _dcmStorePw, final String _exportFile, final String _exportFilePw) throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ProgramCall program = new ProgramCall(m_conn);
        // Initialize the name of the program to run.
        final String programName = "/QSYS.LIB/QYKMEXPK.PGM";
        final ProgramParameter[] parameterList = new ProgramParameter[14];
        // 1 Certificate store path and file Name Input Char(*)
        parameterList[0] = new ProgramParameter(new AS400Text(_dcmStore.length()).toBytes(_dcmStore));
        // 2 Length of certificate store path and file Name Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(_dcmStore.length()));
        // 3 Format of certificate store path and file Name Input Char(8)
        parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
        // 4 Certificate store password Input Char(*)
        parameterList[3] = new ProgramParameter(new AS400Text(_dcmStorePw.length(), 1208).toBytes(_dcmStorePw));
        // 5 Length of certificate store password Input Binary(4)
        parameterList[4] = new ProgramParameter(new AS400Bin4().toBytes(_dcmStorePw.length()));
        // 6 CCSID of certificate store password Input Binary(4)
        parameterList[5] = new ProgramParameter(new AS400Bin4().toBytes(1208));
        // 7 Export path and file name Input Char(*)
        parameterList[6] = new ProgramParameter(new AS400Text(_exportFile.length()).toBytes(_exportFile));
        // 8 Length of export path and file name Input Binary(4)
        parameterList[7] = new ProgramParameter(new AS400Bin4().toBytes(_exportFile.length()));
        // 9 Format of import path and file name Input Char(8)
        parameterList[8] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
        // 10 Version of export file Input Char(10)
        parameterList[9] = new ProgramParameter(new AS400Text(10).toBytes("*PKCS12V3 "));
        // 11 Export file password Input Char(*)
        parameterList[10] = new ProgramParameter(new AS400Text(_exportFilePw.length(), 1208).toBytes(_exportFilePw));
        // 12 Length of export file password Input Binary(4)
        parameterList[11] = new ProgramParameter(new AS400Bin4().toBytes(_exportFilePw.length()));
        // 13 CCSID of export file password Input Binary(4)
        parameterList[12] = new ProgramParameter(new AS400Bin4().toBytes(1208));
        // 14 Error code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[13] = ec;

        program.setProgram(programName, parameterList);
        // Run the program.
        runProgram(_logger, program, ec);
    }

    // QykmImportKeyStore
    public void callQykmImportKeyStore(final AppLogger _logger, final String _dcmStore, final String _dcmStorePw, final String _dcmImportFile, final String _importFilePw)
            throws PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        final ProgramCall program = new ProgramCall(m_conn);
        // Initialize the name of the program to run.
        final String programName = "/QSYS.LIB/QYKMIMPK.PGM";
        // Set up the parms
        final ProgramParameter[] parameterList = new ProgramParameter[14];
        // 1 Certificate store path and file Name Input Char(*)
        parameterList[0] = new ProgramParameter(new AS400Text(_dcmStore.length()).toBytes(_dcmStore));
        // 2 Length of certificate store path and file Name Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(_dcmStore.length()));
        // 3 Format of certificate store path and file Name Input Char(8)
        parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
        // 4 Certificate store password Input Char(*)
        parameterList[3] = new ProgramParameter(new AS400Text(_dcmStorePw.length(), 1208).toBytes(_dcmStorePw));
        // 5 Length of certificate store password Input Binary(4)
        parameterList[4] = new ProgramParameter(new AS400Bin4().toBytes(_dcmStorePw.length()));
        // 6 CCSID of certificate store password Input Binary(4)
        parameterList[5] = new ProgramParameter(new AS400Bin4().toBytes(1208));
        // 7 Import path and file name Input Char(*)
        parameterList[6] = new ProgramParameter(new AS400Text(_dcmImportFile.length()).toBytes(_dcmImportFile));
        // 8 Length of import path and file name Input Binary(4)
        parameterList[7] = new ProgramParameter(new AS400Bin4().toBytes(_dcmImportFile.length()));
        // 9 Format of import path and file name Input Char(8)
        parameterList[8] = new ProgramParameter(new AS400Text(8).toBytes("OBJN0100"));
        // 10 Version of import file Input Char(10)
        parameterList[9] = new ProgramParameter(new AS400Text(10).toBytes("*PKCS12V3 "));
        // 11 Import file password Input Char(*)
        parameterList[10] = new ProgramParameter(new AS400Text(_importFilePw.length(), 1208).toBytes(_importFilePw));
        // 12 Length of import file password Input Binary(4)
        parameterList[11] = new ProgramParameter(new AS400Bin4().toBytes(_importFilePw.length()));
        // 13 CCSID of import file password Input Binary(4)
        parameterList[12] = new ProgramParameter(new AS400Bin4().toBytes(1208));
        // 14 Error code I/O Char(*)
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[13] = ec;

        program.setProgram(programName, parameterList);
        // Run the program.
        runProgram(_logger, program, ec);
    }

    @Override
    public void close() throws IOException {
        m_conn.disconnectAllServices();
    }

    private void runProgram(final AppLogger _logger, final ProgramCall _program, final ErrorCodeParameter _ec) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        if (!_program.run()) {
            for (final AS400Message msg : _program.getMessageList()) {
                // Show each message.
                _logger.println_err("" + msg);
            }
            throw new IOException("DCM API call failure");
        }
        final String errorMessageId = _ec.getMessageID();
        for (final AS400Message msg : _program.getMessageList()) {
            // Show each message.
            _logger.println(StringUtils.colorizeForTerminal("" + msg, TerminalColor.CYAN));
        }
        if (!StringUtils.isEmpty(errorMessageId)) {
            throw new IOException("API gave error message " + new MessageLookerUpper(errorMessageId.trim()));
        }
    }

}
