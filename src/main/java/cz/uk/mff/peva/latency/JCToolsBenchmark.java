package cz.uk.mff.peva.latency;
import com.google.common.collect.ImmutableList;
import cz.uk.mff.peva.*;
import org.HdrHistogram.Histogram;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by honza on 15/06/2017.
 */
public class JCToolsBenchmark implements ILatencyBenchmark {

    public static final String JC_TOOLS_1P1C = "JCTools_1p1c";

    private final class SingleProducerOperation {

        public void runOper(SpscArrayQueue<Runnable> queue, ThreadLocalState state) {
            sendBurstSize(queue, state.getEmptyTask(), state.getLastTask());

            state.waitForLastTaskExecution();
            state.resetLastTask();
        }
    }

    private final class MultipleProducerOperation {

        public void runOper(MpscArrayQueue<Runnable> queue, ThreadLocalState state) {
            sendBurstSize(queue, state.getEmptyTask(), state.getLastTask());

            state.waitForLastTaskExecution();
            state.resetLastTask();
        }
    }

    private void sendBurstSize(
            MpscArrayQueue<Runnable> queue,
            ThreadLocalState.EmptyTask emptyTask,
            ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            while (!queue.offer(emptyTask)) { }

            Blackhole.consumeCPU(backoffTokens);
        }

        while (!queue.offer(lastTask)) { }
    }

    private void sendBurstSize(
            SpscArrayQueue<Runnable> queue,
            ThreadLocalState.EmptyTask emptyTask,
            ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            while (!queue.offer(emptyTask)) { }

            Blackhole.consumeCPU(backoffTokens);
        }

        while (!queue.offer(lastTask)) { }
    }

    private static abstract class ProducerCommon extends ThreadPinnedRunnable {

        final int warmupIteration;
        final int iteration;
        final int batchSize;

        protected ProducerCommon(
                int warmupIteration,
                int iteration,
                int batchSize) {

            this.warmupIteration = warmupIteration;
            this.iteration = iteration;
            this.batchSize = batchSize;
        }
    }

    private abstract class SingleProducerCommon extends ProducerCommon {
        private final SingleProducerOperation operation = new SingleProducerOperation();
        private final SpscArrayQueue<Runnable> queue;
        private final ThreadLocalState threadLocalState;

        protected final Control control;

        SingleProducerCommon(
                SpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize,
                Control control) {

            super(warmupIteration, iteration, batchSize);

            this.queue = queue;
            this.threadLocalState = new ThreadLocalState();
            this.control = control;
        }

        void runBatch() {
            for (int i = 0; i < batchSize; i++) {
                operation.runOper(queue, threadLocalState);
            }
        }
    }

    private abstract class MultiProducerCommon extends ProducerCommon {
        private final MultipleProducerOperation operation = new MultipleProducerOperation();
        private final MpscArrayQueue<Runnable> queue;
        private final ThreadLocalState threadLocalState;

        protected final Control control;

        protected MultiProducerCommon(
                MpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize,
                Control control) {

            super(warmupIteration, iteration, batchSize);

            this.queue = queue;
            this.control = control;
            this.threadLocalState = new ThreadLocalState();
        }

        final void runBatch() {
            for (int i = 0; i < batchSize; i++) {
                operation.runOper(queue, threadLocalState);
            }
        }

    }

    private final class SingleProducer extends SingleProducerCommon implements Runnable, IAverageLatencyBenchmark {
        double[] averageLatencies;

        public SingleProducer(
                final Control control,
                final SpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize) {
            super(
                    queue,
                    warmupIteration,
                    iteration,
                    batchSize,
                    control);

            this.averageLatencies = new double[iteration];
        }

        @Override
        public void run() {
            control.waitForStart();

            for (int i = 0; i < warmupIteration; i++) {
                System.gc();

                System.out.println("WARMUP ITERATION " + i);
                final long t0 = System.nanoTime();
                final long result = runSingleIteration(t0);

                double averageLatency = (double) result/operationCount;

                System.out.println(averageLatency);

                control.waitForNextIteration();
            }

            for (int i = 0; i < iteration; i++) {
                System.gc();

                System.out.println("ITERATION " + i);


                onOperationStart();

                final long t0 = System.nanoTime();
                final long result = runSingleIteration(t0);

                onOperationEnd();

                averageLatencies[i] = (double) result/operationCount;

                System.out.println(averageLatencies[i]);

                control.waitForNextIteration();
            }


            finishOperationsProfiling();
            control.stop();
        }

        private long runSingleIteration(long start) {
            for (int i = 0; i < operationCount; i++) {
                runBatch();
            }

            return System.nanoTime() - start;
        }

        @Override
        public IAverageLatencyResults getResult() {
            return new AverageLatencyResults("PRODUCER", averageLatencies);
        }
    }

    private final class MultipleProducer extends MultiProducerCommon implements Runnable, IAverageLatencyBenchmark {
        double[] averageLatencies;


        public MultipleProducer(
                final MpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize,
                Control control) {

            super(
                    queue,
                    warmupIteration,
                    iteration,
                    batchSize,
                    control);

            this.averageLatencies = new double[iteration];
        }

        @Override
        public void run() {
            control.waitForStart();

            for (int i = 0; i < warmupIteration; i++) {
                System.gc();

                System.out.println("WARMUP ITERATION " + i);

                final long t0 = System.nanoTime();
                final long result = runSingleIteration(t0);

                double averageLatency = (double) result/operationCount;

                System.out.println(averageLatency);

                control.waitForNextIteration();
            }

            for (int i = 0; i < iteration; i++) {
                System.gc();

                System.out.println("ITERATION " + i);



                onOperationStart();
                final long t0 = System.nanoTime();
                final long result = runSingleIteration(t0);
                onOperationEnd();

                averageLatencies[i] = (double) result/operationCount;


                System.out.println(averageLatencies[i]);

                final long pause = System.nanoTime();
                while (System.nanoTime() - pause > 1_000) {
                    Thread.yield();
                }

                control.waitForNextIteration();
            }

            finishOperationsProfiling();
            control.stop();
        }

        private long runSingleIteration(long start) {
            for (int i = 0; i < operationCount; i++) {
                runBatch();
            }

            return System.nanoTime() - start;
        }

        @Override
        public IAverageLatencyResults getResult() {
            final String name = String.format("Thread [backoff=%d]", backoffTokens);
            return new AverageLatencyResults(name, averageLatencies);
        }
    }

    private final class SingleHistogramProducer extends SingleProducerCommon implements Runnable {

        private final List<Histogram> histograms;

        public SingleHistogramProducer(
                SpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize,
                Control control) {
            super(
                    queue,
                    warmupIteration,
                    iteration,
                    batchSize,
                    control);

            this.histograms = new ArrayList<>();
        }

        @Override
        public void run() {
            control.waitForStart();

            for (int i = 0; i < warmupIteration; i++) {
                System.gc();

                System.out.println("WARMUP ITERATION " + i);
                final Histogram histogram = runSingleIteration();
                histogram.outputPercentileDistribution(
                        System.out,
                        5,
                        1.0,
                        false);
                control.waitForNextIteration();
            }

            for (int i = 0; i < iteration; i++) {
                System.gc();

                System.out.println("ITERATION " + i);

                final Histogram histogram = runSingleIteration();
                histograms.add(histogram);

                control.waitForNextIteration();
            }


            control.stop();
        }

        private Histogram runSingleIteration() {
            Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);
            for (int i = 0; i < operationCount; i++) {

                onOperationStart();
                final long start = System.nanoTime();
                runBatch();
                final long batchTime = System.nanoTime() - start;
                onOperationEnd();

                histogram.recordValue(batchTime);
            }

            finishOperationsProfiling();

            return histogram;
        }
    }


    private final class MultiHistogramProducer extends MultiProducerCommon implements Runnable {
        private final List<Histogram> histograms = new ArrayList<>();

        protected MultiHistogramProducer(
                MpscArrayQueue<Runnable> queue,
                int warmupIteration,
                int iteration,
                int batchSize,
                Control control) {

            super(queue, warmupIteration, iteration, batchSize, control);
        }

        @Override
        public void run() {
            control.waitForStart();

            for (int i = 0; i < warmupIteration; i++) {
                System.gc();

                System.out.println("WARMUP ITERATION " + i);
                final Histogram histogram = runSingleIteration();
                histogram.outputPercentileDistribution(
                        System.out,
                        5,
                        1.0,
                        false);

                control.waitForNextIteration();
            }

            for (int i = 0; i < iteration; i++) {
                System.gc();

                System.out.println("ITERATION " + i);

                final Histogram histogram = runSingleIteration();
                histograms.add(histogram);

                control.waitForNextIteration();
            }

            control.stop();
        }

        private Histogram runSingleIteration() {
            Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);
            for (int i = 0; i < operationCount; i++) {

                onOperationStart();
                final long start = System.nanoTime();
                runBatch();
                final long batchTime = System.nanoTime() - start;
                onOperationEnd();

                histogram.recordValue(batchTime);
            }

            finishOperationsProfiling();

            return histogram;
        }
    }



    private final static class Consumer<Q extends Queue<Runnable>> implements Runnable {
        private final Control control;
        private final QueueConsumer<Runnable, Q> queueConsumer;

        private Consumer(Q queue, Control control, IdleStrategy idleStrategy) {
            this.control = control;
            this.queueConsumer = new QueueConsumer<>(queue, idleStrategy);
        }

        @Override
        public void run() {
            control.waitForStart();
            queueConsumer.start();
            queueConsumer.run();
        }

        public void stop() throws InterruptedException {
            queueConsumer.stop();
        }
    }


    private final IThreadCreator threadCreator;
    private final int producerCount;
    private final int operationCount;
    private final int burstSize;
    private final List<Thread> threads;
    private final int warmupIteration;
    private final int iteration;
    private final int backoffTokens;
    private final int batchSize;
    private final ILatenciesPrinter printer;

    private final BenchmarkMode benchmarkMode;

    public JCToolsBenchmark(QueueLatencyBenchmarkArgs args) {
        this.threadCreator = args.getThreadCreator();
        this.producerCount = args.getProducerCount();
        this.operationCount = args.getOperationCount();
        this.burstSize = args.getBurstSize();
        this.threads = new ArrayList<>();
        this.iteration = args.getIteration();
        this.warmupIteration = args.getWarmupIteration();
        this.backoffTokens = args.getProducerBackoffTokens();
        this.printer = args.getLatenciesPrinter();
        this.batchSize = args.getBatchSize();
        this.benchmarkMode = args.getBenchmarkMode();
    }

    @Override
    public void start(Control control) throws Exception {
        System.out.println("JC TOOLS");

        if (benchmarkMode == BenchmarkMode.AverageTime) {
            runAverageTimeBenchmarks(control);
        } else {
            runHistogramBenchmarks(control);
        }

        // start producer threads

        // wait for barrier to start
        // wait for latch to shutdown

        // gather results
    }

    private void runAverageTimeBenchmarks(Control control)
            throws InterruptedException, IOException {

        if (producerCount == 1) {
            run1P1CBenchmark(control);
        } else if (producerCount > 1) {
            runMP1CBenchmark(control);
        }
    }

    private void run1P1CBenchmark(Control control) throws InterruptedException, IOException {
        final SpscArrayQueue<Runnable> queue = new SpscArrayQueue<>(64 * 1024);

        SingleProducer singleProducer = new SingleProducer(
                control,
                queue,
                warmupIteration,
                iteration,
                batchSize);

        Thread producerThread = threadCreator.createProducer(singleProducer);
        control.initProfilers(singleProducer);
        producerThread.start();

        singleProducer.setPrefix(JC_TOOLS_1P1C);
        singleProducer.setUseOrder(false);

        threads.add(producerThread);

        Consumer<SpscArrayQueue<Runnable>> consumer = new Consumer<>(queue, control, new BusySpinIdleStrategy());

        Thread consumerThread = threadCreator.createConsumer(consumer);
        consumerThread.start();

        control.waitForStart();
        control.waitForStop();

        for (final Thread thread : threads) {
            thread.join();
        }

        consumer.stop();
        consumerThread.join();

        printer.print(
                JC_TOOLS_1P1C,
                iteration,
                ImmutableList.of(singleProducer.getResult()));
    }

    private void runMP1CBenchmark(Control control) throws InterruptedException, IOException {

        final String benchmarkName = String.format("JCTools_%dp1c", producerCount);
        final List<IAverageLatencyBenchmark> benchmarks = new ArrayList<>();

        final MpscArrayQueue<Runnable> queue = new MpscArrayQueue<>(64 * 1024);

        for (int i = 0; i < producerCount; i++) {
            MultipleProducer producer = new MultipleProducer(
                    queue,
                    warmupIteration,
                    iteration,
                    batchSize,
                    control);

            Thread producerThread = threadCreator.createProducer(producer);
            producer.setPrefix(benchmarkName);
            producer.setUseOrder(false);

            control.initProfilers(producer);

            producerThread.start();

            benchmarks.add(producer);

            threads.add(producerThread);
        }

        Consumer<MpscArrayQueue<Runnable>> consumer = new Consumer<>(queue, control, new BusySpinIdleStrategy());

        Thread consumerThread = threadCreator.createConsumer(consumer);
        consumerThread.start();

        control.waitForStart();
        control.waitForStop();

        for (final Thread thread : threads) {
            thread.join();
        }

        consumer.stop();
        consumerThread.join();

        List<IAverageLatencyResults> results =
                benchmarks
                        .stream()
                        .map(IAverageLatencyBenchmark::getResult)
                        .collect(Collectors.toList());

        printer.print(benchmarkName, iteration, results);
    }

    private void runHistogramBenchmarks(Control control) throws IOException, InterruptedException {
        if (producerCount == 1) {
            run1P1CBenchmarkHistogram(control);
        } else if (producerCount > 1) {
            runMP1CBenchmarkHistogram(control);
        }
    }

    private void run1P1CBenchmarkHistogram(Control control) throws InterruptedException, FileNotFoundException {
        final SpscArrayQueue<Runnable> queue = new SpscArrayQueue<>(64 * 1024);

        SingleHistogramProducer singleProducer = new SingleHistogramProducer(
                queue,
                warmupIteration,
                iteration,
                batchSize,
                control);

        singleProducer.setPrefix(JC_TOOLS_1P1C);

        Thread producerThread = threadCreator.createProducer(singleProducer);

        control.initProfilers(singleProducer);
        producerThread.start();

        threads.add(producerThread);

        Consumer<SpscArrayQueue<Runnable>> consumer = new Consumer<>(queue, control, new BusySpinIdleStrategy());

        Thread consumerThread = threadCreator.createConsumer(consumer);
        consumerThread.start();

        control.waitForStart();
        control.waitForStop();

        for (final Thread thread : threads) {
            thread.join();
        }

        consumer.stop();
        consumerThread.join();

        printer.print(JC_TOOLS_1P1C, singleProducer.histograms);
    }

    private void runMP1CBenchmarkHistogram(Control control) throws InterruptedException, FileNotFoundException {
        final MpscArrayQueue<Runnable> queue = new MpscArrayQueue<>(64 * 1024);

        final List<MultiHistogramProducer> producers = new ArrayList<>();

        for (int i = 0; i < producerCount; i++) {
            MultiHistogramProducer producer = new MultiHistogramProducer(
                    queue,
                    warmupIteration,
                    iteration,
                    batchSize,
                    control);

            producers.add(producer);
            String name = String.format("JCTools_%dp1c_producer_%d", producerCount, i);
            producer.setPrefix(name);


            Thread producerThread = threadCreator.createProducer(producer);
            control.initProfilers(producer);

            producerThread.start();

            threads.add(producerThread);
        }

        Consumer<MpscArrayQueue<Runnable>> consumer = new Consumer<>(queue, control, new BusySpinIdleStrategy());

        Thread consumerThread = threadCreator.createConsumer(consumer);
        consumerThread.start();

        control.waitForStart();
        control.waitForStop();

        for (final Thread thread : threads) {
            thread.join();
        }

        consumer.stop();
        consumerThread.join();

        for (int i = 0; i < producers.size(); i++) {
            String name = String.format("JCTools_%dp1c_producer_%d", producerCount, i);
            printer.print(name, producers.get(i).histograms);
        }
    }
}
