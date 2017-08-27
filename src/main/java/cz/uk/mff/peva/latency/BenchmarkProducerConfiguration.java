package cz.uk.mff.peva.latency;

/**
 * Created by honza on 20/06/2017.
 */



public class BenchmarkProducerConfiguration {
    private final int warmupIterationCount;
    private final int iterationCount;
    private final long operationCount;

    public BenchmarkProducerConfiguration(int warmupIterationCount, int iterationCount, long operationCount) {
        this.warmupIterationCount = warmupIterationCount;
        this.iterationCount = iterationCount;
        this.operationCount = operationCount;
    }
}
