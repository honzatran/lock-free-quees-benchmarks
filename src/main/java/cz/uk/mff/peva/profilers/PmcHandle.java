package cz.uk.mff.peva.profilers;

/**
 * Created by honza on 09/07/2017.
 */
public class PmcHandle {
    static {
        System.loadLibrary("JavaPCM");
        //System.loadLibrary("PCM");
        //System.loadLibrary("PcmMsr");
        //System.loadLibrary("pcm-wrapper");
    }

    public native boolean initialize(String configFileName);

    public native void setFirstCounter(String counterName);

    public native void setSecondCounter(String counterName);

    public native void setThirdCounter(String counterName);

    public native void setForthCounter(String counterName);

    public native void startMeasuring();

    public native void onBegin();

    public native void onEnd();

    public native void destroy();

    public native PmcCoreCounterHandle getCoreProfiler(int cpu, int operation);
}
