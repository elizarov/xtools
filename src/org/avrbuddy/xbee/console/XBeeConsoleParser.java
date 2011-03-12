package org.avrbuddy.xbee.console;

import org.avrbuddy.util.HexUtil;
import org.avrbuddy.xbee.api.*;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
class XBeeConsoleParser extends Thread {
    private final Logger log = Logger.getLogger(XBeeConsoleParser.class.getName());
    private final XBeeConnection conn;
    private final XBeeNodeDiscovery discovery;

    public XBeeConsoleParser(XBeeConnection conn) {
        super(XBeeConsoleParser.class.getName());
        this.conn = conn;
        discovery = new XBeeNodeDiscovery(conn);
    }

    @Override
    public void run() {
        try {
            discovery.discover();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = in.readLine()) != null)
                parseLine(line);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error while reading from console", e);
        } catch (InterruptedException e) {
            // ignored - quit
        }
    }

    private void parseLine(String line) throws InterruptedException {
        try {
            processLine(line);
        } catch (IllegalArgumentException e) {
            System.err.println("Command format not recognized: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed: " + e);
        }
    }

    private void processLine(String line) throws IOException {
        String[] s = line.split("\\s+", 2);
        if (s.length == 0)
            return;
        XBeeAddress destination = resolveAddress(s[0]);
        if (destination != null) {
            line = s[1];
        }
        if (line.startsWith("'")) {
            if (destination == null)
                destination = XBeeAddress.BROADCAST;
            conn.sendFramesWithId(XBeeTxFrame.newBuilder()
                    .setDestination(destination)
                    .setData(HexUtil.parseAscii(line.substring(1))));
            return;
        }
        s = line.split("\\s+", 2);
        String cmd = s[0];
        if (cmd.length() == 2) {
            conn.sendFramesWithId(XBeeAtFrame.newBuilder(destination)
                    .setAtCommand(cmd)
                    .setData(s.length == 1 ? new byte[0] : HexUtil.parseAscii(s[1])));
            return;
        }
        if (cmd.equalsIgnoreCase("DEST")) {
            XBeeAddress target = null;
            if (s.length == 2) {
                target = resolveAddress(s[1]);
                if (target == null) {
                    System.err.println("Usage: [<address>] DEST [<target-address>]");
                    helpAddress();
                    return;
                }
            }
            dest(destination, target);
            return;
        }
        if (cmd.equalsIgnoreCase("RESET")) {
            conn.resetHost(destination);
            return;
        }
        if (cmd.equalsIgnoreCase("AVR")) {
            conn.openArvProgrammer(destination).close();
            return;
        }
        System.err.println("Command not recognized: " + cmd);
    }

    private void dest(XBeeAddress destination, XBeeAddress target) throws IOException {
        conn.sendFramesWithId(
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DH")
                        .setData(target == null ? new byte[0] : target.getHighAddressBytes()),
                XBeeAtFrame.newBuilder(destination)
                        .setAtCommand("DL")
                        .setData(target == null ? new byte[0] : target.getLowAddressBytes()));
    }

    private void helpAddress() {
        System.err.println("Where address is one of:");
        System.err.println("  '*' for broadcast");
        System.err.println("  '.' for local node");
        System.err.println("  '#' for coordinator");
        System.err.println("  '.<NODE-ID>' for a given node id");
        System.err.println("  '#<64-BIT-HEX>[:<16-BIT-HEX>]' for a given serial number and optional local address");
    }

    private XBeeAddress resolveAddress(String s) throws IOException {
        if (s.equals("#"))
            return XBeeAddress.COORDINATOR;
        if (s.equals(XBeeAddress.BROADCAST_STRING))
            return XBeeAddress.BROADCAST;
        if (s.equals(".")) {
            XBeeNode node = discovery.getOrDiscoverLocalNode();
            if (node == null)
                throw new IllegalArgumentException("Failed to resolve local node");
            return node.getAddress();
        }
        if (s.startsWith(".")) {
            XBeeNode node = discovery.getOrDiscoverById(s.substring(1));
            if (node == null)
                throw new IllegalArgumentException("Failed to remote node " + s);
            return node.getAddress();
        }
        if (s.startsWith("#"))
            return XBeeAddress.valueOf(s.substring(1));
        return null;
    }
}
