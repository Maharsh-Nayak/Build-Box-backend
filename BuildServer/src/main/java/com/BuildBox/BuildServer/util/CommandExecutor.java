package com.BuildBox.BuildServer.util;


import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandExecutor {

    public static void run(String command) throws Exception {
        String[] cmdArray;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            cmdArray = new String[]{"cmd", "/c", command};
        } else {
            cmdArray = new String[]{"sh", "-c", command};
        }

        Process process = Runtime.getRuntime().exec(cmdArray);
        
        // Read input stream (normal output)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // Read error stream
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        process.waitFor();

        if (process.exitValue() != 0) {
            throw new RuntimeException("Command failed with exit code " + process.exitValue() + ": " + command);
        }
    }
}
