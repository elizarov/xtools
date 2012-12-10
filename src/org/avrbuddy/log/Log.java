package org.avrbuddy.log;

import org.omg.CORBA.IMP_LIMIT;

import java.io.IOException;
import java.util.logging.*;

/**
 * @author Roman Elizarov
 */
public class Log {
    private static final int LOG_FILE_LIMIT = 10 * 1024 * 1024; // 10MB

    private Log() {}

    public static void init(Class<?> mainClass) throws IOException {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        Handler[] defaultHandlers = root.getHandlers();
        for (Handler handler : defaultHandlers)
            root.removeHandler(handler);
        root.setLevel(Level.FINEST);
        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new ConsoleFormatter());
        console.setLevel(System.getProperty("verbose") != null ? Level.FINE : Level.INFO);
        root.addHandler(console);
        FileHandler file = new FileHandler(mainClass.getSimpleName() + ".%g.log", LOG_FILE_LIMIT, 2);
        file.setFormatter(new FileFormatter());
        file.setLevel(Level.FINEST);
        root.addHandler(file);
        Logger main = Logger.getLogger(mainClass.getName());
        main.info("Started " + mainClass.getSimpleName());
    }

    public static Logger get(Class<?> loggingClass) {
        return Logger.getLogger(loggingClass.getName());
    }
}
