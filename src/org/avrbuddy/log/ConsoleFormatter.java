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
            return message + "\n";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        record.getThrown().printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
