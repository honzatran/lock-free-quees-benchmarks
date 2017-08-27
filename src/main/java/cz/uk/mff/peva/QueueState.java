package cz.uk.mff.peva;



import org.agrona.concurrent.IdleStrategy;

import java.util.Queue;

/**
 * Created by honza on 11/06/2017.
 */
public class QueueState<T extends Runnable, Q extends Queue<T>> {
    private QueueConsumer<T, Q> queueConsumer;
    private Thread consumerThread;

    public void startConsumer(Q queue, IdleStrategy idleStrategy) {
        this.queueConsumer = new QueueConsumer<>(queue, idleStrategy);
        this.consumerThread = new Thread(queueConsumer);

        queueConsumer.start();
        consumerThread.start();
    }

    public void stopConsumer() throws InterruptedException {
        queueConsumer.stop();
        consumerThread.join();
    }
}
