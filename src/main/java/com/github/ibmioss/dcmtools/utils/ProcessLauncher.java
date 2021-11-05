package com.github.ibmioss.dcmtools.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import com.github.ibmioss.dcmtools.utils.StringUtils.TerminalColor;

public class ProcessLauncher {

    public static class ProcessResult {
        private final int m_exitStatus;
        private final List<String> m_stderr;
        private final List<String> m_stdout;

        public ProcessResult(final List<String> m_stdout, final List<String> m_stderr, final int m_exitStatus) {
            super();
            this.m_stdout = m_stdout;
            this.m_stderr = m_stderr;
            this.m_exitStatus = m_exitStatus;
        }

        public int getExitStatus() {
            return m_exitStatus;
        }

        public List<String> getStderr() {
            return m_stderr;
        }

        public List<String> getStdout() {
            return m_stdout;
        }

        public void prettyPrint() {
            for (final String stdout : m_stdout) {
                System.out.println(StringUtils.colorizeForTerminal(stdout, TerminalColor.GREEN));
            }
            for (final String stderr : m_stderr) {
                System.out.println(StringUtils.colorizeForTerminal(stderr, TerminalColor.BRIGHT_RED));
            }
        }

    }

    public static ProcessResult exec(final String _cmd) throws UnsupportedEncodingException, IOException {
        final Process p = Runtime.getRuntime().exec(_cmd);
        final List<String> stdout = new LinkedList<String>();
        final List<String> stderr = new LinkedList<String>();
        final Thread stderrThread = new Thread() {
            @Override
            public void run() {

                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), "UTF-8"))) {
                    String line;
                    while (null != (line = br.readLine())) {
                        stderr.add(line);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            };
        };
        stderrThread.setDaemon(true);
        stderrThread.start();
        p.getOutputStream().close();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            while (null != (line = br.readLine())) {
                stdout.add(line);
            }
        }
        int rc;
        try {
            rc = p.waitFor();
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }
        return new ProcessResult(stdout, stderr, rc);
    }
}
