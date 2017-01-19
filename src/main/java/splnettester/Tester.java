package splnettester;

import tests.BasicTest;
import tests.UploadDownloadFileTest;
import testtools.MultiThreadProcessTest;
import testtools.ProcessRunner;
import testtools.ProcessTest;
import testtools.TestManager;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by assaf on 11/01/17.
 */
public class Tester {
    private static final String clientPath = "/home/assaf/Documents/Dev/spl/net/Client/bin/TFTPclient";
    private static final String serverPath = "/home/assaf/Documents/Dev/spl/net/Server";
    private static final String[] clientArgs = new String[] {"127.0.0.1", "7777"};
    private  static final ArrayList<TestManager> tests = new ArrayList<>();
    private static ProcessRunner runner = null;

    private static void initTests() throws NoSuchAlgorithmException {
        tests.add(BasicTest.getTestManager(clientPath, clientArgs));
        tests.add(UploadDownloadFileTest.getTestManager(clientPath, clientArgs, 1, 10));
        tests.add(UploadDownloadFileTest.getTestManager(clientPath, clientArgs, 20, 10));
        tests.add(UploadDownloadFileTest.getTestManager(clientPath, clientArgs, 30, 10));
        tests.add(UploadDownloadFileTest.getTestManagerEmptyFile(clientPath, clientArgs, 1, 1));
        tests.add(UploadDownloadFileTest.getTestManagerEmptyFile(clientPath, clientArgs, 20, 10));
        tests.add(UploadDownloadFileTest.getTestManagerEmptyFile(clientPath, clientArgs, 30, 10));
        tests.add(UploadDownloadFileTest.getTestManager512File(clientPath, clientArgs, 1, 1));
        tests.add(UploadDownloadFileTest.getTestManager512File(clientPath, clientArgs, 20, 10));
        tests.add(UploadDownloadFileTest.getTestManager512File(clientPath, clientArgs, 30, 10));
    }

    private static void printResult(TestManager manager) throws InterruptedException {
        String testName = manager.getTestName();
        ArrayList<MultiThreadProcessTest> tests = manager.getTests();

        System.out.println("******************" + testName + "**********************");
        int failCounter = 0;
        int successCounter = 0;
        for (MultiThreadProcessTest t : tests) {
            ConcurrentLinkedQueue<ProcessTest> subTests = t.getTests();
            ProcessTest subTest;
            if (t.getErrors() != 0) { System.out.print("\t" + t.getTestName()); System.out.println(" FAILED");  }
            while((subTest = subTests.poll()) != null) {
                if (!subTest.isSuccess()) {
                    failCounter++;
                    System.out.println("\t\tTest " + subTest.getTestName() + " failed => Expected " + subTest.getExpected() + " " + subTest.getHowever());
                    Thread.sleep(500);
                    if (subTest.getE() != null) { subTest.getE().printStackTrace(); }
                } else {
                    successCounter++;
                }
            }
        }

        if (failCounter == 0) { System.out.println("\tAll " + successCounter + " tests passed!!"); }
        else { System.out.println("\tTotal: " + (successCounter + failCounter) + ", Green: " + successCounter + ", Red: " + failCounter); }
    }

    private static void runTest(TestManager manager) throws InterruptedException {
        String testName = manager.getTestName();
        System.out.println("Running " + testName );
        manager.run();
        System.out.println("Done running " + testName );
    }

    private static void stopServer() {
        runner.close(2000);
    }

    public static void main(String[] args) throws Exception {
        initTests();
        for (TestManager t : tests) {
            runTest(t);
        }

        for (TestManager t : tests) {
            printResult(t);
        }

        System.exit(0);
    }
}
