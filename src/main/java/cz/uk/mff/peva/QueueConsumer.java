package cz.uk.mff.peva;

import org.agrona.concurrent.IdleStrategy;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 10/06/2017.
 */

public class QueueConsumer<T extends Runnable, Q extends Queue<T>>
        extends LeftPadding implements Runnable {

    private final Q queue;
    private final IdleStrategy idleStrategy;

    long p1, p2, p3, p4, p5, p6, p7;
    long p8, p9, p10, p11, p12, p13, p14;


    private volatile boolean running;
    private CountDownLatch stopLatch = new CountDownLatch(1);

    public QueueConsumer(Q queue, IdleStrategy idleStrategy) {
        this.queue = queue;
        this.idleStrategy = idleStrategy;
        this.running = false;
    }

    public void start() {
        running = true;
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

            runnable.run();
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
