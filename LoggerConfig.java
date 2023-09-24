import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerConfig {
    public static Logger configureLogger() {
        Logger logger = Logger.getLogger("Application");

        try {
            FileHandler fh = new FileHandler("myapp.log");
            logger.addHandler(fh);

            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        return logger;
    }
}
