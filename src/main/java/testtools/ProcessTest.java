package testtools;

/**
 * Created by assaf on 11/01/17.
 */
public class ProcessTest {
    String testName;
    String expected;
    String however;
    boolean success = false;
    Throwable e;

    public ProcessTest(String testName, String expected, String however, boolean success) {
        this.testName = testName;
        this.expected = expected;
        this.however = however;
        this.success = success;
    }

    public ProcessTest(String testName, String expected, String however, boolean success, Throwable e) {
        this.testName = testName;
        this.expected = expected;
        this.however = however;
        this.success = success;
        this.e = e;
    }

    public Throwable getE() {
        return e;
    }

    public String getTestName() {
        return testName;
    }

    public String getExpected() {
        return expected;
    }

    public String getHowever() {
        return however;
    }

    public boolean isSuccess() {
        return success;
    }
}
