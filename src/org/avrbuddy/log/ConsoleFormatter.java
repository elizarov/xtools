package org.avrbuddy.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Roman Elizarov
 */
class ConsoleFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String message = formatMessage(record);
        if (record.getThrown() == null)
            return String.format("%s%n", message);
        return String.format("%s: %s%n", message, record.getThrown().getMessage());
    }
}
