package cz.uk.mff.peva.profilers;

import cz.uk.mff.peva.IIterationProfiler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PmcCoreProfiler implements IIterationProfiler {
    private static final String HW_COUNTER = "HwCounter.csv";

    private final PmcCoreCounterHandle handle;
    private final Path outputDir;
    private final int cpu;

    public PmcCoreProfiler(PmcCoreCounterHandle handle, Path outputDir, int cpu) {
        this.handle = handle;
        this.outputDir = outputDir;
        this.cpu = cpu;
    }

    @Override
    public void onIterationStart() {
        handle.onBegin();
    }

    @Override
    public void onIterationEnd() {
        handle.onEnd();
    }

    @Override
    public void recordResult(String prefix) {
        final String filename = prefix + "_pmc_cpu_" + cpu;
        Path output = Paths.get(outputDir.toString(), filename);

        handle.recordResult(output.toString());
        handle.reset();
    }
}
