package cz.uk.mff.peva;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by honza on 10/06/2017.
 */


public class ThreadLocalState {
    static abstract class LeftPadding {
        long p1; long p2; long p3; long p4; long p5; long p6; long p7; long p8;
        long p11; long p12; long p13; long p14; long p15; long p16; long p17;
    }

    public static class LastTask extends LeftPadding implements Runnable {
        private final AtomicBoolean flag = new AtomicBoolean(false);

        long p1; long p2; long p3; long p4; long p5; long p6; long p7; long p8;
        long p11; long p12; long p13; long p14; long p15; long p16; long p17; long p18;

        @Override
        public void run() {
            flag.lazySet(true);
        }
    }

    public static class EmptyTask implements Runnable {
        @Override
        public void run() {
        }
    }

    private final LastTask lastTask;
    private final EmptyTask emptyTask;

    public ThreadLocalState() {
        this.lastTask = new LastTask();
        this.emptyTask = new EmptyTask();
    }

    public LastTask getLastTask() {
        return lastTask;
    }

    public EmptyTask getEmptyTask() {
        return emptyTask;
    }

    public void resetLastTask() {
        this.lastTask.flag.lazySet(false);
    }

    public void waitForLastTaskExecution() {
        while (!lastTask.flag.get()) {
        }
    }
}
