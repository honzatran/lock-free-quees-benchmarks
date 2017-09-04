package cz.uk.mff.peva;

import org.agrona.concurrent.IdleStrategy;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class QueueProfilledConsumer<T extends Runnable, Q extends Queue<T>> extends LeftPadding implements Runnable {

    private final Q queue;
    private final IdleStrategy idleStrategy;

    private final int profileCount;
    private int runnableExecuted;

    private int profiled;
    private final int operationCount;

    long p1, p2, p3, p4, p5, p6, p7;
    long p8, p9, p10, p11, p12, p13, p14;

    private volatile boolean running;
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private ThreadPinnedRunnable threadPinnedRunnable;

    public QueueProfilledConsumer(Q queue, IdleStrategy idleStrategy, int profileCount, int operationCount, ThreadPinnedRunnable runnable) {
        this.queue = queue;
        this.idleStrategy = idleStrategy;
        this.profileCount = profileCount;
        this.runnableExecuted = 0;
        this.threadPinnedRunnable = runnable;
        this.profiled = 0;
        this.operationCount = operationCount;
    }

    public void start() {
        running = true;
        threadPinnedRunnable.onOperationStart();
    }

    @Override
    public void run() {
        main_loop:
        while (running) {
            T runnable = queue.poll();

            if (runnable == null) {
                idleStrategy.reset();

                while ((runnable = queue.poll()) == null) {
                    if (!running) {
                        break main_loop;
                    }

                    idleStrategy.idle();
                }
            }

            runnableExecuted++;

            runnable.run();

            if (runnableExecuted == profileCount) {
                threadPinnedRunnable.onOperationEnd();

                runnableExecuted = 0;

                profiled++;
                if (profiled == operationCount) {
                    threadPinnedRunnable.finishOperationsProfiling();
                    profiled = 0;
                }

                threadPinnedRunnable.onOperationStart();
            }
        }

        stopLatch.countDown();
    }

    public void stop() throws InterruptedException {
        running = false;

        if (!stopLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("STOP THREAD ERROR");
        }
    }
}
