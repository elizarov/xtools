package org.avrbuddy.xbee.cmd;

import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

/**
 * @author Roman Elizarov
 */
public class CommandContext {
    public final XBeeConnection conn;
    public final XBeeNodeDiscovery discovery;

    public CommandContext(XBeeConnection conn, XBeeNodeDiscovery discovery) {
        this.conn = conn;
        this.discovery = discovery;
    }

    public CommandContext(XBeeConnection conn) {
        this(conn, new XBeeNodeDiscovery(conn));
    }
}
