package testtools;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by assaf on 11/01/17.
 */
public class TestManager {

    ArrayList<MultiThreadProcessTest> tests = new ArrayList<>();
    String testName;
    int threads;
    ExecutorService service;

    public TestManager(String testName, int threads) {
        this.testName = testName;
        this.threads = threads;
    }

    public void add(MultiThreadProcessTest test) {
        this.tests.add(test);
    }

    public String getTestName() {
        return testName;
    }

    public ArrayList<MultiThreadProcessTest> getTests() {
        return tests;
    }

    void waitOnFutures(ArrayList<Pair<Future<Boolean>, Long>> futures) throws InterruptedException {
        int i = 0;
        for (Pair<Future<Boolean>, Long> p : futures) {
            Future<Boolean> f = p.getKey();
            if (f == null) { i++; continue; }
            Long timeout = p.getValue();
            MultiThreadProcessTest test = tests.get(i);
            Long startedAt = test.getStartedAt() < 0 ? 0 : test.getStartedAt();

            long diff = Calendar.getInstance().getTimeInMillis() - startedAt;
            while(!f.isDone() && (diff < timeout || startedAt == 0)) { diff = Calendar.getInstance().getTimeInMillis() - startedAt; Thread.sleep(500); }
            if (!f.isDone()) {
                f.cancel(true);
                test.addTestResult(new ProcessTest(test.getTestName(), "to end on time", "but did not", false));
                test.endQuietly();
            }
            i++;
        }
    }

    boolean runPhase() throws InterruptedException {
        boolean allEnded = true;
        ArrayList<Pair<Future<Boolean>, Long>> futures = new ArrayList<>();
        for (MultiThreadProcessTest t : tests) {
            if (t.isEnded()) { futures.add(new Pair<Future<Boolean>, Long>(null, (long)0)); continue; }
            long timeout = t.nextWait();
            Future<Boolean> f = service.submit(t);
            futures.add(new Pair<>(f, timeout));
            allEnded = false;
        }

        waitOnFutures(futures);

        return !allEnded;
    }

    public void run() throws InterruptedException {
        service = Executors.newFixedThreadPool(threads);
        while(runPhase()) {};
        service.shutdown();
        service.awaitTermination(5000, TimeUnit.MILLISECONDS);
    }
}
