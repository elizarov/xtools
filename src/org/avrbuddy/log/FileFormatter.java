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

package org.avrbuddy.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Roman Elizarov
 */
class FileFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String time = new SimpleDateFormat("yyyyMMdd HHmmss.SSS").format(new Date(record.getMillis()));
        String name = record.getLoggerName();
        int i = name.lastIndexOf('$');
        if (i > 0)
            name = name.substring(i + 1);
        i = name.lastIndexOf('.');
        if (i > 0)
            name = name.substring(i + 1);
        String str = formatFullMessage(record);
        return  time + " " + record.getLevel() + " {" + name + "} " + str;
    }

    private String formatFullMessage(LogRecord record) {
        String message = formatMessage(record);
        Throwable thrown = record.getThrown();
        if (thrown == null)
            return String.format("%s%n", message);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message == null ? thrown.getMessage() : message);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
