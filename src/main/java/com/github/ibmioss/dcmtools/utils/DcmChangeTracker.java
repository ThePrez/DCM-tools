package com.github.ibmioss.dcmtools.utils;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.DcmUserOpts;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class DcmChangeTracker {

    public static class CertAddedhange implements DcmChange {
        private final String m_label;
        private final Certificate m_newCert;

        public CertAddedhange(final String m_label, final Certificate _newCert) {
            this.m_label = m_label;
            this.m_newCert = _newCert;
        }

        @Override
        public String getFormattedExplanation(final String _linePrefix) {
            String ret = String.format("%sThe following certificate was added with certificate with ID '%s':\n", _linePrefix, m_label);
            ret += CertUtils.getCertInfoStr(m_newCert, _linePrefix + "        ");
            return ret;
        }

        public String getLabel() {
            return m_label;
        }
    }

    public class CertRemovedChange implements DcmChange {
        private final String m_label;

        public CertRemovedChange(final String m_label) {
            this.m_label = m_label;
        }

        @Override
        public String getFormattedExplanation(final String _linePrefix) {
            return String.format("%sRemoval: The certificate with ID '%s' has been removed\n", _linePrefix, m_label);
        }

        public String getLabel() {
            return m_label;
        }
    }

    public static class CertUpdatedhange implements DcmChange {
        private final String m_label;
        private final Certificate m_newCert;
        private final Certificate m_oldCert;

        public CertUpdatedhange(final String m_label, final Certificate _oldCert, final Certificate _newCert) {
            this.m_label = m_label;
            this.m_oldCert = _oldCert;
            this.m_newCert = _newCert;
        }

        @Override
        public String getFormattedExplanation(final String _linePrefix) {
            String ret = String.format("%sThe certificate with ID '%s' has been updated.\n", m_label);
            ret += String.format("%s    The old certificate is:\n%s", _linePrefix, CertUtils.getCertInfoStr(m_oldCert, _linePrefix + "        "));
            ret += String.format("%s    The new certificate is:\n%s", _linePrefix, CertUtils.getCertInfoStr(m_oldCert, _linePrefix + "        "));
            return ret;
        }

        public String getLabel() {
            return m_label;
        }
    }

    public interface DcmChange {
        public String getFormattedExplanation(String _linePrefix);
    }

    private final DcmUserOpts m_opts;

    private final KeyStoreInterrogator m_startingSnapshot;

    public DcmChangeTracker(final DcmUserOpts _opts) throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        m_startingSnapshot = KeyStoreInterrogator.getFromDCM(_opts.isYesMode(), _opts.getDcmStore(), _opts.getDcmPassword());
        m_opts = _opts;
    }

    public synchronized List<DcmChange> getChanges() throws IOException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final List<DcmChange> ret = new LinkedList<DcmChange>();

        // Get the current snapshot
        final KeyStoreInterrogator current = KeyStoreInterrogator.getFromDCM(m_opts.isYesMode(), m_opts.getDcmStore(), m_opts.getDcmPassword());

        final ArrayList<String> startingAliases = Collections.list(m_startingSnapshot.getKeyStore().aliases());
        final ArrayList<String> currentAliases = Collections.list(current.getKeyStore().aliases());
        final List<String> intersection = new LinkedList<>(startingAliases);
        intersection.retainAll(currentAliases);

        // look for deleted certificates
        for (final String startingAlias : startingAliases) {
            if (!current.getKeyStore().containsAlias(startingAlias)) {
                ret.add(new CertRemovedChange(startingAlias));
            }
        }

        // look for new certificates
        for (final String currentAlias : currentAliases) {
            if (!startingAliases.contains(currentAlias)) {
                ret.add(new CertAddedhange(currentAlias, current.getKeyStore().getCertificate(currentAlias)));
            }
        }

        // look for updated certificates
        for (final String commonAlias : intersection) {
            final Certificate oldCert = m_startingSnapshot.getKeyStore().getCertificate(commonAlias);
            final Certificate newCert = current.getKeyStore().getCertificate(commonAlias);
            if (!CertUtils.areCertsEqual(oldCert, newCert)) {
                ret.add(new CertUpdatedhange(commonAlias, oldCert, newCert));
            }
        }
        return ret;
    }

    public KeyStoreInterrogator getStartingSnapshot() {
        return m_startingSnapshot;
    }

    public synchronized void printChanges() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, PropertyVetoException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException {
        final List<DcmChange> changes = getChanges();
        if (changes.isEmpty()) {
            throw new IOException("No changes were made to the DCM keystore!");
        }
        System.out.println("The following changes were made on the DCM keystore:");
        for (final DcmChange change : changes) {
            System.out.println(StringUtils.colorizeForTerminal(change.getFormattedExplanation("    "), TerminalColor.GREEN));
        }
    }
}
