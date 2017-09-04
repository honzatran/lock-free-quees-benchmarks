package cz.uk.mff.peva.latency;

import cz.uk.mff.peva.*;
import cz.uk.mff.peva.profilers.PmcCoreProfiler;
import cz.uk.mff.peva.profilers.PmcProfiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by honza on 16/06/2017.
 */
public class Control {
    private final CyclicBarrier startBarrier;
    private final CountDownLatch stopLatch;

    private final CyclicBarrier iterationBarrier;

    private PmcProfiler pmcProfiler;

    private final Map<Integer, PmcCoreProfiler> coreProfilers;


    public Control(QueueLatencyBenchmarkArgs args) {
        this.startBarrier = new CyclicBarrier(args.getProducerCount() + 2);
        this.stopLatch = new CountDownLatch(args.getProducerCount());
        this.iterationBarrier = new CyclicBarrier(args.getProducerCount());
        this.coreProfilers = new HashMap<>();

        initProfilers(args);
    }

    public void waitForStart() {
        try {
            startBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    public void waitForStop() {
        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (pmcProfiler != null) {
            try {
                pmcProfiler.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForNextIteration() {
        try {
            iterationBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stopLatch.countDown();
        System.out.println("STOPPED");
    }

    public void onMeasurementStart() {
    }

    public void onMeasurementEnd() {
    }

    private void initProfilers(QueueLatencyBenchmarkArgs args) {
        if (args.usePmcProfilers()) {
            this.pmcProfiler = new PmcProfiler(args.getConfig());
            this.pmcProfiler.startMeasuring();
        }
    }

    public void initProfilers(ThreadPinnedRunnable runnable) {
        int cpuId = runnable.getCpuId();

        if (pmcProfiler != null && cpuId != ThreadPinnedRunnable.NO_CPU_SET) {

            PmcCoreProfiler pmcCoreProfiler = coreProfilers.computeIfAbsent(
                    cpuId,
                    cpu -> pmcProfiler.getPmcCoreProfiler(cpu));

            runnable.addProfiler(pmcCoreProfiler);
        }
    }

    public PmcCoreProfiler initProfiler(int cpuId) {
        if (pmcProfiler != null && cpuId != ThreadPinnedRunnable.NO_CPU_SET) {

            return coreProfilers.computeIfAbsent(
                    cpuId,
                    cpu -> pmcProfiler.getPmcCoreProfiler(cpu));
        }

        return null;
    }
}
