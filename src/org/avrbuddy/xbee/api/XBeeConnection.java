package org.avrbuddy.xbee.api;

import org.avrbuddy.avr.AvrProgrammer;
import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeConnection {
    private static final Logger log = Log.get(XBeeConnection.class);

    public static final long DEFAULT_TIMEOUT = 3000;

    private static final String AP_COMMAND = "AP";
    private static final byte AP_FRAME_ID = 1;

    private static final String RTS_COMMAND = "D6";
    private static final byte RTS_FRAME_ID = 2;
    private static final byte RTS_ON = 1;

    private static final String CTS_COMMAND = "D7";
    private static final byte CTS_FRAME_ID = 3;
    private static final byte CTS_ON = 1;

    private static final int STATE_NEW = 0;
    private static final int STATE_AP_CHECKED = 1;
    private static final int STATE_RTS_ON = 2;
    private static final int STATE_CTS_ON = 4;

    private static final long RESET_TIMEOUT = DEFAULT_TIMEOUT;
    private static final long DEST_TIMEOUT = DEFAULT_TIMEOUT;

    private final SerialConnection serial;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Thread reader;
    private final XBeeFrameListenerList listenerList = new XBeeFrameListenerList();

    private byte lastFrameId = AP_FRAME_ID;
    private int state = STATE_NEW;

    public static XBeeConnection open(SerialConnection serial) throws IOException {
        XBeeConnection conn = new XBeeConnection(serial);
        conn.start();
        // enable inbound flow control to make sure we can receive all answers (signal RTS)
        serial.setHardwareFlowControl(SerialConnection.FLOW_CONTROL_IN);
        // check AT setting
        try {
            conn.sendFrames(XBeeAtFrame.newBuilder().setAtCommand(AP_COMMAND).setFrameId(AP_FRAME_ID).build());
        } catch (IOException e) {
            conn.close();
            throw e;
        }
        if (!conn.waitState(STATE_AP_CHECKED)) {
            conn.close();
            throw new IOException("No valid XBee device detected. Check that XBee is configured with API firmware and AP is set to 2");
        }
        // enable hardware flow control - RTS
        try {
            conn.sendFrames(
                    XBeeAtFrame.newBuilder().setAtCommand(RTS_COMMAND).
                            setData(new byte[]{RTS_ON}).setFrameId(RTS_FRAME_ID).build());
        } catch (IOException e) {
            conn.close();
            throw e;
        }
        if (!conn.waitState(STATE_RTS_ON)) {
            conn.close();
            throw new IOException("Failed to enable RTS on XBee");
        }
        // enable hardware flow control - CTS
        try {
            conn.sendFrames(
                    XBeeAtFrame.newBuilder().setAtCommand(CTS_COMMAND).
                            setData(new byte[]{CTS_ON}).setFrameId(CTS_FRAME_ID).build());
        } catch (IOException e) {
            conn.close();
            throw e;
        }
        if (!conn.waitState(STATE_CTS_ON)) {
            conn.close();
            throw new IOException("Failed to enable CTS on XBee");
        }
        // enable outbound flow control
        serial.setHardwareFlowControl(SerialConnection.FLOW_CONTROL_IN | SerialConnection.FLOW_CONTROL_OUT);
        // done
        return conn;
    }

    private XBeeConnection(SerialConnection serial) {
        this.serial = serial;
        in = new DataInputStream(new UnescapeStream(serial.getInput()));
        out = new DataOutputStream(new EscapeStream(serial.getOutput()));
        reader = new Thread("XBeeReader-" + serial) {
            @Override
            public void run() {
                runReader();
            }
        };
    }

    private void start() {
        reader.start();
    }

    private synchronized boolean waitState(int mask) throws IOException {
        try {
            wait(1000);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        return (state & mask) != 0;
    }

    private synchronized void updateState(int state) {
        this.state |= state;
        notifyAll();
    }

    public void close() {
        reader.interrupt();
        serial.close();
    }

    public <F> void addListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        listenerList.addListener(frameClass, listener);
    }

    public <F> void removeListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        listenerList.removeListener(frameClass, listener);
    }

    public SerialConnection openTunnel(XBeeAddress destination) {
        return new XBeeTunnel(this, destination);
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

    private XBeeFrameWithId buildFrameWithId(XBeeFrameWithId.Builder builder) {
        return builder.setFrameId(nextFrameId()).build();
    }

    private void sendFrameInternal(XBeeFrame frame) throws IOException {
        byte[] data = frame.getFrame();
        serial.getOutput().write(data[0]);
        out.write(data, 1, data.length - 1);
        out.flush();
    }

    private void runReader() {
        try {
            while (!Thread.interrupted()) {
                XBeeFrame frame = nextFrame();
                log.fine("<- " + frame);
                dispatch(frame);
            }
        } catch (InterruptedIOException e) {
            // ignored, exit
        } catch (IOException e) {
            log.log(Level.SEVERE, "IO Exception", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void dispatch(XBeeFrame frame) {
        if (frame instanceof XBeeAtResponseFrame)
            checkAtResponse(((XBeeAtResponseFrame) frame));
        Object[] listeners = listenerList.getListeners();
        for (int i = 0; i < listeners.length; i += 2) {
            Class<?> frameClass = (Class<Object>) listeners[i];
            if (frameClass.isInstance(frame))
                ((XBeeFrameListener) listeners[i + 1]).frameReceived(frame);
        }
    }

    private void checkAtResponse(XBeeAtResponseFrame frame) {
        if (frame.getAtCommand().equals(AP_COMMAND) && frame.getFrameId() == AP_FRAME_ID &&
                frame.getStatus() == XBeeAtResponseFrame.STATUS_OK &&
                frame.getData().length == 1 && frame.getData()[0] == 2)
            updateState(STATE_AP_CHECKED);
        else if (frame.getAtCommand().equals(RTS_COMMAND) && frame.getFrameId() == RTS_FRAME_ID &&
                frame.getStatus() == XBeeAtResponseFrame.STATUS_OK)
            updateState(STATE_RTS_ON);
        else if (frame.getAtCommand().equals(CTS_COMMAND) && frame.getFrameId() == CTS_FRAME_ID &&
                frame.getStatus() == XBeeAtResponseFrame.STATUS_OK)
            updateState(STATE_CTS_ON);
    }

    private XBeeFrame nextFrame() throws IOException {
        while (true) {
            int skipped = 0;
            while (serial.getInput().read() != XBeeUtil.FRAME_START)
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

    private byte nextFrameId() {
        lastFrameId++;
        if (lastFrameId == 0)
            lastFrameId = 1;
        return lastFrameId;
    }

    public List<XBeeFrameWithId> waitResponses(long timeout, XBeeFrameWithId... frames) throws InterruptedIOException {
        final List<XBeeFrameWithId> responses = new ArrayList<XBeeFrameWithId>();
        final List<XBeeFrameWithId> waitList = new ArrayList<XBeeFrameWithId>(Arrays.asList(frames));
        XBeeFrameListener<XBeeFrameWithId> listener = new XBeeFrameListener<XBeeFrameWithId>() {
            public void frameReceived(XBeeFrameWithId frame) {
                synchronized (waitList) {
                    for (Iterator<XBeeFrameWithId> iterator = waitList.iterator(); iterator.hasNext();) {
                        XBeeFrameWithId waitFrame = iterator.next();
                        if (frame.isResponseFor(waitFrame)) {
                            iterator.remove();
                            responses.add(frame);
                        }
                    }
                    if (waitList.isEmpty())
                        waitList.notifyAll();
                }
            }
        };
        addListener(XBeeFrameWithId.class, listener);
        try {
            synchronized (waitList) {
                waitList.wait(timeout);
            }
        } catch (InterruptedException e) {
            throw ((InterruptedIOException) new InterruptedIOException().initCause(e));
        } finally {
            removeListener(XBeeFrameWithId.class, listener);
        }
        return responses;
    }

    public List<XBeeFrameWithId> changeRemoteDestination(XBeeAddress destination, XBeeAddress target) throws IOException {
        return waitResponses(DEST_TIMEOUT, sendFramesWithId(
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DH")
                        .setData(target == null ? new byte[0] : target.getHighAddressBytes()),
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DL")
                        .setData(target == null ? new byte[0] : target.getLowAddressBytes())));
    }

    public List<XBeeFrameWithId> resetRemoteHost(XBeeAddress destination) throws IOException {
        return waitResponses(RESET_TIMEOUT,
                sendFramesWithId(
                        XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{5}),
                        XBeeAtFrame.newBuilder(destination).setAtCommand("D3").setData(new byte[]{4})));
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
}
