package cz.uk.mff.peva.latency;

/**
 * Created by honza on 02/07/2017.
 */
public enum BenchmarkMode {
    AverageTime("AverageTime"),
    Histogram("Histogram"),
    HwCounters("HwCounters");

    String text;

    BenchmarkMode(String text) {
        this.text = text;
    }

    public static BenchmarkMode parse(String text) {
        switch (text) {
            case "AverageTime":
                return AverageTime;
            case "Histogram":
                return Histogram;
            case "HwCounters":
                return HwCounters;
            default:
                throw new RuntimeException(text + "is not benchmark mode");
        }
    }
}
