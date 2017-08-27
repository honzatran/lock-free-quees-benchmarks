package cz.uk.mff.peva.latency;


import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import cz.uk.mff.peva.ThreadLocalState;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.BenchmarkMode;

import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 10/06/2017.
 */

@State(Scope.Benchmark)
public class DisruptorLatencyBenchmarks {

    @Param({"1", "100"})
    public static int burstSize;

    private static class RunnableHolder {
        Runnable runnable;
    }

    private static class RunnableHandler implements EventHandler<RunnableHolder> {

        @Override
        public void onEvent(
                RunnableHolder event,
                long sequence,
                boolean endOfBatch) throws Exception {

            event.runnable.run();
        }
    }

    private Disruptor<RunnableHolder> disruptor = createDisruptor(ProducerType.MULTI);

    @Setup(Level.Trial)
    public void startDisruptor() {
        disruptor.handleEventsWith(new RunnableHandler());
        disruptor.start();
    }

    @TearDown(Level.Trial)
    public void stopDisruptor() {
        disruptor.shutdown();
    }

    @State(Scope.Benchmark)
    public static class SingleProducerDisruptorProvider {
        Disruptor<RunnableHolder> disruptor = createDisruptor(ProducerType.SINGLE);

        @Setup(Level.Trial)
        public void startDisruptor() {
            disruptor.handleEventsWith(new RunnableHandler());
            disruptor.start();
        }

        @TearDown(Level.Trial)
        public void stopDisruptor() {
            disruptor.shutdown();
        }
    }

    @State(Scope.Thread)
    public static class SingleProducerThreadState extends ThreadLocalState {
        RingBuffer<RunnableHolder> ringBuffer;

        @Setup(Level.Trial)
        public void setUp(SingleProducerDisruptorProvider disruptorProvider) {
            resetLastTask();
            this.ringBuffer = disruptorProvider.disruptor.getRingBuffer();
        }
    }


    @State(Scope.Thread)
    public static class ProducerThreadState extends ThreadLocalState {
        RingBuffer<RunnableHolder> ringBuffer;

        @Setup(Level.Trial)
        public void setUp(DisruptorLatencyBenchmarks benchmarks) {
            resetLastTask();
            this.ringBuffer = benchmarks.disruptor.getRingBuffer();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    public void oneProducer(SingleProducerThreadState state) {
        final RingBuffer<RunnableHolder> ringBuffer = state.ringBuffer;

        sendBurst(ringBuffer, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    @Threads(2)
    public void twoProducecers(ProducerThreadState state) {
        final RingBuffer<RunnableHolder> ringBuffer = state.ringBuffer;

        sendBurst(ringBuffer, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    @Threads(4)
    public void fourProducecers(ProducerThreadState state) {
        final RingBuffer<RunnableHolder> ringBuffer = state.ringBuffer;

        sendBurst(ringBuffer, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    public static void sendBurst(
            final RingBuffer<RunnableHolder> ringBuffer,
            final ThreadLocalState.EmptyTask emptyTask,
            final ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            long seq = ringBuffer.next();
            ringBuffer.get(seq).runnable = emptyTask;
            ringBuffer.publish(seq);

        }

        long seq = ringBuffer.next();
        ringBuffer.get(seq).runnable = lastTask;
        ringBuffer.publish(seq);
    }

    private static Disruptor<RunnableHolder> createDisruptor(ProducerType producerType) {
        return new Disruptor<>(
                RunnableHolder::new,
                1024 * 64,
//                Executors.defaultThreadFactory(),
                new AffinityThreadFactory("DISRUPTOR", AffinityStrategies.SAME_SOCKET),
                producerType,
                new BusySpinWaitStrategy());
    }

}
