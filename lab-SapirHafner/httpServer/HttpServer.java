package httpServer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class HttpServer {
    public static void main(String[] args) {
        int port = 0;
        String root = "";
        String defaultPage = "";
        int maxThreads = 0; 

        try {
            // Load configuration from the file
            Properties properties = loadConfiguration("config.ini");

            // Use configuration values
            port = Integer.parseInt(properties.getProperty("port"));
            root = properties.getProperty("root");
            defaultPage = properties.getProperty("defaultPage");
            maxThreads = Integer.parseInt(properties.getProperty("maxThreads"));
        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return;
        }

        try {
            // Create and start the server thread with the ExecutorService
            ServerListeningThread serverListeningThread = new ServerListeningThread(port, root, defaultPage, maxThreads);
            serverListeningThread.start();
        } catch (Exception e) {
            System.err.println("Error creating server: " + e.getMessage());
            e.printStackTrace();
       }
    }

    public static Properties loadConfiguration(String configFile) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            throw e;
        }
        return properties;
    }
}


