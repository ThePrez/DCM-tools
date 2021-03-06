package com.github.ibmioss.dcmtools.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.ProcessLauncher;
import com.github.theprez.jcmdutils.ProcessLauncher.ProcessResult;
import com.github.theprez.jcmdutils.StringUtils;

public class KeyStoreLoader {
    private static final String PKCS_12 = "PKCS12";

    public static String extractTrustFromInstalledCerts(final AppLogger _logger) throws IOException {
        final File destFile = TempFileManager.createTempFile(null);
        destFile.delete();
        final ProcessResult cmdResults = ProcessLauncher.exec("/QOpenSys/pkgs/bin/trust extract --format=java-cacerts --purpose=server-auth -v " + destFile.getAbsolutePath());
        if (0 != cmdResults.getExitStatus()) {
            for (final String errLine : cmdResults.getStderr()) {
                _logger.println_err(errLine);
            }
            throw new IOException("Error extracting trusted certificates");
        }
        _logger.println_success("Successfully extracted installed certificates");
        return destFile.getAbsolutePath(); // TODO: delete this file!
    }

    private final KeyStore m_keyStore;

    public KeyStoreLoader(final KeyStore _ks) {
        m_keyStore = _ks;
    }

    public KeyStoreLoader(final AppLogger _logger, final List<String> _files, final String _pw, final String _label, final boolean _caOnly) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        final List<String> filesToLoad = new LinkedList<String>();
        filesToLoad.addAll(_files);

        // TODO: if .zip file, unzip and process files individually
        for (final String file : _files) {
            final File f = new File(file);
            if (f.isDirectory()) {
                for (final File innerFile : f.listFiles()) {
                    filesToLoad.add(innerFile.getAbsolutePath());
                }
            } else if (file.toLowerCase().endsWith(".zip")) {
                for (final File innerFile : TempFileManager.unzip(file)) {
                    filesToLoad.add(innerFile.getAbsolutePath());
                }
            }
        }

        final String[] keystoreTypes = new String[] { KeyStore.getDefaultType(), "JKS", "PKCS12", "JCEKS", "PKCS12V3" };
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        boolean isKeyStoreLoaded = false;

        for (final String file : filesToLoad) {
            boolean isFileLoaded = false;

            // skip directories and .zip files (we already added their contents to the main list)
            if (new File(file).isDirectory() || file.toLowerCase().endsWith(".zip")) {
                continue;
            }
            // Try to load as keystore file
            for (final String keystoreType : keystoreTypes) {
                KeyStore fileKs = null;
                try (FileInputStream fis = new FileInputStream(file)) {
                    fileKs = KeyStore.getInstance(keystoreType);
                    fileKs.load(fis, null == _pw ? null : _pw.toCharArray());
                } catch (final Throwable e) {
                    continue;
                }
                if (null != fileKs) {
                    keyStore = CertUtils.mergeKeyStore(keyStore, fileKs);
                    isFileLoaded = true;
                    isKeyStoreLoaded = true;
                    break;
                }
            }
            // That didn't work! Try to load as a certificate file
            if (!isFileLoaded) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    final Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(fis);
                    if (!certs.isEmpty()) {
                        int counter = 1;
                        for (final Certificate cert : certs) {
                            if (_caOnly && !isCertCa(cert)) {
                                continue;
                            }
                            final String aliasBase = StringUtils.isEmpty(_label) ? new File(file).getName().replaceFirst("[.][^.]+$", "") : _label.trim();
                            final String aliasSuffix = (1 == counter) ? "" : "." + counter;
                            counter++;
                            final String alias = aliasBase + aliasSuffix;
                            keyStore.setCertificateEntry(alias, cert);
                            isFileLoaded = true;
                        }
                    }
                } catch (final Throwable e) {
                }
            }
            if (!isFileLoaded) {
                _logger.println_warn("WARNING: File " + new File(file).getName() + " will not be processed. It is in an unsupported format");
            }
        }
        // Out of ideas
        if (!keyStore.aliases().hasMoreElements() && !isKeyStoreLoaded) {
            throw new IOException("Failure loading certificates");
        }
        _logger.println_verbose("Successfully loaded certificates");
        m_keyStore = keyStore;
    }

    public KeyStore getKeyStore() {
        return m_keyStore;
    }

    private boolean isCertCa(final Certificate cert) {
        if (cert instanceof X509Certificate) {
            return ((X509Certificate) cert).getBasicConstraints() != -1;
        }
        return false;
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
