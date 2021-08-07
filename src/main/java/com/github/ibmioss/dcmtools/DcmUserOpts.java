package com.github.ibmioss.dcmtools;

import java.io.IOException;

import com.github.ibmioss.dcmtools.utils.ConsoleUtils;
import com.github.ibmioss.dcmtools.utils.StringUtils;

public abstract class DcmUserOpts {
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
        return dcmPassword = ConsoleUtils.askUserForPwd("Enter DCM keystore password: ");
    }

    public String getDcmStore() throws IOException {
        if (StringUtils.isNonEmpty(dcmStore)) {
            return dcmStore;
        }
        final String dcmStoreResp = isYesMode ? "*SYSTEM" : ConsoleUtils.askUserWithDefault("Target DCM keystore: (leave blank for *SYSTEM) ", "*SYSTEM");
        if ("*SYSTEM".equals(dcmStoreResp.trim().toUpperCase()) || "SYSTEM".equals(dcmStoreResp.trim().toUpperCase())) {
            return dcmStore = SYSTEM_DCM_STORE;
        } else {
            return dcmStore = dcmStoreResp;
        }
    }

    public boolean isYesMode() {
        return isYesMode;
    }

    public void setDcmPassword(String dcmPassword) {
        this.dcmPassword = dcmPassword;
    }

    public void setDcmStore(String _dcmStore) {
        this.dcmStore = _dcmStore;
    }

    public void setYesMode(boolean isYesMode) {
        this.isYesMode = isYesMode;
    }

    public void validate() {// TODO: ?

    }
}
