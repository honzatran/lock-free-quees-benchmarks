package cz.uk.mff.peva;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 13/06/2017.
 */
@State(Scope.Benchmark)
public class BlackholeBenchmark {

    @Param({"1", "10", "50", "100"})
    int tokens;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(1)
    public void consumeCpu() {
        Blackhole.consumeCPU(tokens);
    }
}
