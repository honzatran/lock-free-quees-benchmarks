package cz.uk.mff.peva.latency;

/**
 * Created by honza on 23/06/2017.
 */
public interface IAverageLatencyResults {
    String getThreadName();
    double[] getAverageLatencies();
}
