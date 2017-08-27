package cz.uk.mff.peva;

import cz.uk.mff.peva.latency.*;
import cz.uk.mff.peva.profilers.PmcCoreProfiler;
import cz.uk.mff.peva.profilers.PmcProfiler;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honza on 17/06/2017.
 */
public class Benchmark {
    private static final String TYPE = "benchmark.type";


    public static void main(final String[] args) throws Exception {
        /*
        Benchmark benchmark = new Benchmark();
        benchmark.init();
        benchmark.runBenchmarks();
        */


        Config config = new Config();
        BenchmarkRunner runner = new BenchmarkRunner(config);
        runner.runBenchmarks();
    }

}
