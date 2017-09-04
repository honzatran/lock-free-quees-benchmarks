package cz.uk.mff.peva.latency; import com.lmax.disruptor.RingBuffer;
import cz.uk.mff.peva.ThreadLocalState;
import cz.uk.mff.peva.ThreadPinnedRunnable;
import org.HdrHistogram.Histogram;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 20/06/2017.
 */




class DisruptorAverageTimeProducerL0 extends ThreadPinnedRunnable implements IAverageLatencyBenchmark {

    private final RingBuffer<RunnableHolder> ringBuffer;
    private final ThreadLocalState state = new ThreadLocalState();
    private final Control control;
    private double[] averageTimes;

    private final int warmupIterationCount;
    private final int iterationCount;

    private final int operationCount;
    private final int burstSize;
    private final int backoffTokens;
    private final int batchSize;
    private final BenchmarkMode benchmarkMode;
    private final List<Histogram> histograms;

    protected DisruptorAverageTimeProducerL0(
            RingBuffer<RunnableHolder> ringBuffer,
            Control control,
            int warmupIterationCount,
            int iterationCount,
            int operationCount,
            int burstSize,
            int backoffTokens,
            int batchSize,
            BenchmarkMode benchmarkMode) {
        this.ringBuffer = ringBuffer;
        this.control = control;
        this.warmupIterationCount = warmupIterationCount;
        this.iterationCount = iterationCount;
        this.operationCount = operationCount;
        this.burstSize = burstSize;
        this.backoffTokens = backoffTokens;
        this.averageTimes = new double[iterationCount];

        this.state.resetLastTask();
        this.batchSize = batchSize;
        this.benchmarkMode = benchmarkMode;
        this.histograms = new ArrayList<>();
    }

    @Override
    public void run() {
        runBenchmark();
    }

    public void runBenchmark() {
        control.waitForStart();

        switch (benchmarkMode) {
            case AverageTime:
                runAverageTimeMode();
                break;
            case Histogram:
                runHistogramMode();
                break;
            case HwCounters:
                runHwCountersMode();
                break;
            default:
                throw new RuntimeException("Unsupported mode");
        }

        control.stop();
    }


    private void runAverageTimeMode() {
        for (int i = 0; i < warmupIterationCount; i++) {
            System.gc();

            System.out.println("WARMUP ITERATION " + i);

            onOperationStart();
            final long start = System.nanoTime();
            final long result = runAverageTimeIteration(start);

            double averageTime = (double) result / operationCount;
            System.out.println(averageTime);

            control.waitForNextIteration();
        }

        for (int i = 0; i < iterationCount; i++) {

            control.onMeasurementStart();
            final long start = System.nanoTime();
            long result = runAverageTimeIteration(start);
            averageTimes[i] = (double) result / operationCount;
            control.onMeasurementEnd();
            System.out.println(averageTimes[i]);

            control.waitForNextIteration();
        }

        finishOperationsProfiling();
    }

    private void runHistogramMode() {
        for (int i = 0; i < warmupIterationCount; i++) {
            System.gc();

            System.out.println("WARMUP ITERATION " + i);

            Histogram histogram = runHistogramIteration();
            histogram.outputPercentileDistribution(System.out, 1.0);

            control.waitForNextIteration();
        }

        for (int i = 0; i < iterationCount; i++) {

            System.out.println("ITERATION " + i);

            Histogram histogram = runHistogramIteration();
            histograms.add(histogram);

            control.waitForNextIteration();
        }
    }

    private void runHwCountersMode() {
        for (int i = 0; i < warmupIterationCount; i++) {
            System.gc();
            System.out.println("WARMUP ITERATION " + i);

            runHwCountersIteration();
            control.waitForNextIteration();
        }

        for (int i = 0; i < iterationCount; i++) {
            System.gc();
            System.out.println("ITERATION " + i);

            runHwCountersIteration();
            control.waitForNextIteration();
        }
    }

    private long runAverageTimeIteration(long start) {
        for (int i = 0; i < operationCount; i++) {
            runSingleOperation();
        }
        final long end = System.nanoTime();
        onOperationEnd();

        while (System.nanoTime() - end < 1000) {
            Thread.yield();
        }

        return end - start;
    }

    private Histogram runHistogramIteration() {
        Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);

        for (int i = 0; i < operationCount; i++) {

            onOperationStart();
            control.onMeasurementStart();

            long start = System.nanoTime();
            runSingleOperation();
            long time = System.nanoTime() - start;

            control.onMeasurementEnd();
            onOperationEnd();

            histogram.recordValue(time);
        }

        finishOperationsProfiling();

        return histogram;
    }

    private void runHwCountersIteration() {

        for (int i = 0; i < operationCount; i++) {
            onOperationStart();
            sendBurst(ringBuffer, state.getEmptyTask(), state.getLastTask());
            onOperationEnd();

            state.waitForLastTaskExecution();
            state.resetLastTask();
        }

        finishOperationsProfiling();
    }

    private void runSingleOperation() {
        for (int i = 0; i < batchSize; i++) {
            sendBurst(ringBuffer, state.getEmptyTask(), state.getLastTask());
            state.waitForLastTaskExecution();
            state.resetLastTask();
        }
    }

    private void sendBurst(
            RingBuffer<RunnableHolder> ringBuffer,
            ThreadLocalState.EmptyTask emptyTask,
            ThreadLocalState.LastTask lastTask) {

        for (int i = 0; i < burstSize - 1; i++) {
            long seq = ringBuffer.next();
            ringBuffer.get(seq).runnable = emptyTask;
            ringBuffer.publish(seq);

            if (backoffTokens > 0) {
                Blackhole.consumeCPU(backoffTokens);
            }
        }

        long seq = ringBuffer.next();
        ringBuffer.get(seq).runnable = lastTask;
        ringBuffer.publish(seq);
    }

    @Override
    public IAverageLatencyResults getResult() {
        String threadName = String.format("Thread [backoff=%d]", backoffTokens);
        return new AverageLatencyResults(threadName, averageTimes);
    }

    public List<Histogram> getHistograms() {
        return histograms;
    }

}

final class DisruptorAverageTimeProducer extends DisruptorAverageTimeProducerL0 implements Runnable {
    private long p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16;


    DisruptorAverageTimeProducer(
            QueueLatencyBenchmarkArgs args,
            Control control,
            RingBuffer<RunnableHolder> ringBuffer) {

        super(
                ringBuffer,
                control,
                args.getWarmupIteration(),
                args.getIteration(),
                args.getOperationCount(),
                args.getBurstSize(),
                args.getProducerBackoffTokens(),
                args.getBatchSize(),
                args.getBenchmarkMode());
    }
}
