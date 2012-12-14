package org.avrbuddy.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author Roman Elizarov
 */
public class State {
    private final Sync sync = new Sync();

    public boolean is(int state) {
        return sync.is(state);
    }

    public boolean set(int state) {
        return sync.set(state);
    }

    public boolean clear(int state) {
        return sync.clear(state);
    }

    public void await(int state, long timeout) {
        if (timeout == 0)
            sync.acquireShared(state);
        else
            try {
                sync.tryAcquireSharedNanos(state, TimeUnit.MILLISECONDS.toNanos(timeout));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        public boolean is(int state) {
            return (getState() & state) != 0;
        }

        public boolean set(int state) {
            int expect;
            do {
                expect = getState();
                if ((expect & state) != 0)
                    return false;
            } while (!compareAndSetState(expect, expect | state));
            releaseShared(state);
            return true;
        }

        public boolean clear(int state) {
            int expect;
            do {
                expect = getState();
                if ((expect & state) == 0)
                    return false;
            } while (!compareAndSetState(expect, expect & ~state));
            return true;
        }

        @Override
        protected int tryAcquireShared(int arg) {
            return is(arg) ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            return true;
        }
    }
}
