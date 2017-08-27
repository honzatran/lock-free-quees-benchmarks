package cz.uk.mff.peva.latency;

import org.HdrHistogram.Histogram;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by honza on 23/06/2017.
 */
public interface ILatenciesPrinter {
    void print(
            String benchmarkName,
            int iterationCount,
            List<IAverageLatencyResults> results) throws IOException;

    void print(
            String benchmarkName,
            List<Histogram> histograms) throws FileNotFoundException;
}
