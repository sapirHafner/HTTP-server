package httpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerListeningThread extends Thread {
    private int port;
    private String root;
    private String defaultPage;
    private ServerSocket serverSocket;
    private static ExecutorService threadPool;

    public ServerListeningThread(int port, String root, String defaultPage, int maxThreads) throws IOException {
        this.port = port;
        this.root = root;
        this.defaultPage = defaultPage;

        // Initialize threadPool only once
        if (threadPool == null) {
            synchronized (ServerListeningThread.class) {
                if (threadPool == null) {
                    // Create a fixed-size thread pool with the specified maximum number of threads
                    threadPool = Executors.newFixedThreadPool(maxThreads);
                }
            }
        }

        this.serverSocket = new ServerSocket(this.port);
    }

    @Override
    public void run() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                // Use the shared ExecutorService to execute the worker thread
                threadPool.execute(new ClientHandle(socket, defaultPage, root));
            }
        } catch (IOException e) {
            System.err.println("Error while server is listening: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error while closing server socket: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}