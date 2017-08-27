package cz.uk.mff.peva.throughput;

import cz.uk.mff.peva.latency.JCToolsBenchmark;
import org.jctools.queues.SpscArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 21/04/2017.
 */


@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JCToolBenchmark {
    public static final Long ONE = 1L;

    @Param(value = "132000")
    public static int qCapacity;


    @State(Scope.Thread)
    @AuxCounters
    public static class PollCounters {
        public int offerSuccess;
        public int offerFailure;
    }

    @State(Scope.Group)
    public static class SingleConsumerQueueState {
        SpscArrayQueue<Long> queue;

        @Setup(Level.Trial)
        public void setUp() {
            this.queue = new SpscArrayQueue<>(qCapacity);
        }
    }


    @Group("Q")
    @Benchmark
    public Long take(SingleConsumerQueueState state) throws InterruptedException {
        return state.queue.poll();
    }

    @Group("Q")
    @GroupThreads(1)
    @Benchmark
    public void put(SingleConsumerQueueState state, PollCounters pollCounters) throws InterruptedException {
        if (state.queue.offer(ONE)) {
            pollCounters.offerSuccess++;
        } else {
            pollCounters.offerFailure++;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JCToolsBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(2)
                .forks(5)
                .timeout(TimeValue.seconds(5))
                .build();

        new Runner(opt).run();
    }
}
