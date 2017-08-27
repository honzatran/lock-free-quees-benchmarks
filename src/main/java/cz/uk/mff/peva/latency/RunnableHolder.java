package cz.uk.mff.peva.latency;

import cz.uk.mff.peva.LeftPadding;

/**
 * Created by honza on 20/06/2017.
 */

abstract class RunnableL0 extends LeftPadding {
    public Runnable runnable;
}

final class RunnableHolder extends RunnableL0 {
    long p2, p3, p4, p5, p6, p7, p8;
}
