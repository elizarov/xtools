package org.avrbuddy.xbee.cmd;

import org.avrbuddy.log.Log;
import org.avrbuddy.util.HexUtil;
import org.avrbuddy.xbee.api.*;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class CommandProcessor {
    private static final Logger log = Log.getLogger(CommandProcessor.class);

    public static final String COMMENT_PREFIX = "#";

    public static final String CMD_DISCOVER = "DISCOVER";
    public static final String CMD_SEND = "SEND";
    public static final String CMD_DEST = "DEST";
    public static final String CMD_RESET = "RESET";
    public static final String CMD_AVR = "AVR";

    public static final String NODE_LOCAL = ".";
    public static final String NODE_ID_PREFIX = "@";

    private final XBeeConnection conn;
    private final XBeeNodeDiscovery discovery;

    public CommandProcessor(XBeeConnection conn, XBeeNodeDiscovery discovery) {
        this.conn = conn;
        this.discovery = discovery;
    }

    public String processCommand(String line) throws InterruptedException {
        try {
            return processImpl(line);
        } catch (InvalidCommandException e) {
            log.log(Level.WARNING, e.getMessage());
            helpCommands();
        } catch (IllegalArgumentException e) {
            log.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
        }
        return null;
    }

    private String processImpl(String line) throws IllegalArgumentException, IOException {
        line = line.trim();
        if (line.isEmpty() || line.startsWith(COMMENT_PREFIX))
            return null;
        log.fine("Processing '" + line + "'");
        String[] s = line.split("\\s+", 2);
        XBeeAddress destination = resolveNodeAddress(s[0]);
        if (destination != null) {
            if (s.length < 2)
                throw new InvalidCommandException("command name is missing");
            s = s[1].split("\\s+", 2);
        }
        String cmd = s[0];
        String args = s.length > 1 ? s[1] : null;
        if (cmd.equalsIgnoreCase(CMD_DISCOVER)) {
            if (args != null)
                throw new InvalidCommandException(CMD_DISCOVER + ": unexpected argument");
            if (destination == null)
                destination = discovery.getOrDiscoverLocalNode().getAddress();
            return CMD_DISCOVER + ": " + destination;
        }
        if (cmd.equalsIgnoreCase(CMD_SEND)) {
            if (destination == null)
                destination = XBeeAddress.BROADCAST;
            if (args == null)
                throw new InvalidCommandException(CMD_SEND + ": text is missing");
            XBeeFrameWithId[] responses =
                    conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT,
                            conn.sendFramesWithId(XBeeTxFrame.newBuilder()
                                    .setDestination(destination)
                                    .setData(HexUtil.parseAscii(args))));
            return CMD_SEND + ": " + fmtStatus(responses);
        }
        if (cmd.equalsIgnoreCase(CMD_DEST)) {
            XBeeAddress target = args == null ?
                    discovery.getOrDiscoverLocalNode().getAddress() :
                    resolveNodeAddress(args);
            XBeeFrameWithId[] responses = conn.changeRemoteDestination(destination, target);
            return CMD_DEST + ": " + fmtStatus(responses);
        }
        if (cmd.equalsIgnoreCase(CMD_RESET)) {
            XBeeFrameWithId[] responses = conn.resetRemoteHost(destination);
            return CMD_RESET + ": " + fmtStatus(responses);
        }
        if (cmd.equalsIgnoreCase(CMD_AVR)) {
            if (destination == null)
                throw new InvalidCommandException(CMD_AVR + ": node is missing");
            conn.openArvProgrammer(destination).close();
            return CMD_AVR + ": OK";
        }
        if (cmd.length() == 2) {
            XBeeFrameWithId[] responses = conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT,
                    conn.sendFramesWithId(XBeeAtFrame.newBuilder(destination)
                            .setAtCommand(cmd)
                            .setData(s.length == 1 ? new byte[0] : XBeeUtil.parseAtValue(cmd, s[1]))));
            String result = "";
            if (responses[0] != null) {
                byte[] data = responses[0].getData();
                if (data.length > 0)
                    result = " " + XBeeUtil.formatAtValue(cmd, data);
            }
            return cmd + ": " + fmtStatus(responses) + result;
        }
        throw new InvalidCommandException("Command not recognized: " + cmd);
    }

    private String fmtStatus(XBeeFrameWithId[] responses) {
        byte status = conn.getStatus(responses);
        switch (status) {
            case XBeeConnection.STATUS_TIMEOUT:
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
                return HexUtil.formatByte(status);
        }
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
