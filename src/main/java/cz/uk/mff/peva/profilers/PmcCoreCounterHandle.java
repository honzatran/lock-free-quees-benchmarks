package cz.uk.mff.peva.profilers;

public class PmcCoreCounterHandle {
    private final int index;

    public PmcCoreCounterHandle(int index) {
        this.index = index;
    }

    public void onBegin() {
        onBegin(index);
    }

    public void onEnd() {
        onEnd(index);
    }

    public void reset() {
        reset(index);
    }

    public void recordResult(String output) {
        recordResult(index, output);
    }

    private native void onBegin(int index);

    private native void onEnd(int index);

    private native void reset(int index);

    private native void recordResult(int index, String output);
}
