package cz.uk.mff.peva.latency;

import cz.uk.mff.peva.*;
import cz.uk.mff.peva.profilers.PmcProfiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honza on 15/06/2017.
 */
public class QueueLatencyBenchmarkArgs {
    public static final String PRODUCER_COUNT = "latency.producer_count";
    public static final String OPERATION_COUNT = "latency.operation_count";
    public static final String BURST_SIZE = "latency.burst_size";
    public static final String WARMUP_ITERATION = "latency.warmup_iteration";
    public static final String ITERATION = "latency.iteration";
    public static final String PRODUCER_BACKOFF = "producer.backoff";
    public static final String BATCH_SIZE = "batchSize";
    public static final String BENCHMARK_MODE = "benchmarkMode";
    public static final String USE_PMC = "usePmc";

    private final Config config;
    private final int producerCount;
    private final int operationCount;
    private final int burstSize;
    private final int warmupIteration;
    private final int iteration;
    private final int producerBackoffTokens;
    private final int batchSize;
    private final BenchmarkMode benchmarkMode;

    private final IThreadCreator threadCreator;
    private final ILatenciesPrinter latenciesPrinter;

    public QueueLatencyBenchmarkArgs(Config config) {
        this.config = config;
        this.producerCount = config.getInt(PRODUCER_COUNT);
        this.operationCount = config.getInt(OPERATION_COUNT);
        this.threadCreator = new AffinityThreadCreator(config);
        this.burstSize = config.getInt(BURST_SIZE);
        this.warmupIteration = config.getInt(WARMUP_ITERATION);
        this.iteration = config.getInt(ITERATION);
        this.producerBackoffTokens = config.getInt(PRODUCER_BACKOFF);
        this.latenciesPrinter = new CsvPrinter(config);
        this.batchSize = config.getInt(BATCH_SIZE);
        this.benchmarkMode = BenchmarkMode.parse(config.getString(BENCHMARK_MODE));
    }

    public int getProducerCount() {
        return producerCount;
    }

    public int getOperationCount() {
        return operationCount;
    }

    public IThreadCreator getThreadCreator() {
        return threadCreator;
    }

    public int getBurstSize() {
        return burstSize;
    }

    public int getWarmupIteration() {
        return warmupIteration;
    }

    public int getIteration() {
        return iteration;
    }

    public int getProducerBackoffTokens() {
        return producerBackoffTokens;
    }

    public ILatenciesPrinter getLatenciesPrinter() {
        return latenciesPrinter;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public BenchmarkMode getBenchmarkMode() {
        return benchmarkMode;
    }

    public Config getConfig() {
        return config;
    }

    public boolean usePmcProfilers() {
        return config.getBool(USE_PMC);
    }
}
