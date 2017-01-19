package tests;

import testtools.Logger;
import testtools.MultiThreadProcessTest;
import testtools.ProcessTest;
import testtools.TestManager;

/**
 * Created by assaf on 11/01/17.
 */
public class BasicTest {

    public static class BasicLoginAndDisconnectTest extends MultiThreadProcessTest {
        int step = 0;


        public BasicLoginAndDisconnectTest(String testName, String processPath, String[] args) {
            super(testName, processPath, args, "basic_one");
        }

        @Override
        public void nextStep() {
            String got;

            switch(step) {
                case 0:
                    Logger.log(getTestName(), "Executing step 0");
                    process.write("LOGRQ assaf");
                    got = process.read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("Login correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Login correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                case 1:
                    Logger.log(getTestName(), "Executing step 1");
                    process.write("DISC");
                    got = process.read();
                    if (got.equals("ACK 0")) {
                        addTestResult(new ProcessTest("Login correctly", "", "", true));
                    } else {
                        addTestResult(new ProcessTest("Login correctly", "ACK 0", "but got " + got, false));
                        endTest();
                    }
                    break;
                default:
                    Logger.log(getTestName(), "Ending test...");
                    endTest();
                    Logger.log(getTestName(), "Test ended");
            }

            step++;
        }

        @Override
        public long nextWait() {
            return 20000;
        }
    }

    public static TestManager getTestManager(String processPath, String[] args) {
        TestManager manager = new TestManager("BasicTest", 1);
        manager.add(new BasicLoginAndDisconnectTest("BasicTest Process 0", processPath, args));
        return manager;
    }
}
