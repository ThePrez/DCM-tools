package com.github.ibmioss.dcmtools.utils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class FileUtils {
    public static void moveToWithBackup(final String _src, final String _dest, final boolean _deleteBackup) throws IOException {
        
        final File destFile = new File(_dest);
        final File srcFile = new File(_src);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ssZ");
        String backupFileName = destFile.getAbsolutePath()+simpleDateFormat.format(new Date())+".backup";
        final File backupFile = new File(backupFileName);
        rename(destFile, backupFile);
        rename(srcFile, destFile);
        if(_deleteBackup) {
            delete(backupFile);
        }
    }

    public static void delete(File _f) throws IOException {
        boolean wasSuccessful = _f.delete();
        if(!wasSuccessful) {
            throw new IOException(String.format("Unable to delete file '%s'.", _f.getAbsolutePath()));
        }
    }

    public static void rename(File _f, File _newName) throws IOException {
        boolean wasSuccessful = _f.renameTo(_newName);
        if(!wasSuccessful) {
            throw new IOException(String.format("Unable to rename file '%s' to '%s'.", _f.getAbsolutePath(), _newName.getAbsolutePath()));
        }
    }

}
