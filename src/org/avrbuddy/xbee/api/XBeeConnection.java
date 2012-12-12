package org.avrbuddy.xbee.api;

import org.avrbuddy.avr.AvrProgrammer;
import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeConnection {
    private static final Logger log = Log.getLogger(XBeeConnection.class);

    public static final long DEFAULT_TIMEOUT = 3000;

    public static final byte STATUS_TIMEOUT = (byte) 0xff;

    private static final String AP_COMMAND = "AP";
    private static final byte AP_MODE = 2; // use escaping

    private static final String RTS_COMMAND = "D6";
    private static final byte RTS_ON = 1;

    private static final String CTS_COMMAND = "D7";
    private static final byte CTS_ON = 1;

    private final SerialConnection serial;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Thread reader;
    private final XBeeFrameListenerList listenerList = new XBeeFrameListenerList();
    private final AtomicBoolean closed = new AtomicBoolean();
    private byte lastFrameId;

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
        if (!closed.compareAndSet(false, true))
            return;
        serial.close();
        Object[] listeners = listenerList.getListeners();
        for (int i = 0; i < listeners.length; i += 2)
            ((XBeeFrameListener) listeners[i + 1]).connectionClosed();
    }

    public <F> void addListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        if (closed.get())
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
        XBeeFrameWithId[] responses = new XBeeFrameWithId[0];
        for (XBeeFrameWithId.Builder builder : builders) {
            responses = waitResponses(timeout, sendFramesWithId(builder));
            if (getStatus(responses) != XBeeAtResponseFrame.STATUS_OK)
                break;
        }
        return responses;
    }

    public byte getStatus(XBeeFrameWithId[] responses) {
        int status = XBeeAtResponseFrame.STATUS_OK;
        for (XBeeFrameWithId response : responses) {
            if (response == null)
                return STATUS_TIMEOUT;
            status = Math.max(status, response.getStatus() & 0xff);
        }
        return (byte) status;
    }

    // -------------- HIGH-LEVER PUBLIC OPERATION --------------

    public SerialConnection openTunnel(XBeeAddress destination) {
        return new XBeeTunnel(this, destination);
    }

    public XBeeFrameWithId[] changeRemoteDestination(XBeeAddress destination, XBeeAddress target) throws IOException {
        return sendFramesWithIdSeriallyAndWait(DEFAULT_TIMEOUT,
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DH")
                        .setData(target == null ? new byte[0] : target.getHighAddressBytes()),
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DL")
                        .setData(target == null ? new byte[0] : target.getLowAddressBytes()));
    }


    public XBeeFrameWithId[] resetRemoteHost(XBeeAddress destination) throws IOException {
        // do the actual reset (D3 -> output, low)
        XBeeFrameWithId[] response = waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{4})));
        // restore config (D3 -> disable).
        // don't wait for the second message, because reset was already initiated by the first one
        sendFramesWithId(XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{0}));
        return response;
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
        log.fine("Configuring API mode " + AP_MODE);
        if (XBeeAtResponseFrame.STATUS_OK != getStatus(waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand(AP_COMMAND).setData(AP_MODE)))))
            throw new IOException("No valid XBee device detected. Check that XBee is configured with API firmware and baud rate");
        // enable hardware flow control - RTS & CTS
        log.fine("Configuring RTS and CTS flow control");
        if (XBeeAtResponseFrame.STATUS_OK != getStatus(waitResponses(DEFAULT_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand(RTS_COMMAND).setData(RTS_ON),
                XBeeAtFrame.newBuilder().setAtCommand(CTS_COMMAND).setData(CTS_ON)))))
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

    @SuppressWarnings({"unchecked"})
    private void dispatch(XBeeFrame frame) {
        Object[] listeners = listenerList.getListeners();
        for (int i = 0; i < listeners.length; i += 2) {
            Class<?> frameClass = (Class<Object>) listeners[i];
            if (frameClass.isInstance(frame))
                ((XBeeFrameListener) listeners[i + 1]).frameReceived(frame);
        }
    }

    private class ReaderThread extends Thread {
        ReaderThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                while (!closed.get()) {
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
