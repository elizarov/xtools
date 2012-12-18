package org.avrbuddy.xbee.cmd;

import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeVisitor;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public abstract class Command implements XBeeNodeVisitor, Cloneable {
    public enum Option { DESTINATION, PARAMETER }

    public static final String OK = "OK";
    public static final String FAILED = "FAILED";

    protected final Logger log = Log.getLogger(getClass());

    protected CommandDestination destination;
    protected String name;
    protected String parameter;

    public Command() {
        this.name = getClass().getSimpleName().toUpperCase(Locale.US);
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

    public String getParameterDescription() {
        return "";
    }

    public abstract String getCommandDescription();

    public void validate() {}

    public void execute(CommandContext ctx) throws IOException {
        validate();
        String result = executeImpl(ctx);
        if (result != null)
            log.info(name + ": " + result);
    }

    protected abstract String executeImpl(CommandContext ctx) throws IOException;

    @Override
    public void visitNode(XBeeNode node) {
        log.info(name + ": " + node);
    }

    public CommandDestination getDestination() {
        return destination;
    }

    public void setDestination(CommandDestination destination) {
        if (destination != null && !getOptions().contains(Option.DESTINATION))
            throw new InvalidCommandException(name + " does not support destination");
        this.destination = destination;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        if (parameter != null && !getOptions().contains(Option.PARAMETER))
            throw new InvalidCommandException(name + " does not expect arguments");
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (destination != null)
            sb.append(destination).append(' ');
        sb.append(name);
        if (parameter != null)
            sb.append(' ').append(parameter);
        return sb.toString();
    }
}
