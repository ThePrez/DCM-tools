package com.github.ibmioss.dcmtools;

import java.io.File;
import java.io.IOException;

import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;

public class DcmUserOpts {
    public static final String SYSTEM_DCM_STORE = "/QIBM/UserData/ICSS/Cert/Server/DEFAULT.KDB";

    static String extractValue(final String _str) {
        final int equalsSign = _str.indexOf('=');
        return (-1 == equalsSign) ? _str : _str.substring(1 + equalsSign);
    }

    private String dcmPassword = null;
    private String dcmStore = SYSTEM_DCM_STORE;

    private boolean isYesMode = false;

    public String getDcmPassword() throws IOException {

        // Resolve the DCM keystore password
        if (StringUtils.isNonEmpty(dcmPassword)) {
            return dcmPassword;
        }
        if (isYesMode) {
            throw new IOException("DCM keystore password not specified");
        }
        return dcmPassword = ConsoleQuestionAsker.get().askUserForPwd("Enter DCM keystore password: ");
    }

    public String getDcmStore() throws IOException {
        if (StringUtils.isNonEmpty(dcmStore)) {
            return dcmStore;
        }
        final String dcmStoreResp = isYesMode ? "*SYSTEM" : ConsoleQuestionAsker.get().askUserWithDefault("Target DCM keystore: (leave blank for *SYSTEM) ", "*SYSTEM");
        if ("*SYSTEM".equals(dcmStoreResp.trim().toUpperCase()) || "SYSTEM".equals(dcmStoreResp.trim().toUpperCase())) {
            return dcmStore = SYSTEM_DCM_STORE;
        } else {
            return dcmStore = new File(dcmStoreResp).getAbsolutePath();
        }
    }

    String getDcmStoreNonInteractive() {
        return dcmStore;
    }

    public boolean isYesMode() {
        return isYesMode;
    }

    public void setDcmPassword(final String dcmPassword) {
        this.dcmPassword = dcmPassword;
    }

    public void setDcmStore(final String _dcmStore) {
        this.dcmStore = null == _dcmStore ? null : new File(_dcmStore).getAbsolutePath();
    }

    public void setYesMode(final boolean isYesMode) {
        this.isYesMode = isYesMode;
    }

    public void validate() {// TODO: ?

    }
}
