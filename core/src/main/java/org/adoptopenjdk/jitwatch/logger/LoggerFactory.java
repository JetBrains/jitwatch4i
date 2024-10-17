package org.adoptopenjdk.jitwatch.logger;

import java.nio.file.Path;

public class LoggerFactory {
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    public static void setLogFile(Path path) {
        // relax for now
    }
}
