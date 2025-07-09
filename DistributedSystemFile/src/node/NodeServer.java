package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static node.NodeService.synchronizeNodeFiles;

public class NodeServer {
    private static final int NODE_COUNT = 3;
    private static final int BASE_PORT = 3001;
    private static final String[] NODE_ADDRESSES = {"127.0.0.1:3001", "127.0.0.1:3002", "127.0.0.1:3003"};
    private static final int SYNC_INTERVAL_DAILY = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    public static void main(String[] args) {
        System.out.println("=== Starting Distributed Node Cluster ===");

        for (int i = 0; i < NODE_COUNT; i++) {
            final int nodeId = i + 1;
            final int port = BASE_PORT + i;
            final String storageRoot = "folder" + nodeId;

            System.out.printf("\n🚀 Initializing Node %d:\n", nodeId);
            System.out.printf("  - Port: %d\n", port);
            System.out.printf("  - Storage: %s\n", storageRoot);

            startNodeServer(port, storageRoot);
            scheduleDailySync(storageRoot, port);
        }

        System.out.println("\n✅ All nodes initialized and running");
    }

    private static void startNodeServer(int port, String storageRoot) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.printf("🔄 Node on port %d listening for connections...\n", port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.printf("🔗 New connection received on port %d\n", port);
                    new Thread(new NodeService(clientSocket, storageRoot)).start(); }
            } catch (IOException e) {
                System.err.printf("❌ Node on port %d failed: %s\n", port, e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

        private static void scheduleDailySync(String storageRoot, int port) {
            long initialDelay = calculateInitialSyncDelay();
            System.out.printf("⏰ Scheduled daily sync for node %s starting in %d minutes\n",
                    storageRoot, initialDelay / (60 * 1000));
    
            synchronizeNodeFiles(storageRoot, port, Arrays.asList(NODE_ADDRESSES));
    
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.printf("\n🔄 Starting daily synchronization for node %s...\n", storageRoot);
                    System.out.printf("✅ Completed synchronization for node %s\n", storageRoot);
                }
            }, initialDelay, SYNC_INTERVAL_DAILY);
        }

    private static long calculateInitialSyncDelay() {
        Calendar now = Calendar.getInstance();
        Calendar syncTime = Calendar.getInstance();

        // Set sync time to midnight
        syncTime.set(Calendar.HOUR_OF_DAY, 0);
        syncTime.set(Calendar.MINUTE, 0);
        syncTime.set(Calendar.SECOND, 0);
        syncTime.set(Calendar.MILLISECOND, 0);

        // If we already passed midnight today, schedule for midnight tomorrow
        if (now.after(syncTime)) {
            syncTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return syncTime.getTimeInMillis() - now.getTimeInMillis();
    }
}