package cz.uk.mff.peva.latency;

import com.google.common.base.Splitter;
import cz.uk.mff.peva.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by honza on 07/07/2017.
 */
public class BenchmarkRunner {
    private static final String SCENARIO = "scenario";
    private static final String RUN_BENCHMARK = "run benchmark";

    private static final Splitter.MapSplitter MODIFICATION_SPLITTER =
            Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .withKeyValueSeparator('=');


    private Config config;

    public BenchmarkRunner(Config config) {
        this.config = config;
    }

    public void runBenchmarks() throws IOException {
        final String scenarioFile = config.getString(SCENARIO);

        Path path = Paths.get(scenarioFile);
        Files.lines(path)
             .filter(this::scenarioFilter)
             .map(String::trim)
             .forEach(this::processScenarioLine);
    }

    private void processScenarioLine(String line) {
        if (line.contentEquals(RUN_BENCHMARK)) {
            runBenchmark();
        } else {
            MODIFICATION_SPLITTER.split(line)
                                 .forEach(config::updateString);
        }
    }

    private void runBenchmark() {
        final QueueLatencyBenchmarkArgs args = new QueueLatencyBenchmarkArgs(config);
        final String type = config.getString("benchmark.type");

        final Control control = new Control(args);

        ILatencyBenchmark benchmark;
        if (type.equals("JCTools")) {
            benchmark = new JCToolsBenchmark(args);
        } else if (type.equals("Disruptor")){
            benchmark = new DisruptorLatencyBenchmark(args);
        } else {
            throw new RuntimeException("unknown benchmark type");
        }

        try {
            benchmark.start(control);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean scenarioFilter(String line) {
        if (line.isEmpty()) {
            return false;
        }

        if (line.startsWith("#")) {
            return false;
        }

        return true;
    }
}
