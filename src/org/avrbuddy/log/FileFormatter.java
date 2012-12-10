package org.avrbuddy.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Roman Elizarov
 */
class FileFormatter extends ConsoleFormatter {
    @Override
    public String format(LogRecord record) {
        String time = new SimpleDateFormat("yyyyMMdd HHmmss.sss").format(new Date(record.getMillis()));
        String name = record.getLoggerName();
        int i = name.lastIndexOf('.');
        if (i > 0)
            name = name.substring(i + 1);
        String str = super.format(record);
        return  time + " " + record.getLevel() + " {" + name + "} " + str;
    }
}
