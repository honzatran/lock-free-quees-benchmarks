package cz.uk.mff.peva;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by honza on 17/06/2017.
 */
public class ThreadCreator implements IThreadCreator {
    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    private final AtomicInteger producerIndex = new AtomicInteger(0);
    private final AtomicInteger consumerIndex = new AtomicInteger(0);

    @Override
    public Thread createProducer(Runnable r) {
        Thread thread = threadFactory.newThread(r);
        thread.setName("PRODUCER " + producerIndex.getAndIncrement());
        return thread;
    }

    @Override
    public Thread createConsumer(Runnable r) {
        Thread thread = threadFactory.newThread(r);
        thread.setName("CONSUMER " + consumerIndex.getAndIncrement());
        return thread;
    }
}
