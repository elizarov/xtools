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
    public static final String CMD_SEND_PREFIX = "'";
    public static final String CMD_DEST = "DEST";
    public static final String CMD_RESET = "RESET";

    public static final String NODE_COORDINATOR = "#";
    public static final String NODE_LOCAL = ".";

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
            System.err.println("Error while reading from console");
            e.printStackTrace();
        } catch (InterruptedException e) {
            // ignored - quit
        }
    }

    private void parseLine(String line) throws InterruptedException {
        try {
            processLine(line);
        } catch (IllegalArgumentException e) {
            System.err.println("Command format not recognized: " + e.getMessage());
            help();
        } catch (IOException e) {
            System.err.println("Failed: " + e);
        }
    }

    private void processLine(String line) throws IllegalArgumentException, IOException {
        line = line.trim();
        if (line.isEmpty()) {
            help();
            return;
        }
        String[] s = line.split("\\s+", 2);
        XBeeAddress destination = resolveNode(s[0]);
        line = s[1];
        if (line.startsWith(CMD_SEND_PREFIX)) {
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
        if (cmd.equalsIgnoreCase(CMD_DEST)) {
            XBeeAddress target = XBeeAddress.BROADCAST;
            if (s.length == 2)
                target = resolveNode(s[1]);
            conn.changeRemoteDestination(destination, target);
            return;
        }
        if (cmd.equalsIgnoreCase(CMD_RESET)) {
            conn.resetRemoteHost(destination);
            return;
        }
        if (cmd.equalsIgnoreCase("AVR")) {
            conn.openArvProgrammer(destination).close();
            return;
        }
        System.err.println("Command not recognized: " + cmd);
    }

    private void help() {
        System.err.printf("Use:%n");
        System.err.printf("  <node> %s<text>            -- send text%n", CMD_SEND_PREFIX);
        System.err.printf("  <node> %s <target-node> -- change destination node (DH,DL)%n", CMD_DEST);
        System.err.printf("  <node> RESET              -- reset node (D3)%n", CMD_DEST);
        System.err.printf("  <node> <AT> <args>        -- send any AT command%n", CMD_DEST);
        System.err.printf("Where node is one of:%n");
        System.err.printf("  %s for broadcast%n", XBeeAddress.BROADCAST_STRING);
        System.err.printf("  %s for local node%n", NODE_LOCAL);
        System.err.printf("  %s for coordinator%n", NODE_COORDINATOR);
        System.err.printf("  <NODE-ID> for a given node id%n");
        System.err.printf("  [<8-OR-10-BYTE-HEX>] for a given serial number and optional local address%n");
    }

    private XBeeAddress resolveNode(String s) throws IllegalArgumentException, IOException {
        if (s.equals(NODE_COORDINATOR))
            return XBeeAddress.COORDINATOR;
        if (s.equals(XBeeAddress.BROADCAST_STRING))
            return XBeeAddress.BROADCAST;
        if (s.equals(NODE_LOCAL)) {
            XBeeNode node = discovery.getOrDiscoverLocalNode();
            if (node == null)
                throw new IllegalArgumentException("Failed to resolve local node");
            return node.getAddress();
        }
        if (s.startsWith(XBeeAddress.S_PREFIX))
            return XBeeAddress.valueOf(s);
        XBeeNode node = discovery.getOrDiscoverByNodeId(s, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
        if (node == null)
            throw new IllegalArgumentException("Failed to discover remote node " + s);
        return node.getAddress();
    }
}
