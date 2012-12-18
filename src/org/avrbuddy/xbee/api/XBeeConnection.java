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

package org.avrbuddy.xbee.api;

import org.avrbuddy.avr.AvrProgrammer;
import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.util.State;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeConnection {
    private static final Logger log = Log.getLogger(XBeeConnection.class);

    private static final int CLOSED = 1;

    public static final long DEFAULT_TIMEOUT = 3000;

    public static final int STATUS_TIMEOUT = 0x100;

    private final SerialConnection serial;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Thread reader;
    private final XBeeFrameListenerList listenerList = new XBeeFrameListenerList();
    private final State state = new State();
    private byte lastFrameId;

    private int maxPayloadSize;

    // -------------- PUBLIC FACTORY --------------

    public static XBeeConnection open(SerialConnection serial) throws IOException {
        XBeeConnection conn = new XBeeConnection(serial);
        try {
            conn.configureConnection();
        } catch (IOException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    // -------------- PUBLIC LOW-LEVER OPERATIONS --------------

    public void close() {
        if (!state.set(CLOSED))
            return;
        serial.close();
        Object[] listeners = listenerList.getListeners();
        for (int i = 0; i < listeners.length; i += 2)
            ((XBeeFrameListener) listeners[i + 1]).connectionClosed();
    }

    public <F> void addListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        if (state.is(CLOSED))
            return;
        listenerList.addListener(frameClass, listener);
    }

    public <F> void removeListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        listenerList.removeListener(frameClass, listener);
    }

    public synchronized void sendFrames(XBeeFrame... frames) throws IOException {
        for (XBeeFrame frame : frames) {
            log.fine("-> " + frame);
        }
        for (XBeeFrame frame : frames) {
            sendFrameInternal(frame);
        }
    }

    public synchronized XBeeFrameWithId[] sendFramesWithId(XBeeFrameWithId.Builder... builders) throws IOException {
        XBeeFrameWithId[] frames = new XBeeFrameWithId[builders.length];
        for (int i = 0; i < builders.length; i++) {
            frames[i] = buildFrameWithId(builders[i]);
        }
        sendFrames(frames);
        return frames;
    }

    public XBeeFrameWithId[] waitResponses(long timeout, final XBeeFrameWithId... frames) throws InterruptedIOException {
        final XBeeFrameWithId[] responses = new XBeeFrameWithId[frames.length];
        XBeeFrameListener<XBeeFrameWithId> listener = new XBeeFrameListener<XBeeFrameWithId>() {
            public void frameReceived(XBeeFrameWithId frame) {
                synchronized (responses) {
                    int waitCnt = 0;
                    for (int i = 0; i < frames.length; i++) {
                        XBeeFrameWithId waitFrame = frames[i];
                        if (responses[i] == null) {
                            if (frame.isResponseFor(waitFrame))
                                responses[i] = frame;
                            else
                                waitCnt++;
                        }
                    }
                    if (waitCnt == 0)
                        responses.notifyAll();
                }
            }

            @Override
            public void connectionClosed() {
                synchronized (responses) {
                    responses.notifyAll();
                }
            }
        };
        addListener(XBeeFrameWithId.class, listener);
        try {
            synchronized (responses) {
                responses.wait(timeout);
            }
        } catch (InterruptedException e) {
            throw ((InterruptedIOException) new InterruptedIOException().initCause(e));
        } finally {
            removeListener(XBeeFrameWithId.class, listener);
        }
        return responses;
    }

    public XBeeFrameWithId[] sendFramesWithIdSeriallyAndWait(long timeout, XBeeFrameWithId.Builder... builders) throws IOException {
        XBeeFrameWithId[] responses = new XBeeFrameWithId[builders.length];
        for (int i = 0; i < builders.length; i++) {
            XBeeFrameWithId[] res = waitResponses(timeout, sendFramesWithId(builders[i]));
            responses[i] = res[0];
            if (getStatus(res) != XBeeAtResponseFrame.STATUS_OK)
                break;
        }
        return responses;
    }

    public int getStatus(XBeeFrameWithId[] responses) {
        int status = XBeeAtResponseFrame.STATUS_OK;
        for (XBeeFrameWithId response : responses) {
            if (response == null)
                return STATUS_TIMEOUT;
            status = Math.max(status, response.getStatus() & 0xff);
        }
        return status;
    }

    // -------------- HIGH-LEVER PUBLIC OPERATION --------------

    public SerialConnection openTunnel(XBeeAddress destination) throws IOException {
        return new XBeeTunnel(this, destination, getMaxPayloadSize());
    }

    // destination == null to change destination of local node via local AT commands
    public int changeRemoteDestination(XBeeAddress destination, XBeeAddress target) throws IOException {
        return getStatus(sendFramesWithIdSeriallyAndWait(DEFAULT_TIMEOUT,
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DH")
                        .setData(target == null ? new byte[0] : target.getHighAddressBytes()),
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DL")
                        .setData(target == null ? new byte[0] : target.getLowAddressBytes())));
    }


    // destination == null to reset local node via local AT commands
    public int resetRemoteHost(XBeeAddress destination) throws IOException {
        // do the actual reset (D3 -> output, low)
        int status = getStatus(waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{4}))));
        // restore config (D3 -> disable).
        // don't wait for the second message, because reset was already initiated by the first one
        sendFramesWithId(XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{0}));
        return status;
    }

    public AvrProgrammer openArvProgrammer(XBeeAddress destination) throws IOException {
        SerialConnection tunnel = openTunnel(destination);
        try {
            return AvrProgrammer.connect(tunnel);
        } catch (IOException e) {
            tunnel.close();
            throw e;
        }
    }

    // -------------- PRIVATE CONSTRUCTOR AND HELPER METHODS --------------

    private XBeeConnection(SerialConnection serial) {
        this.serial = serial;
        in = new DataInputStream(new UnescapeStream(serial.getInput()));
        out = new DataOutputStream(new EscapeStream(serial.getOutput()));
        reader = new ReaderThread("XBeeReader-" + serial);
    }

    private void configureConnection() throws IOException {
        // enable inbound flow control to make sure we can receive all answers (signal RTS)
        serial.setHardwareFlowControl(SerialConnection.FLOW_CONTROL_IN);
        serial.drainInput();
        // now start reader thread to parse input
        reader.start();
        // configure API MODE
        log.fine("Configuring API mode " + (byte) 2);
        if (XBeeAtResponseFrame.STATUS_OK != getStatus(waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("AP").setData((byte) 2)))))
            throw new IOException("No valid XBee device detected. Check that XBee is configured with API firmware and baud rate");
        // enable hardware flow control - RTS & CTS
        log.fine("Configuring RTS and CTS flow control");
        if (XBeeAtResponseFrame.STATUS_OK != getStatus(waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("D6").setData((byte) 1),
                XBeeAtFrame.newBuilder().setAtCommand("D7").setData((byte) 1)))))
            throw new IOException("Failed to enable RTS and CTS flow control on XBee");
        // enable outbound flow control
        serial.setHardwareFlowControl(SerialConnection.FLOW_CONTROL_IN | SerialConnection.FLOW_CONTROL_OUT);
    }

    private XBeeFrame nextFrame() throws IOException {
        while (true) {
            int skipped = 0;
            while (readByteOrEOF() != XBeeUtil.FRAME_START)
                skipped++; // skip bytes
            if (skipped != 0)
                log.log(Level.WARNING, "Skipped " + skipped + " bytes before start of frame");
            int length = in.readShort() & 0xffff;
            byte[] frame = new byte[length + 4];
            frame[0] = XBeeUtil.FRAME_START;
            frame[1] = (byte) (length >> 8);
            frame[2] = (byte) length;
            in.readFully(frame, 3, length + 1);
            try {
                return XBeeFrame.parse(frame);
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, e.getMessage());
            }
        }
    }

    private byte readByteOrEOF() throws IOException {
        int b = serial.getInput().read();
        if (b < 0)
            throw new EOFException("Port is closed");
        return (byte) b;
    }

    private byte nextFrameId() {
        lastFrameId++;
        if (lastFrameId == 0)
            lastFrameId = 1;
        return lastFrameId;
    }


    private XBeeFrameWithId buildFrameWithId(XBeeFrameWithId.Builder builder) {
        return builder.setFrameId(nextFrameId()).build();
    }

    private void sendFrameInternal(XBeeFrame frame) throws IOException {
        byte[] data = frame.getFrame();
        serial.getOutput().write(data[0]);
        out.write(data, 1, data.length - 1);
        out.flush();
    }

    private int getMaxPayloadSize() throws IOException {
        if (maxPayloadSize != 0)
            return maxPayloadSize;
        log.fine("Querying max payload size");
        XBeeFrameWithId[] response = waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("NP")));
        if (getStatus(response) != XBeeAtResponseFrame.STATUS_OK)
            throw new IOException("Cannot determine max payload size for XBee");
        byte[] data = response[0].getData();
        if (data.length != 2)
            throw new IOException("Unrecognized response for max payload size request");
        return maxPayloadSize = ((data[0] & 0xff) << 8) + (data[1] & 0xff);
    }

    @SuppressWarnings({"unchecked"})
    private void dispatch(XBeeFrame frame) {
        Object[] listeners = listenerList.getListeners();
        for (int i = 0; i < listeners.length; i += 2) {
            Class<?> frameClass = (Class<Object>) listeners[i];
            if (frameClass.isInstance(frame))
                ((XBeeFrameListener) listeners[i + 1]).frameReceived(frame);
        }
    }

    public String fmtStatus(XBeeFrameWithId[] responses) {
        return fmtStatus(getStatus(responses));
    }

    public String fmtStatus(int status) {
        switch (status) {
            case STATUS_TIMEOUT:
                return "TIMEOUT";
            case XBeeAtResponseFrame.STATUS_OK:
                return "OK";
            case XBeeAtResponseFrame.STATUS_ERROR:
                return "ERROR";
            case XBeeAtResponseFrame.STATUS_INVALID_COMMAND:
                return "INVALID COMMAND";
            case XBeeAtResponseFrame.STATUS_TX_FAILURE:
                return "TX FAILURE";
            case XBeeAtResponseFrame.STATUS_INVALID_PARAMETER:
                return "INVALID PARAMETER";
            default:
                return HexUtil.formatByte((byte) status);
        }
    }

    private class ReaderThread extends Thread {
        ReaderThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                while (!state.is(CLOSED)) {
                    XBeeFrame frame = nextFrame();
                    log.fine("<- " + frame);
                    dispatch(frame);
                }
            } catch (EOFException e) {
                // ignored, exit
            } catch (InterruptedIOException e) {
                // ignored, exit
            } catch (IOException e) {
                log.log(Level.SEVERE, "IO Exception", e);
            }
            close();
        }
    }
}
