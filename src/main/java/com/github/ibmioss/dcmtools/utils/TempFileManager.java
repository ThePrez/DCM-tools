package com.github.ibmioss.dcmtools.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.theprez.jcmdutils.StringUtils;

public class TempFileManager {

    private static final List<File> s_dirsToCleanup = new LinkedList<File>();
    private static final List<File> s_filesToCleanup = new LinkedList<File>();
    public static final String TEMP_KEYSTORE_PWD = StringUtils.generateRandomString(10);
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cleanup();
            }
        });
    }

    public static void cleanup() {
        for (final File f : s_filesToCleanup) {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
        for (final File f : s_dirsToCleanup) {
            rmdir(f);
        }
    }

    private static long copy(final OutputStream _dest, final InputStream _src) throws IOException {
        final byte[] buf = new byte[16 * 1024];
        int bytesRead = 0;
        long totalBytesRead = 0;
        while (-1 != (bytesRead = _src.read(buf))) {
            totalBytesRead += bytesRead;
            _dest.write(buf, 0, bytesRead);
        }
        return totalBytesRead;
    }

    public static File createTempDir() throws IOException {
        final File dotDir = new File(System.getProperty("user.home", "~"), ".dcmimport");
        dotDir.mkdirs();
        final File ret = File.createTempFile(".dcmimport", ".file", dotDir);
        ret.delete();
        if (!ret.mkdir()) {
            throw new IOException("Unable to create temporary directory");
        }
        ret.deleteOnExit();
        s_dirsToCleanup.add(ret);
        return ret;
    }

    public static File createTempFile() throws IOException {
        return createTempFile(null);
    }

    public static File createTempFile(final String _fileName) throws IOException {
        final File dotDir = new File(System.getProperty("user.home", "~"), ".dcmimport");
        dotDir.mkdirs();
        final File ret;
        if (null == _fileName) {
            ret = File.createTempFile(".dcmimport", ".file", dotDir);
        } else {
            ret = new File(dotDir, _fileName);
            ret.createNewFile();
        }
        ret.deleteOnExit();
        s_filesToCleanup.add(ret);
        return ret;
    }

    private static void rmdir(final File _dir) {
        for (final File innerFile : _dir.listFiles()) {
            if (innerFile.isDirectory()) {
                rmdir(innerFile);
            }
        }
        if (!_dir.delete()) {
            _dir.deleteOnExit();
        }

    }

    public static List<File> unzip(final String _zipFile) throws FileNotFoundException, IOException {
        final List<File> ret = new LinkedList<File>();
        final File baseDir = createTempDir();
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(_zipFile))) {
            ZipEntry entry = null;
            while (null != (entry = zip.getNextEntry())) { // NOPMD by jkw on 8/8/13 2:47 PM "Avoid assignments in operands" in some cases we don't mind 'em
                if (0 == entry.getName().length()) {
                    continue;
                }
                final File destFile = new File(baseDir.getAbsolutePath() + "/" + entry.getName());
                destFile.getParentFile().mkdirs();
                s_filesToCleanup.add(destFile);
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
                    copy(bos, zip);
                }
                ret.add(destFile);
            }
        }
        return ret;
    }
}
