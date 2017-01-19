package testtools;

/**
 * Created by assaf on 13/01/17.
 */
public class Logger {
    public synchronized static void log(String testName, String message) {
        System.out.println(testName + " => " + message);
        System.out.flush();
    }
}
