package cz.uk.mff.peva.latency;

/**
 * Created by honza on 23/06/2017.
 */
public class AverageLatencyResults implements IAverageLatencyResults {
    private final String threadName;
    private final double[] latencies;

    public AverageLatencyResults(String threadName, double[] latencies) {
        this.threadName = threadName;
        this.latencies = latencies;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public double[] getAverageLatencies() {
        return latencies;
    }
}
