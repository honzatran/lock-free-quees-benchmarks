package cz.uk.mff.peva.latency;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import cz.uk.mff.peva.Benchmark;
import cz.uk.mff.peva.CsvPrinter;
import cz.uk.mff.peva.IThreadCreator;
import cz.uk.mff.peva.ThreadLocalState;
import org.HdrHistogram.Histogram;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by honza on 16/06/2017.
 */
public class DisruptorLatencyBenchmark implements ILatencyBenchmark{

    public static final String DISRUPTOR_1P1C = "Disruptor_1p1c";

    private static class Consumer implements EventHandler<RunnableHolder>, LifecycleAware {
        private final Control control;

        private Consumer(Control control) {
            this.control = control;
        }

        @Override
        public void onEvent(
                RunnableHolder event,
                long sequence,
                boolean endOfBatch) throws Exception {
            event.runnable.run();
        }

        @Override
        public void onStart() {
            control.waitForStart();
        }

        @Override
        public void onShutdown() {
        }
    }

    private final QueueLatencyBenchmarkArgs args;
    private final int producerCount;
    private final List<Thread> threads;
    private final IThreadCreator threadCreator;

    public DisruptorLatencyBenchmark(QueueLatencyBenchmarkArgs args) {
        this.producerCount = args.getProducerCount();
        this.threads = new ArrayList<>();
        this.threadCreator = args.getThreadCreator();
        this.args = args;
    }

    @Override
    public void start(Control control) throws Exception {
        System.out.println("DISRUPTOR");

        ProducerType producerType = producerCount == 1 ? ProducerType.SINGLE : ProducerType.MULTI;

        String benchmarkName = String.format("Disruptor_%dp1c", producerCount);

        Disruptor<RunnableHolder> disruptor = new Disruptor<RunnableHolder>(
                RunnableHolder::new,
                64 * 1024,
                threadCreator::createConsumer,
                producerType,
                new BusySpinWaitStrategy());

        Consumer consumer = new Consumer(control);

        disruptor.handleEventsWith(consumer);

        List<DisruptorAverageTimeProducer> benchmarks = new ArrayList<>();

        for (int i = 0; i < producerCount; i++) {
            DisruptorAverageTimeProducer averageTimeProducer = new DisruptorAverageTimeProducer(
                    args,
                    control,
                    disruptor.getRingBuffer());

            if (producerType == ProducerType.SINGLE) {
                averageTimeProducer.setPrefix(DISRUPTOR_1P1C);
            } else {
                averageTimeProducer.setPrefix(getFilename(i));
            }

            if (args.getBenchmarkMode() == BenchmarkMode.AverageTime) {
                averageTimeProducer.setUseOrder(false);
            }

            benchmarks.add(averageTimeProducer);

            Thread producerThread = threadCreator.createProducer(averageTimeProducer);

            control.initProfilers(averageTimeProducer);

            threads.add(producerThread);

            producerThread.start();
        }

        disruptor.start();
        control.waitForStart();

        control.waitForStop();

        for (final Thread thread : threads) {
            thread.join();
        }

        disruptor.shutdown();

        if (args.getBenchmarkMode() == BenchmarkMode.AverageTime) {
            printAverageTimes(benchmarkName, benchmarks);
        } else if (args.getBenchmarkMode() == BenchmarkMode.Histogram) {
            printHistograms(benchmarks);
        }
    }

    private void printAverageTimes(
            String benchmarkName,
            List<DisruptorAverageTimeProducer> benchmarks) throws IOException {
        List<IAverageLatencyResults> results = benchmarks
                .stream()
                .map(IAverageLatencyBenchmark::getResult)
                .collect(Collectors.toList());

        args.getLatenciesPrinter()
                .print(benchmarkName, args.getIteration(), results);
    }

    private void printHistograms(List<DisruptorAverageTimeProducer> benchmarks) throws FileNotFoundException {
        if (benchmarks.size() == 1) {
            printSingleProducerHistogram(benchmarks.get(0));
        } else {
            printMultiproducerHistogram(benchmarks);
        }
    }

    private void printSingleProducerHistogram(DisruptorAverageTimeProducer benchmark) throws FileNotFoundException {
        final String fileName = DISRUPTOR_1P1C;
        args.getLatenciesPrinter().print(fileName, benchmark.getHistograms());
    }

    private void printMultiproducerHistogram(List<DisruptorAverageTimeProducer> benchmarks) throws FileNotFoundException {
        for (int i = 0; i < benchmarks.size(); i++) {
            String fileName = getFilename(i);
            args.getLatenciesPrinter().print(fileName, benchmarks.get(i).getHistograms());
        }
    }

    private String getFilename(int i) {
        return String.format("Disruptor_%dp1c_producer_%d", producerCount, i);
    }

}

