package org.avrbuddy.xbee.link;

import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
public class XBeeLinkMain {
    private static final long WRITE_TIMEOUT = 100; // 100ms

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: " + XBeeLinkMain.class.getName() + " <XBee-port> <baud> <link-node-id> <link-port>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        String linkNodeId = args[2];
        String linkPort = args[3];
        XBeeConnection conn = XBeeConnection.open(SerialConnection.open(port, baud));
        XBeeNodeDiscovery discovery = new XBeeNodeDiscovery(conn);
        XBeeNode linkNode = discovery.getOrDiscoverByNodeId(linkNodeId, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
        if (linkNode == null) {
            System.err.println("Failed to discover link node " + linkNodeId);
            return;
        }
        XBeeNode localNode = discovery.getOrDiscoverLocalNode();
        if (localNode == null) {
            System.err.println("Failed to resolve local node");
            return;
        }
        conn.changeRemoteDestination(linkNode.getAddress(), localNode.getAddress());

        final SerialConnection tunnel = conn.openTunnel(linkNode.getAddress());
        final SerialConnection link = SerialConnection.open(linkPort, baud);

        tunnel.setWriteTimeout(WRITE_TIMEOUT);
        link.setWriteTimeout(WRITE_TIMEOUT);

        link.setPortConnectionAction(new Runnable() {
            @Override
            public void run() {
                System.err.println("Connection to link port established, resetting remote host");
                try {
                    tunnel.resetHost();
                } catch (IOException e) {
                    System.err.println("Failed to reset remote host");
                    e.printStackTrace();
                }
            }
        });

        new TransferBytes("remote->link", tunnel.getInput(), link.getOutput()).start();
        new TransferBytes("list->remote", link.getInput(), tunnel.getOutput()).start();
    }

    private static class TransferBytes extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public TransferBytes(String name, InputStream in, OutputStream out) {
            super(name);
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            try {
                while (true) {
                    int first = in.read();
                    if (first < 0)
                        break;
                    buf[0] = (byte) first;
                    int n = Math.min(buf.length - 1, in.available());
                    if (n > 0) {
                        n = in.read(buf, 1, n);
                        if (n < 0)
                            break;
                    }
                    out.write(buf, 0, n + 1);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println(getName() + " I/O failed");
                e.printStackTrace();
                return;
            }
            System.err.println(getName() + " end of stream");
        }
    }
}
