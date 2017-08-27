package cz.uk.mff.peva.throughput;

import com.lmax.disruptor.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Created by honza on 22/04/2017.
 */
@State(Scope.Group)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class DisruptorBenchmark {
    public static class LongEvent {
        long val;

        public LongEvent(long val) {
            this.val = val;
        }
    }

    public static class LongEventFactory implements EventFactory<LongEvent> {

        @Override
        public LongEvent newInstance() {
            return new LongEvent(-1);
        }
    }

    public static class LongPublisher {
        private final RingBuffer<LongEvent> ringBuffer;

        public LongPublisher(RingBuffer<LongEvent> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        public void push() {
            long next = ringBuffer.next();
            try {
                ringBuffer.get(next).val = 42;
            } finally {
                ringBuffer.publish(next);
            }
        }
    }

    private final RingBuffer<LongEvent> ringBuffer = RingBuffer.createSingleProducer(
            new LongEventFactory(),
            128 * 1024,
            new BusySpinWaitStrategy());

    private final EventPoller<LongEvent> poller = ringBuffer.newPoller();
    {
        ringBuffer.addGatingSequences(poller.getSequence());
    }

    public long writeValue = 42;

    @State(Scope.Thread)
    @AuxCounters
    public static class ConsumerCounters {
        public long taken;
        public long takenFailed;

        @Setup(Level.Iteration)
        public void resetCounters() {
            taken = takenFailed = 0;
        }
    }

    @State(Scope.Thread)
    @AuxCounters
    public static class ProducerCounters {
        public long published;
        public long publishedFailed;

        @Setup(Level.Iteration)
        public void resetCounters() {
            published = publishedFailed = 0;
        }

    }


    class LongEventHandler implements EventPoller.Handler<LongEvent> {
        Blackhole blackhole;

        @Override
        public boolean onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
            return false;
        }
    }


    @Group("Q")
    @Benchmark
    public void take(Blackhole blackhole, ConsumerCounters counters) throws Exception {
        EventPoller.PollState result = poller.poll((value, l , b) -> {
            counters.taken++;
            blackhole.consume(value);
            return false;
        });

        if (result == EventPoller.PollState.IDLE) {
            counters.takenFailed++;
        }
    }

    @Group("Q")
    @Benchmark
    public void put(ProducerCounters counters) throws InterruptedException {
        long next;
        try {
            next = ringBuffer.tryNext();
            ringBuffer.get(next).val = writeValue;
            ringBuffer.publish(next);
            counters.published++;
        } catch (InsufficientCapacityException e) {
            counters.publishedFailed++;
        }
    }
}
