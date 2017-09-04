package cz.uk.mff.peva.profilers;

import cz.uk.mff.peva.Config;
import cz.uk.mff.peva.latency.BenchmarkMode;
import cz.uk.mff.peva.latency.QueueLatencyBenchmarkArgs;

import javax.imageio.plugins.bmp.BMPImageWriteParam;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Created by honza on 09/07/2017.
 */
public class PmcProfiler implements AutoCloseable {
    private static final String PMC_CONFIG_FILE = "pmcConfigFile";
    private static final String PMC_OUTPUT_DIR = "pmcOutputDir";

    private static final String PMC_COUNTER_ONE = "pmcCounterOne";
    private static final String PMC_COUNTER_TWO = "pmcCounterTwo";
    private static final String PMC_COUNTER_THREE = "pmcCounterThree";
    private static final String PMC_COUNTER_FOUR = "pmcCounterFour";

    private final PmcHandle handle;
    private final Path outputDir;

    private final int operation;

    public PmcProfiler(Config config) {
        final String configFile = config.getString(PMC_CONFIG_FILE);
        final String outputDir = config.getString(PMC_OUTPUT_DIR);

        this.handle = new PmcHandle();
        this.handle.initialize(configFile);

        this.outputDir = Paths.get(outputDir);

        this.operation = getOperationCount(config);

        createOutputDir();
        setCounters(config);
    }

    private void createOutputDir() {
        boolean exists = Files.exists(outputDir);

        if (exists && !Files.isDirectory(outputDir)) {
            throw new RuntimeException("Pmc output dir is not a directory");
        }

        if (!exists) {
            try {
                Files.createDirectory(outputDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int getOperationCount(Config config) {
        BenchmarkMode mode = BenchmarkMode.parse(
                config.getString(QueueLatencyBenchmarkArgs.BENCHMARK_MODE));

        switch (mode) {
            case HwCounters:
            case Histogram:
                return config.getInt(QueueLatencyBenchmarkArgs.OPERATION_COUNT);
            case AverageTime:
                return config.getInt(QueueLatencyBenchmarkArgs.ITERATION);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public void startMeasuring() {
        handle.startMeasuring();
    }

    public PmcCoreProfiler getPmcCoreProfiler(int cpu) {
        final PmcCoreCounterHandle coreHandle = handle.getCoreProfiler(cpu, operation);
        return new PmcCoreProfiler(coreHandle, outputDir, cpu);
    }

    @Override
    public void close() throws Exception {
        this.handle.destroy();
    }

    private void setCounters(Config config) {
        setCounter(config, PMC_COUNTER_ONE, handle::setFirstCounter);
        setCounter(config, PMC_COUNTER_TWO, handle::setSecondCounter);
        setCounter(config, PMC_COUNTER_THREE, handle::setThirdCounter);
        setCounter(config, PMC_COUNTER_FOUR, handle::setForthCounter);
    }

    private void setCounter(final Config config, final String counter, final Consumer<String> counterSetter) {
        if (config.containsKey(counter)) {
            counterSetter.accept(config.getString(counter));
        }
    }
}
