package cz.uk.mff.peva;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ThreadPinnedRunnable implements Runnable {
    public static final int NO_CPU_SET = -1;

    private final List<IIterationProfiler> profilers;
    private int cpuId = NO_CPU_SET;

    private String prefix;

    private int recordedNum = 0;

    private boolean useOrder = true;

    protected ThreadPinnedRunnable() {
        this.profilers = new ArrayList<>();
    }

    protected final void onOperationStart() {
        for (int i = 0; i < profilers.size(); i++) {
            profilers.get(i)
                     .onIterationStart();
        }
    }

    protected final void onOperationEnd() {
        for (int i = 0; i < profilers.size(); i++) {
            profilers.get(i)
                     .onIterationEnd();
        }
    }

    protected final void finishOperationsProfiling() {
        for (int i = 0; i < profilers.size(); i++) {
            String prefix =
                    getPrefix();

            profilers.get(i)
                     .recordResult(prefix);
        }
    }

    public void addProfiler(final IIterationProfiler profiler) {
        profilers.add(profiler);
    }

    public int getCpuId() {
        return cpuId;
    }

    public void setCpuId(final int cpuId) {
        this.cpuId = cpuId;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public void setUseOrder(final boolean useOrder) {
        this.useOrder = useOrder;
    }

    @NotNull
    private String getPrefix() {
        final String prefix = this.prefix != null ? this.prefix : "profiler";
        if (useOrder) {
            return prefix + "_"+ recordedNum++;
        }

        return prefix;
    }

}
