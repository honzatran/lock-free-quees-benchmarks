package cz.uk.mff.peva.latency;

import cz.uk.mff.peva.QueueState;
import cz.uk.mff.peva.ThreadLocalState;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.BenchmarkMode;

import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 11/06/2017.
 */

@State(Scope.Benchmark)
public class JCToolsLatencyBenchmarks {

    @Param({"1", "100"})
    public static int burstSize;

    @State(Scope.Thread)
    public static class ProducerThreadState extends ThreadLocalState {
        @Setup(Level.Trial)
        public void setUp() {
            resetLastTask();
        }
    }

    @State(Scope.Benchmark)
    public static class MpScQueueState extends QueueState<Runnable, MpscArrayQueue<Runnable>> {
        MpscArrayQueue<Runnable> queue;

        @Setup(Level.Trial)
        public void startQueueProducer() {
            queue = new MpscArrayQueue<>(64 * 1024);
            startConsumer(queue, new BusySpinIdleStrategy());
        }

        @TearDown(Level.Trial)
        public void stopQueueProducer() throws InterruptedException {
            stopConsumer();
        }
    }

    @State(Scope.Benchmark)
    public static class SpScQueueState extends QueueState<Runnable, SpscArrayQueue<Runnable>> {
        SpscArrayQueue<Runnable> queue;

        @Setup(Level.Trial)
        public void startQueueProducer() {
            this.queue = new SpscArrayQueue<>(64 * 1024);
            startConsumer(queue, new BusySpinIdleStrategy());
        }

        @TearDown(Level.Trial)
        public void stopQueueProducer() throws InterruptedException {
            stopConsumer();
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    public void oneProducer(SpScQueueState queueState, ProducerThreadState state) {
        sendBurst(queueState.queue, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    @Threads(2)
    public void twoProducers(MpScQueueState queueState, ProducerThreadState state) {
        sendBurst(queueState.queue, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(5)
    @Threads(4)
    public void fourProducers(MpScQueueState queueState, ProducerThreadState state) {
        sendBurst(queueState.queue, state.getEmptyTask(), state.getLastTask());

        state.waitForLastTaskExecution();
        state.resetLastTask();
    }

    public static void sendBurst(
            final SpscArrayQueue<Runnable> queue,
            final ThreadLocalState.EmptyTask emptyTask,
            final ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            while (!queue.offer(emptyTask)) {

            }
        }

        while (!queue.offer(lastTask)) {

        }
    }

    public static void sendBurst(
            final MpscArrayQueue<Runnable> queue,
            final ThreadLocalState.EmptyTask emptyTask,
            final ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            while (!queue.offer(emptyTask)) {

            }
        }

        while (!queue.offer(lastTask)) {

        }
    }
}
