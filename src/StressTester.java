import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StressTester {

    // Settings to break the server
    private static final int NUMBER_OF_CLIENTS = 20;   // Simulate 20 ESPs
    private static final int MESSAGES_PER_CLIENT = 50; // Each sends 50 logs
    private static final String SERVER_IP = "127.0.0.1"; // Localhost
    private static final int SERVER_PORT = 1234;       // Your ESP port

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- STARTING STRESS TEST ---");
        System.out.println("Target: " + NUMBER_OF_CLIENTS + " clients sending " + MESSAGES_PER_CLIENT + " messages each.");
        System.out.println("Expected total logs: " + (NUMBER_OF_CLIENTS * MESSAGES_PER_CLIENT));

        // ExecutorService helps us run many threads at once
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_CLIENTS);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            final int id = i;
            executor.submit(() -> runFakeClient(id));
        }

        // Wait for all to finish
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        System.out.println("--- TEST FINISHED in " + (endTime - startTime) + "ms ---");
        System.out.println("Check your log.txt! Does it have exactly " + (NUMBER_OF_CLIENTS * MESSAGES_PER_CLIENT) + " new lines?");
    }

    private static void runFakeClient(int id) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // 1. Send Handshake (MAC Address)
            String fakeMac = "FAKE:MAC:" + id;
            out.println(fakeMac);

            // 2. Spam messages
            for (int j = 0; j < MESSAGES_PER_CLIENT; j++) {
                out.println("Stress test message " + j + " from client " + id);
                // We do NOT sleep here. We want to be as fast/aggressive as possible.
            }

        } catch (Exception e) {
            System.err.println("Client " + id + " failed: " + e.getMessage());
        }
    }
}