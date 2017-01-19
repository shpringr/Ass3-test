package testtools;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by assaf on 11/01/17.
 */
public class ProcessRunner {
    String processPath;
    String[] args;
    Process process;
    BufferedReader reader;
    BufferedWriter writer;

    public int getExitValue() {
        return exitValue;
    }

    int exitValue = -1;

    public ProcessRunner(String processPath, String[] args) {
        this.processPath = processPath;
        this.args = args.clone();
    }

    public void start(long timeout, String workingDirectory, boolean redirectStdoutToMine) throws Exception {
        try {
            ArrayList<String> command = new ArrayList<>();
            command.add(processPath);
            for (String s : args) { command.add(s); }

            ProcessBuilder builder = new ProcessBuilder(command.toArray(new String[args.length + 1]));
            builder.redirectErrorStream(true);
            builder.directory(new File(workingDirectory));
            if (redirectStdoutToMine) { builder.redirectOutput(ProcessBuilder.Redirect.INHERIT); }

            process = builder.start();

            OutputStream stdin = process.getOutputStream ();
            InputStream stdout = process.getInputStream ();

            reader = new BufferedReader (new InputStreamReader(stdout));
            writer = new BufferedWriter(new OutputStreamWriter(stdin));
        } catch (Exception e) {
            if (process != null) { close(timeout); }
            throw e;
        }
    }

    public boolean write(String input) {
        try {
            input += "\n";
            writer.write(input);
            writer.flush();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public String read() {
        try {
            String line = reader.readLine();
            return line;
        } catch (IOException e) {
            return "";
        }
    }

    public void close(long timeout) {
        try {
            if (reader!= null) { reader.close(); }
            if (writer != null) { writer.close(); }
            if (process != null && process.isAlive()) { process.waitFor(4000, TimeUnit.MILLISECONDS); process.destroy(); }
            process.waitFor(3500, TimeUnit.MILLISECONDS);
            if (process != null) { exitValue = process.exitValue(); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
