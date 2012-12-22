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

package org.avrbuddy.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class LoggedThread extends Thread {
    protected final Logger log;

    public LoggedThread() {
        log = Log.getLogger(getClass());
        setName(getClass().getSimpleName());
        setUncaughtExceptionHandler(new Handler());
    }

    public LoggedThread(String name) {
        log = Logger.getLogger(getClass() + ":" + name);
        setName(getClass().getSimpleName() + ":" + name);
        setUncaughtExceptionHandler(new Handler());
    }

    private class Handler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.log(Level.SEVERE, "Thread failed", e);
        }
    }
}
