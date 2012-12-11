package org.avrbuddy.log;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.*;

/**
 * @author Roman Elizarov
 */
public class Log {
    private static final int LOG_FILE_LIMIT = 10 * 1024 * 1024; // 10MB

    private Log() {}

    public static void init(Class<?> mainClass) {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        Handler[] defaultHandlers = root.getHandlers();
        for (Handler handler : defaultHandlers)
            root.removeHandler(handler);
        root.setLevel(Level.FINEST);
        Logger main = Logger.getLogger(mainClass.getName());

        // Console
        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new ConsoleFormatter());
        console.setLevel(System.getProperty("verbose") != null ? Level.FINE : Level.INFO);
        root.addHandler(console);

        // Log file
        String logFile = mainClass.getSimpleName().toLowerCase(Locale.US) + ".log";
        FileHandler file;
        try {
            file = new FileHandler(logFile, LOG_FILE_LIMIT, 1, true);
            file.setFormatter(new FileFormatter());
            file.setLevel(Level.FINEST);
            root.addHandler(file);
        } catch (IOException e) {
            main.log(Level.SEVERE, "Failed to open log file " + logFile, e);
        }

        // Done
        main.info("------------ Started " + mainClass.getSimpleName() + " ------------");
    }

    public static Logger getLogger(Class<?> loggingClass) {
        return Logger.getLogger(loggingClass.getName());
    }
}
