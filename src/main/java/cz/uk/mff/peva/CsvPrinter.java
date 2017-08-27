package cz.uk.mff.peva;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import cz.uk.mff.peva.latency.IAverageLatencyResults;
import cz.uk.mff.peva.latency.ILatenciesPrinter;
import org.HdrHistogram.Histogram;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by honza on 23/06/2017.
 */
public class CsvPrinter implements ILatenciesPrinter {
    private static final String RESULT_OUTPUT_DIR = "csv.output.dir";
    private static final Joiner JOINER = Joiner.on(',');

    private final File directory;
    private final boolean merge = false;

    public CsvPrinter(Config config) {
        String dir = config.getString(RESULT_OUTPUT_DIR);
        this.directory = new File(dir);

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("directory " + dir + " cannot be created");
            }
        }
    }

    public void print(
            final String benchmarkName,
            final int iterationCount,
            final List<IAverageLatencyResults> results) throws IOException {
        String fileName = benchmarkName.endsWith(".csv") ? benchmarkName : benchmarkName + ".csv";

        File file = new File(Paths.get(directory.getAbsolutePath(), fileName).toString());

        if (!file.exists()) {
            file.createNewFile();
        }

        try (BufferedWriter writer = Files.newWriter(file, Charset.defaultCharset())) {
            String header = JOINER.join(
                    results
                            .stream()
                            .map(IAverageLatencyResults::getThreadName)
                            .collect(Collectors.toList()));

            writer.write(header);
            writer.newLine();

            for (int i = 0; i < iterationCount; i++) {
                final int iteration = i;
                List<String> averageLatencies = results
                        .stream()
                        .map(r -> r.getAverageLatencies()[iteration])
                        .map(d -> d.toString())
                        .collect(Collectors.toList());

                String line = JOINER.join(averageLatencies);

                writer.write(line);
                writer.newLine();
            }
        }
    }

    @Override
    public void print(final String benchmarkName, final List<Histogram> histograms) throws FileNotFoundException {

        for (int i = 0; i < histograms.size(); i++) {
            final String fileName = benchmarkName + "_iteration_" + i + ".csv";
            final Path path = Paths.get(directory.getPath(), fileName);

            try (FileOutputStream outputStream = new FileOutputStream(path.toFile());
                 BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream);
                 PrintStream printStream = new PrintStream(bufferedStream)) {

                histograms.get(i).outputPercentileDistribution(printStream,
                        10,
                        1.0,
                        true);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
