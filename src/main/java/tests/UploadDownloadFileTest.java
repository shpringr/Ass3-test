package tests;

import org.apache.commons.io.FileUtils;
import testtools.Logger;
import testtools.MultiThreadProcessTest;
import testtools.ProcessTest;
import testtools.TestManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by assaf on 13/01/17.
 */
public class UploadDownloadFileTest extends MultiThreadProcessTest {
    int step = 0;
    int phase = 0;
    int ack = 0;
    String filename;
    String workingDirectory;
    int otherBcasts = 0;
    String fileDownload;
    String fileDownloadTitle;
    int expectedAcks;

    public UploadDownloadFileTest(String testName, String processPath, String[] args, String workingDirectory,
                                  String fileDownload, String fileDownloadTitle, int expectedAcks) throws NoSuchAlgorithmException {
        super(testName, processPath, args, workingDirectory);
        filename = testName;
        this.fileDownload = fileDownload;
        this.fileDownloadTitle = fileDownloadTitle;
        this.expectedAcks = expectedAcks;
        this.workingDirectory = workingDirectory;
    }

    private String read() {
        String got = process.read();
        while (got.contains("BCAST") && !(got + " ").contains(filename + " ")) {
            got = process.read();
            otherBcasts++;
            Logger.log(getTestName(), "************* Got so far " + otherBcasts + " bcasts from others...");
        }
        return got;
    }

    private void handleFileUpload() {
        for (int i = 0; i < 2000; i++) {
            if (ack > expectedAcks) { break; }
            Logger.log(getTestName(), "Waiting for ack " + ack + "...");
            String got = read();
            if (got.equals("ACK " + ack)) {
                addTestResult(new ProcessTest("WRQ ACK correctly", "", "", true));
            } else {
                addTestResult(new ProcessTest("WRQ ACK correctly", "ACK " + ack, "but got " + got, false));
                endTest();
                break;
            }

            ack++;
            step++;
        }

        step--;
    }

    private void handleEndFileUpload() {
        String got;

        if (step == expectedAcks + 3) {
            Logger.log(getTestName(), "Waiting for WRQ complete...");
            got = read();
            if (got.equals("WRQ " + filename + " complete")) {
                addTestResult(new ProcessTest("WRQ complete correctly", "", "", true));
            } else {
                addTestResult(new ProcessTest("WRQ complete correctly", "WRQ " + filename + " complete ", "but got " + got, false));
                endTest();
            }
        } else if (step == expectedAcks + 4) {
            Logger.log(getTestName(), "Waiting for BCAST...");
            got = read();
            if (got.equals("BCAST add " + filename)) {
                addTestResult(new ProcessTest("BCAST correctly", "", "", true));
            } else {
                addTestResult(new ProcessTest("BCAST correctly", "BCAST add " + filename, "but got " + got, false));
                endTest();
            }
        }
    }

    private void handlePhase1() {
        String got;
        try {
            switch(step) {
                case 0:
                    Logger.log(getTestName(), "Copying " + fileDownloadTitle + " to working directory...");
                    Files.copy(Paths.get("resources", fileDownload), Paths.get("./" + workingDirectory, filename), REPLACE_EXISTING);
                    break;
                case 1:
                    Logger.log(getTestName(), "Logging in...");
                    process.write("LOGRQ assaf" + filename);
                    got = read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("Login correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Login correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                case 2:
                    ack = 1;
                    Logger.log(getTestName(), "Sending file " + filename + "...");
                    process.write("WRQ " + filename);
                    got = read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("WRQ correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("WRQ correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                default:
                    if (step > 2 && step <= expectedAcks + 2) { handleFileUpload(); }
                    else if (step > 2 && step < expectedAcks + 5) { handleEndFileUpload(); }
                    else {
                        phase++;
                        step = -1;
                    }
            }
        } catch (Exception e) {
            addTestResult(new ProcessTest("No exception", "Not to throw an exception on test", "but got one", false, e));
            endTest();
        }

        step++;
    }

    void handlePhase2() {
        String got;

        try {
            switch(step) {
                case 0:
                    Logger.log(getTestName(), "Copying " + filename + " for comparing...");
                    Files.copy(Paths.get("./" + workingDirectory, filename), Paths.get("./" + workingDirectory, "temp"), REPLACE_EXISTING);
                    break;
                case 1:
                    Logger.log(getTestName(), "Requesting RRQ " + filename + "...");
                    process.write("RRQ " + filename);
                    break;
                case 2:
                    Logger.log(getTestName(), "Waiting for RRQ complete...");
                    got = read();
                    if (got.equals("RRQ " + filename + " complete")) {
                        addTestResult(new ProcessTest("RRQ complete correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("RRQ complete correctly", "RRQ " + filename + " complete ", "but got " + got, false));
                        endTest();
                    }
                    break;
                case 3:
                    File file1 = Paths.get("./" + workingDirectory, filename).toFile();
                    File file2 = Paths.get("./" + workingDirectory, "temp").toFile();
                    boolean isTwoEqual = FileUtils.contentEquals(file1, file2);
                    if (isTwoEqual) {
                        addTestResult(new ProcessTest("RRQ to equal original file", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("RRQ to equal original file", "RRQ to return same file", "but contents aren't equal", false));
                        endTest();
                    }
                    break;
                case 4:
                    Logger.log(getTestName(), "Deleting uploaded file...");
                    process.write("DELRQ " + filename);
                    got = read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("Deleted correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Deleted correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                case 5:
                    Logger.log(getTestName(), "Waiting for DEL bcast...");
                    got = read();
                    if (got.equals("BCAST del " + filename)) {
                        addTestResult(new ProcessTest("Deleted correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Deleted correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                default:
                    Logger.log(getTestName(), "Disconnecting...");
                    process.write("DISC");
                    got = read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("Login correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Login correctly", "ACK 0", "but got " + got, false));
                    }

                    Logger.log(getTestName(), "Ending test...");
                    endTest();
                    Logger.log(getTestName(), "Test ended");

            }
        } catch (Exception e) {
            addTestResult(new ProcessTest("No exception", "Not to throw an exception on test", "but got one", false, e));
            endTest();
        }

        step++;
    }

    @Override
    public void nextStep() {
        if (phase == 0) {
            handlePhase1();
        } else {
            handlePhase2();
        }
    }

    @Override
    public long nextWait() {
        return 50000;
    }

    public static TestManager getTestManager(String processPath, String[] args, int clients, int threads) throws NoSuchAlgorithmException {
        TestManager manager = new TestManager("UploadDownloadFileTest: " + clients + " Parallel clients", threads);
        for (int i = 0; i < clients; i++) {
            manager.add(new UploadDownloadFileTest("UploadDownloadFileTestProcess" + i, processPath, args, "upload" + i,
                                                    "DanielLevi-lenvie-daimer.mp3","L'envie d'aimer", 14442));
        }
        return manager;
    }

    public static TestManager getTestManagerEmptyFile(String processPath, String[] args, int clients, int threads) throws NoSuchAlgorithmException {
        TestManager manager = new TestManager("UploadDownloadFileEmptyTest: " + clients + " Parallel clients", threads);
        for (int i = 0; i < clients; i++) {
            manager.add(new UploadDownloadFileTest("UploadDownloadFileEmptyTest" + i, processPath, args, "eupload" + i,
                    "emptyfile","emptyfile", 1));
        }
        return manager;
    }

    public static TestManager getTestManager512File(String processPath, String[] args, int clients, int threads) throws NoSuchAlgorithmException {
        TestManager manager = new TestManager("getTestManager512FileTest: " + clients + " Parallel clients", threads);
        for (int i = 0; i < clients; i++) {
            manager.add(new UploadDownloadFileTest("getTestManager512FileTest" + i, processPath, args, "5upload" + i,
                    "5file.txt","512*2 bytes file", 3));
        }
        return manager;
    }
}
