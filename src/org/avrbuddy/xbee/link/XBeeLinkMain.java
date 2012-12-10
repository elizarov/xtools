package org.avrbuddy.xbee.link;

import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeLinkMain {
    private static final Logger log = Log.get(XBeeLinkMain.class);

    private static final long WRITE_TIMEOUT = 100; // 100ms

    public static void main(String[] args) throws IOException {
        Log.init(XBeeLinkMain.class);
        if (args.length != 4) {
            log.log(Level.SEVERE, "Usage: " + XBeeLinkMain.class.getName() + " <XBee-port> <baud> <link-node-id> <link-port>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        String linkNodeId = args[2];
        String linkPort = args[3];
        XBeeConnection conn = XBeeConnection.open(SerialConnection.open(port, baud));
        try {
            new XBeeLinkMain(conn, baud, linkNodeId, linkPort).go();
        } finally {
            conn.close();
        }
    }

    private final XBeeConnection conn;
    private final int baud;
    private final String linkNodeId;
    private final String linkPort;

    public XBeeLinkMain(XBeeConnection conn, int baud, String linkNodeId, String linkPort) {
        this.conn = conn;
        this.baud = baud;
        this.linkNodeId = linkNodeId;
        this.linkPort = linkPort;
    }

    private void go() throws IOException {
        XBeeNodeDiscovery discovery = new XBeeNodeDiscovery(conn);
        XBeeNode linkNode = discovery.getOrDiscoverByNodeId(linkNodeId, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
        if (linkNode == null) {
            log.log(Level.SEVERE, "Failed to discover link node " + linkNodeId);
            return;
        }
        XBeeNode localNode = discovery.getOrDiscoverLocalNode();
        if (localNode == null) {
            log.log(Level.SEVERE, "Failed to resolve local node");
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
                log.log(Level.SEVERE, "Connection to link port established, resetting remote host");
                try {
                    tunnel.resetHost();
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to reset remote host");
                    e.printStackTrace();
                }
            }
        });

        new XBeeLinkThread("remote->link", tunnel.getInput(), link.getOutput()).start();
        new XBeeLinkThread("list->remote", link.getInput(), tunnel.getOutput()).start();
    }

}
