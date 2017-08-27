package cz.uk.mff.peva;

/**
 * Created by honza on 25/06/2017.
 */
public interface IIterationProfiler {
    void onIterationStart();
    void onIterationEnd();

    void recordResult(String path);
}
