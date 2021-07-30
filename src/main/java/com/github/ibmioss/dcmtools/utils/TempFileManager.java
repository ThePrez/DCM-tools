package com.github.ibmioss.dcmtools.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class TempFileManager {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cleanup();
            }
        });
    }
    private static final List<File> s_filesToCleanup = new LinkedList<File>();

    public static void cleanup() {
        for (final File f : s_filesToCleanup) {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
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
}
