package com.github.ibmioss.dcmtools.utils;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleUtils {
    static final Console g_console = System.console();

    public static String askUser(final String _question) throws IOException {
        if (null == g_console) {
            System.out.print(_question);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {

                return br.readLine();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            return g_console.readLine("%s", _question);
        }
    }

    public static String askUserForPwd(String _prompt) throws IOException {
        if (null == g_console) {
            throw new IOException("Can't securely ask for password property).");
        } else {
            char[] pw = g_console.readPassword(_prompt);
            if (null == pw) {
                throw new IOException("Password not entered");
            }
            return new String(pw);
        }
    }

    public static String askUserOrThrow(final String _question) throws IOException {
        String resp = askUser(_question);
        if (StringUtils.isEmpty(resp)) {
            throw new IOException("No response provided");
        }
        return resp.trim();
    }

    public static String askUserWithDefault(final String _question, final String _default) throws IOException {
        String response = askUser(_question).trim();
        return StringUtils.isEmpty(response) ? _default : response;
    }
}
