/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
