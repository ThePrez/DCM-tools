package com.github.ibmioss.dcmtools.utils;

import java.util.Map;
import java.util.TreeMap;

public class MessageLookerUpper {
    final static Map<String, String> s_knownMessages;
    static {
        s_knownMessages = new TreeMap<String, String>();
        s_knownMessages.put("CPFB001", "One or more input parameters is NULL or missing.");
        s_knownMessages.put("CPFB002", "Certificate store does not exist.");
        s_knownMessages.put("CPFB003", "Invalid password.");
        s_knownMessages.put("CPFB004", "User not authorized to certificate store.");
        s_knownMessages.put("CPFB005", "Export file already exists.");
        s_knownMessages.put("CPFB006", "An error occurred.");
        s_knownMessages.put("CPFB007", "User not authorized to directory or file.");
        s_knownMessages.put("CPFB008", "The format name for the certificate store is not valid.");
        s_knownMessages.put("CPFB009", "The format name for the export or import file is not valid.");
        s_knownMessages.put("CPFB00A", "Required option of the operating system is not installed.");
        s_knownMessages.put("CPFB010", "Import file does not exist.");
        s_knownMessages.put("CPFB011", "Import file password is not valid.");
        s_knownMessages.put("CPFB012", "Duplicate key exists.");
        s_knownMessages.put("CPF22F0", "Unexpected errors occurred during processing.");
    }
    private final String m_toString;

    public MessageLookerUpper(final String _msgId) {
        final String deets = s_knownMessages.get(_msgId.toUpperCase().trim());
        if (null != deets) {
            m_toString = String.format("%s: %s", _msgId, deets);
        } else {
            m_toString = _msgId;
        }
    }

    @Override
    public String toString() {
        return m_toString;
    }
}
