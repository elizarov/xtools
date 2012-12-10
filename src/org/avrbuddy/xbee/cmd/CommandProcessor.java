package org.avrbuddy.xbee.cmd;

import org.avrbuddy.log.Log;
import org.avrbuddy.util.HexUtil;
import org.avrbuddy.xbee.api.*;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class CommandProcessor {
    private static final Logger log = Log.get(CommandProcessor.class);

    public static final String CMD_DISCOVER = "DISCOVER";
    public static final String CMD_SEND = "SEND";
    public static final String CMD_DEST = "DEST";
    public static final String CMD_RESET = "RESET";
    public static final String CMD_AVR = "AVR";

    public static final String NODE_LOCAL = ".";
    public static final String NODE_ID_PREFIX = "@";

    public static final long SEND_TIMEOUT = XBeeConnection.DEFAULT_TIMEOUT;
    public static final long AT_TIMEOUT = XBeeConnection.DEFAULT_TIMEOUT;

    private final XBeeConnection conn;
    private final XBeeNodeDiscovery discovery;

    public CommandProcessor(XBeeConnection conn, XBeeNodeDiscovery discovery) {
        this.conn = conn;
        this.discovery = discovery;
    }

    public void processCommand(String line) throws InterruptedException {
        try {
            processImpl(line);
        } catch (IllegalArgumentException e) {
            log.log(Level.WARNING, e.getMessage());
            helpCommands();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
        }
    }

    private void processImpl(String line) throws IllegalArgumentException, IOException {
        line = line.trim();
        if (line.isEmpty()) {
            helpCommands();
            return;
        }
        log.fine("Processing '" + line + "'");
        String[] s = line.split("\\s+", 2);
        XBeeAddress destination = resolveNodeAddress(s[0]);
        if (destination != null) {
            if (s.length < 2)
                throw new IllegalArgumentException("command name is missing");
            s = s[1].split("\\s+", 2);
        }
        String cmd = s[0];
        String args = s.length > 1 ? s[1] : null;
        if (cmd.equalsIgnoreCase(CMD_DISCOVER)) {
            if (args != null)
                throw new IllegalArgumentException(CMD_DISCOVER + ": unexpected argument");
            if (destination == null)
                destination = discovery.getOrDiscoverLocalNode().getAddress();
            log.info(CMD_DISCOVER + ": " + destination);
            return;
        }
        if (cmd.equalsIgnoreCase(CMD_SEND)) {
            if (destination == null)
                destination = XBeeAddress.BROADCAST;
            if (args == null)
                throw new IllegalArgumentException(CMD_SEND + ": test is missing");
            List<XBeeFrameWithId> responses =
                    conn.waitResponses(SEND_TIMEOUT,
                            conn.sendFramesWithId(XBeeTxFrame.newBuilder()
                                .setDestination(destination)
                                .setData(HexUtil.parseAscii(args))));
            log.info(CMD_SEND + ": " + fmtStatus(responses, 1));
            return;
        }
        if (cmd.equalsIgnoreCase(CMD_DEST)) {
            XBeeAddress target = args == null ?
                    discovery.getOrDiscoverLocalNode().getAddress() :
                    resolveNodeAddress(args);
            List<XBeeFrameWithId> responses = conn.changeRemoteDestination(destination, target);
            log.info(CMD_DEST + ": " + fmtStatus(responses, 2));
            return;
        }
        if (cmd.equalsIgnoreCase(CMD_RESET)) {
            List<XBeeFrameWithId> responses = conn.resetRemoteHost(destination);
            log.info(CMD_RESET + ": " + fmtStatus(responses, 2));
            return;
        }
        if (cmd.equalsIgnoreCase(CMD_AVR)) {
            if (destination == null)
                throw new IllegalArgumentException(CMD_AVR + ": node is missing");
            conn.openArvProgrammer(destination).close();
            return;
        }
        if (cmd.length() == 2) {
            List<XBeeFrameWithId> responses = conn.waitResponses(AT_TIMEOUT,
                    conn.sendFramesWithId(XBeeAtFrame.newBuilder(destination)
                            .setAtCommand(cmd)
                            .setData(s.length == 1 ? new byte[0] : HexUtil.parseAscii(s[1]))));
            log.info(cmd + ": " + fmtStatus(responses, 1) +
                    (responses.isEmpty() ? "" : " " + HexUtil.formatAscii(responses.get(0).getData())));
            return;
        }
        throw new IllegalArgumentException("Command not recognized: " + cmd);
    }

    private String fmtStatus(List<XBeeFrameWithId> responses, int expectedSize) {
        if (responses.size() < expectedSize)
            return "TIMEOUT";
        int status = Integer.MAX_VALUE;
        for (XBeeFrameWithId response : responses) {
            status = Math.min(status, response.getStatus() & 0xff);
        }
        return status == 0 ? "OK" : HexUtil.formatByte((byte)status);
    }

    public static void helpCommands() {
        System.err.printf("Available commands:%n");
        System.err.printf("  [<node>] '%s'             -- discover node (local by default)%n", CMD_DISCOVER);
        System.err.printf("  [<node>] '%s' <text>          -- send text to node (node is broadcast by default)%n", CMD_SEND);
        System.err.printf("  [<node>] '%s' [<target-node>] -- change destination node with DH,DL (nodes are local by default)%n", CMD_DEST);
        System.err.printf("  [<node>] '%s'                -- reset node with D3 (node is local by default)%n", CMD_RESET);
        System.err.printf("  [<node>] <AT> <args>            -- send any AT command (node is local by default)%n");
        System.err.printf("Where node is one of:%n");
        System.err.printf("  '%s' for local node%n", NODE_LOCAL);
        System.err.printf("  '%s' for broadcast%n", XBeeAddress.BROADCAST_STRING);
        System.err.printf("  '%s' for coordinator node%n", XBeeAddress.COORDINATOR_STRING);
        System.err.printf("  '%s'<NODE-ID> for a given node id%n", NODE_ID_PREFIX);
        System.err.printf("  '['<64-BIT-HEX>[':'<16-BIT-HEX>]']' for a given serial number and optional local address%n");
    }

    public XBeeAddress resolveNodeAddress(String s) throws IllegalArgumentException, IOException {
        if (s.equals(XBeeAddress.COORDINATOR_STRING))
            return XBeeAddress.COORDINATOR;
        if (s.equals(XBeeAddress.BROADCAST_STRING))
            return XBeeAddress.BROADCAST;
        if (s.equals(NODE_LOCAL)) {
            XBeeNode node = discovery.getOrDiscoverLocalNode();
            if (node == null)
                throw new IllegalArgumentException("Failed to resolve local node");
            return node.getAddress();
        } else if (s.startsWith(NODE_ID_PREFIX)) {
            String id = s.substring(NODE_ID_PREFIX.length());
            XBeeNode node = discovery.getOrDiscoverByNodeId(id, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
            if (node == null)
                throw new IllegalArgumentException("Failed to discover remote node " + id);
            return node.getAddress();
        } else if (s.startsWith(XBeeAddress.S_PREFIX))
            return XBeeAddress.valueOf(s);
        return null;
    }
}
