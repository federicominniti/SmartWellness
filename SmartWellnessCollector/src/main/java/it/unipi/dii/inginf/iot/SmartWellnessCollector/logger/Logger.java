package it.unipi.dii.inginf.iot.SmartWellnessCollector.logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Simple class to perform logging of the data from sensors and status information for the actuators
 */
public class Logger {
    private static Logger instance;
    private static java.util.logging.Logger logger;

    public static Logger getInstance()
    {
        if(instance == null)
            instance = new Logger();

        return instance;
    }

    private Logger ()
    {
        logger = java.util.logging.Logger.getLogger(Logger.class.getName());
        try {
            FileHandler fileHandler = new FileHandler("./info.log");
            logger.addHandler(fileHandler);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord logRecord) {
                    return logRecord.getMessage() + "\n";
                }
            });
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log (String topic, String message)
    {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        logger.info("[" + topic + " - " + timestamp + "] " + message);
    }

    public void logInfo(String message)
    {
        log("DATA", message);
    }
    public void logStatus(String message) { log("STATUS", message); }
    public void logError(String message) { log("ERROR", message); }
}