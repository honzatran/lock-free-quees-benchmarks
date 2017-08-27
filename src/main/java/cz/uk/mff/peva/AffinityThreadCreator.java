package cz.uk.mff.peva;

import net.openhft.affinity.Affinity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by honza on 17/06/2017.
 */
public class AffinityThreadCreator implements IThreadCreator {
    public static final String PRODUCER_CPUS = "affinity.producers_cpu";
    public static final String CONSUMER_CPUS = "affinity.consumers_cpu";

    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private final List<Integer> producersCpus;
    private final List<Integer> consumersCpus;

    int producerIndex = 0;
    int consumerIndex = 0;

    public AffinityThreadCreator(final Config config) {
        this.producersCpus = config.getIntegers(PRODUCER_CPUS);
        this.consumersCpus = config.getIntegers(CONSUMER_CPUS);
    }

    @Override
    public Thread createProducer(final Runnable r) {
        final int cpu = getNextProducerCpu();

        if (r instanceof ThreadPinnedRunnable) {
            ((ThreadPinnedRunnable) r).setCpuId(cpu);
        }

        final Runnable affinityRunnable = getAffinityRunnable(r, cpu);
        return threadFactory.newThread(affinityRunnable);
    }


    @Override
    public Thread createConsumer(final Runnable r) {
        final int cpu = getNextConsumerCpu();

        final Runnable affinityRunnable = getAffinityRunnable(r, cpu);

        if (r instanceof ThreadPinnedRunnable) {
            ((ThreadPinnedRunnable) r).setCpuId(cpu);
        }

        return threadFactory.newThread(affinityRunnable);
    }

    @NotNull
    private Runnable getAffinityRunnable(Runnable r, int cpu) {
        return () -> {
            Affinity.setAffinity(cpu);
            System.out.println(Affinity.getCpu());
            r.run();
        };
    }

    private int getNextProducerCpu() {
        final int cpu = producersCpus.get(producerIndex);
        producerIndex++;

        if (producerIndex >= producersCpus.size()) {
            producerIndex = 0;
        }
        return cpu;
    }

    private int getNextConsumerCpu() {
        final int cpu = consumersCpus.get(consumerIndex);
        consumerIndex++;

        if (consumerIndex >= consumersCpus.size()) {
            consumerIndex = 0;
        }
        return cpu;
    }


}
