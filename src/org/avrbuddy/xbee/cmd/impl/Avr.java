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

package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.avr.AvrMemCmd;
import org.avrbuddy.avr.AvrMemType;
import org.avrbuddy.avr.AvrOperation;
import org.avrbuddy.avr.AvrProgrammer;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.api.XBeeFrameListener;
import org.avrbuddy.xbee.api.XBeeRxFrame;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * @author Roman Elizarov
 */
public class Avr extends Command {
    private AvrOperation operation;

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.DEST_REQUIRED, Option.ARG);
    }

    @Override
    public String getCommandDescription() {
        return "Performs specified operation with Arduino AVR bootloader.";
    }

    @Override
    public String getParameterDescription() {
        return "[<memop>]";
    }

    @Override
    public String getMoreHelp() {
        return "Where <memop> is <memtype>:<memcmd>:<filename>[:<format>]\n" +
            "  <memtype> is one of " + list(AvrMemType.values()) + ";\n" +
            "  <memcmd>  is one of " + list(AvrMemCmd.values()) + ";\n" +
            "  <format>  is 'i' (Index HEX is the only supported format).";
    }

    private String list(Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        if (getArg() != null)
            operation = AvrOperation.parse(getArg());
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        DestTracker destTracker = new DestTracker(ctx);
        destTracker.start();
        ctx.conn.addListener(XBeeRxFrame.class, destTracker);
        try {
            XBeeAddress remoteAddress = destination.resolveAddress(ctx);
            destTracker.changeDestAndSave(remoteAddress);
            AvrProgrammer pgm = AvrProgrammer.open(ctx.conn.openTunnel(remoteAddress));
            if (operation != null)
                operation.execute(pgm);
            pgm.quit();
            pgm.close();
        } finally {
            ctx.conn.removeListener(XBeeRxFrame.class, destTracker);
            destTracker.stopAndRestore();
        }
        return (operation != null ? operation + " " : "") + OK;
    }

    private class DestTracker extends Thread implements XBeeFrameListener<XBeeRxFrame> {
        private static final int ATTEMPTS = 10;

        private final Map<XBeeAddress, XBeeAddress> oldDestMap = Collections.synchronizedMap(new LinkedHashMap<XBeeAddress, XBeeAddress>());
        private final BlockingQueue<XBeeAddress> queue = new LinkedBlockingQueue<XBeeAddress>();
        private final Set<XBeeAddress> inQueue = Collections.synchronizedSet(new HashSet<XBeeAddress>());

        private final CommandContext ctx;
        private final XBeeAddress newDest;

        public DestTracker(CommandContext ctx) throws IOException {
            super("DestTracker");
            this.ctx = ctx;
            newDest = ctx.discovery.getOrDiscoverLocalNode().getAddress();
        }

        public void changeDestAndSave(XBeeAddress remoteAddress) throws IOException {
            XBeeAddress oldDest = ctx.conn.queryRemoteDestination(remoteAddress, ATTEMPTS);
            if (!oldDest.equals(newDest)) {
                ctx.conn.changeRemoteDestination(remoteAddress, newDest);
                oldDestMap.put(remoteAddress, oldDest);
            }
        }

        public void stopAndRestore() throws IOException {
            interrupt();
            try {
                join();
            } catch (InterruptedException e) {
                throw (InterruptedIOException)(new InterruptedIOException(e.getMessage()).initCause(e));
            }
            for (Map.Entry<XBeeAddress, XBeeAddress> entry : oldDestMap.entrySet())
                ctx.conn.changeRemoteDestination(entry.getKey(), entry.getValue());
        }

        @Override
        public void frameReceived(XBeeRxFrame frame) {
            if ((frame.getOptions() & XBeeRxFrame.OPTIONS_BROADCAST) == 0)
                return;
            XBeeAddress remoteAddress = frame.getSource();
            log.info(String.format("Received broadcast packet from %s", remoteAddress));
            if (oldDestMap.containsKey(remoteAddress))
                return;
            if (inQueue.contains(remoteAddress))
                return;
            inQueue.add(remoteAddress);
            queue.add(remoteAddress);
        }

        @Override
        public void connectionClosed() {
            interrupt();
        }

        @Override
        public void run() {
            try {
                while (!interrupted()) {
                    XBeeAddress remoteAddress = queue.take();
                    process(remoteAddress);
                    inQueue.remove(remoteAddress);
                }
            } catch (InterruptedException e) {
                // return
            }
        }

        void process(XBeeAddress remoteAddress) throws InterruptedException {
            XBeeAddress oldDest;
            try {
                oldDest = ctx.conn.queryRemoteDestination(remoteAddress, ATTEMPTS);
            } catch (InterruptedIOException e) {
                throw (InterruptedException)(new InterruptedException().initCause(e));
            } catch (IOException e) {
                log.log(Level.WARNING,
                        String.format("Cannot query destination for broadcast packet source %s", remoteAddress), e);
                return;
            }
            if (!oldDest.equals(XBeeAddress.BROADCAST)) {
                log.log(Level.WARNING,
                        String.format("Broadcast packet source at %s is configured " +
                        "with a destination address of %s", remoteAddress, oldDest));
                return;
            }
            try {
                ctx.conn.changeRemoteDestination(remoteAddress, newDest);
                oldDestMap.put(remoteAddress, oldDest);
            } catch (InterruptedIOException e) {
                throw (InterruptedException)(new InterruptedException().initCause(e));
            } catch (IOException e) {
                log.log(Level.WARNING,
                        String.format("Cannot change destination for broadcast packet source %s", remoteAddress), e);
            }
        }
    }
}
