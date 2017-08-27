package cz.uk.mff.peva;

/**
 * Created by honza on 17/06/2017.
 */
public interface IThreadCreator {

    Thread createProducer(Runnable r);

    Thread createConsumer(Runnable r);

//    default Thread createProducer(ThreadPinnedRunnable r) {
//        return createProducer((Runnable) r);
//    }
//
//    default Thread createConsumer(ThreadPinnedRunnable r) {
//        return createConsumer((Runnable) r);
//    }
}
