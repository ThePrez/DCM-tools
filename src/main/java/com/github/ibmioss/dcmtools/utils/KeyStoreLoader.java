package com.github.ibmioss.dcmtools.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

import com.github.ibmioss.dcmtools.utils.ProcessLauncher.ProcessResult;
import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;

public class KeyStoreLoader {
    private static final String PKCS_12 = "PKCS12";

    public static String extractTrustFromInstalledCerts() throws IOException {
        final File destFile = TempFileManager.createTempFile(null);
        destFile.delete();
        final ProcessResult cmdResults = ProcessLauncher.exec("/QOpenSys/pkgs/bin/trust extract --format=java-cacerts --purpose=server-auth -v " + destFile.getAbsolutePath());
        if (0 != cmdResults.getExitStatus()) {
            for (final String errLine : cmdResults.getStderr()) {
                System.err.println(StringUtils.colorizeForTerminal(errLine, TerminalColor.RED));
            }
            throw new IOException("Error extracting trusted certificates");
        }
        System.out.println(StringUtils.colorizeForTerminal("Successfully extracted installed certificates", TerminalColor.GREEN));
        return destFile.getAbsolutePath(); // TODO: delete this file!
    }

    private final KeyStore m_keyStore;

    public KeyStoreLoader(final KeyStore _ks) {
        m_keyStore = _ks;
    }

    public KeyStoreLoader(final String _file, final String _pw, final String _label, final boolean _caOnly) throws IOException {
        // Try to load as keystore file
        final String[] keystoreTypes = new String[] { KeyStore.getDefaultType(), "JKS", "PKCS12", "JCEKS", "PKCS12V3" };
        KeyStore loaded = null;
        for (final String keystoreType : keystoreTypes) {
            try (FileInputStream fis = new FileInputStream(_file)) {
                final KeyStore keyStore = KeyStore.getInstance(keystoreType);
                keyStore.load(fis, null == _pw ? null : _pw.toCharArray());
                loaded = keyStore;
                break;
            } catch (final Throwable e) {
            }
        }

        // That didn't work! Try to load as a certificate file
        if (null == loaded) {
            try (FileInputStream fis = new FileInputStream(_file)) {
                final Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(fis);
                if (!certs.isEmpty()) {
                    final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    int counter = 1;
                    for (final Certificate cert : certs) {
                        if (_caOnly && isCertCa(cert)) {
                            continue;
                        }
                        String aliasBase = StringUtils.isEmpty(_label) ? new File(_file).getName().replaceFirst("[.][^.]+$", "") : _label.trim();
                        String aliasSuffix = (1==counter)? "":"."+counter;
                        counter++;
                        String alias = aliasBase+aliasSuffix;
                        System.out.println("importing into certificate ID "+alias);
                        keyStore.setCertificateEntry(alias, cert);
                    }
                    loaded = keyStore;
                }
            } catch (final Throwable e) {
            }
        }

        // Out of ideas
        if (null == loaded) {
            throw new IOException("Couldn't load certificates from file");
        }
        System.out.println(StringUtils.colorizeForTerminal("Successfully loaded certificates", TerminalColor.GREEN));
        m_keyStore = loaded;
    }

    private boolean isCertCa(Certificate cert) {
        if (cert instanceof X509Certificate) {
            return ((X509Certificate) cert).getBasicConstraints() != -1;
        }
        return false;
    }

    public KeyStore getKeyStore() {
        return m_keyStore;
    }

    public String saveToDcmApiFormatFile(final String _pw) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final KeyStore keyStore = getKeyStore();
        final File dcmFile = TempFileManager.createTempFile(null);
        final KeyStore tgt = KeyStore.getInstance(PKCS_12);
        tgt.load(null, _pw.toCharArray());
        for (final String alias : Collections.list(keyStore.aliases())) {
            tgt.setCertificateEntry(alias, keyStore.getCertificate(alias));
        }
        try (FileOutputStream fos = new FileOutputStream(dcmFile, true)) {
            tgt.store(fos, _pw.toCharArray());
        }
        return dcmFile.getAbsolutePath();
    }
}
