package testtools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by assaf on 11/01/17.
 */
public abstract class MultiThreadProcessTest implements Callable<Boolean> {
    String testName;
    String processPath;
    String[] args;
    String workingDirectory;
    protected ProcessRunner process;
    ConcurrentLinkedQueue<ProcessTest> tests = new ConcurrentLinkedQueue<>();
    AtomicInteger errors = new AtomicInteger();
    AtomicInteger success = new AtomicInteger();
    boolean started = false;
    boolean ended = false;
    protected long basicTimeout = 3000;
    long startedAt = -1;

    public MultiThreadProcessTest(String testName, String processPath, String[] args, String workingDirectory) {
        this.testName = testName;
        this.processPath = processPath;
        this.args = args;
        this.workingDirectory = workingDirectory;
    }

    public synchronized long getStartedAt() { return startedAt; }

    public int getErrors() {
        return errors.get();
    }

    public int getSuccess() {
        return success.get();
    }

    public ConcurrentLinkedQueue<ProcessTest> getTests() {
        return tests;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isEnded() {
        return ended;
    }

    public String getTestName() {
        return testName;
    }

    public abstract void nextStep();

    public abstract long nextWait();

    public void tryDeleteWorkingDir() {
        try {
            File index = new File(workingDirectory);
            String[]entries = index.list();
            if (entries != null) {
                for(String s: entries){
                    File currentFile = new File(index.getPath(),s);
                    currentFile.delete();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void endQuietly() {
        this.ended = true;
    }

    public void endTest() {
        process.close(basicTimeout);
        if (process.getExitValue() != 0) {
            this.tests.add(new ProcessTest(testName, processPath + " to end properly", "but got error code on exit => " + process.getExitValue(), false));
        }
        ended = true;
    }

    private void start()  {
        try {
            tryDeleteWorkingDir();
            Path path = Paths.get(workingDirectory);
            Files.createDirectories(path);
            process = new ProcessRunner(processPath, args);
            process.start(basicTimeout, workingDirectory, false);
            started = true;
        } catch (Exception ex) {
            this.tests.add(new ProcessTest(testName, processPath + " to start properly", "but failed on start", false, ex));
            endTest();
        }
    }

    public void addTestResult(ProcessTest test) {
        this.tests.add(test);
        if (test.isSuccess()) { success.incrementAndGet(); }
        else { errors.incrementAndGet(); }
    }

    public boolean testStatus() {
        return this.errors.get() != 0;
    }

    public Boolean call() {
        try {
            synchronized (this) { startedAt = Calendar.getInstance().getTimeInMillis(); }
            if (!started) { start(); }
            if (started) {
                nextStep();
            }
        } catch (Exception ex) {
            this.tests.add(new ProcessTest(testName, "not to get an exception running process", "but did got one", false, ex));
            errors.incrementAndGet();
        }

        return testStatus();
    }
}
