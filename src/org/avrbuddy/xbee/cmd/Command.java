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

package org.avrbuddy.xbee.cmd;

import org.avrbuddy.log.Log;
import org.avrbuddy.util.WrongFormatException;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeVisitor;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public abstract class Command implements XBeeNodeVisitor, Cloneable {
    public enum Option {DEST, DEST_REQUIRED, ARG}

    public static final String OK = "OK";
    public static final String FAILED = "FAILED";

    protected final Logger log = Log.getLogger(getClass());

    protected CommandDestination destination;
    protected String name;
    protected String arg;

    public Command() {
        this.name = getClass().getSimpleName().toLowerCase(Locale.US);
    }

    public boolean hasName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public Command clone() {
        try {
            return (Command) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public EnumSet<Option> getOptions() {
        return EnumSet.noneOf(Option.class);
    }

    public String getDestinationDescription() {
        EnumSet<Option> options = getOptions();
        return  !options.contains(Option.DEST) ? "" :
                options.contains(Option.DEST_REQUIRED) ? " <node> " : "[<node>]";
    }

    public String getParameterDescription() {
        return "";
    }

    public abstract String getCommandDescription();

    public String getMoreHelp() {
        return null;
    }

    // returns true if command executed (successfully or not), false is user needs help
    public final boolean execute(CommandContext ctx) {
        String result;
        try {
            validate(ctx);
            result = invoke(ctx);
        } catch (WrongFormatException e) {
            log.log(Level.WARNING, name, e);
            return false;
        } catch (Exception e) {
            log.log(Level.SEVERE, name, e);
            return true;
        }
        if (result != null)
            log.info(name + ": " + result);
        return true;
    }

    public void validate(CommandContext ctx) {
        if (getOptions().contains(Option.DEST_REQUIRED) && destination == null)
            throw new WrongFormatException("Destination node is required");
        if (destination != null && !getOptions().contains(Option.DEST))
            throw new WrongFormatException("Does not support destination");
        if (arg != null && !getOptions().contains(Option.ARG))
            throw new WrongFormatException("Does not expect arguments");
    }

    protected abstract String invoke(CommandContext ctx) throws IOException;

    @Override
    public void visitNode(XBeeNode node) {
        log.info(name + ": " + node);
    }

    public CommandDestination getDestination() {
        return destination;
    }

    public void setDestination(CommandDestination destination) {
        this.destination = destination;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public String getHelpName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (destination != null)
            sb.append(destination).append(' ');
        sb.append(name);
        if (arg != null)
            sb.append(' ').append(arg);
        return sb.toString();
    }
}
