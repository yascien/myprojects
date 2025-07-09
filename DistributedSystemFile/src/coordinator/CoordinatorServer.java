package coordinator;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CoordinatorServer {
    public static void main(String[] args) {
        try {
            CoordinatorService coordinator = new CoordinatorService();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CoordinatorFileSystem", coordinator);
            System.out.println("✅ Successfully started");
            System.out.println("📡 Listening on port 1099");
            System.out.println("🔗 Service bound as 'CoordinatorService'");
        } catch (Exception e) {
            System.err.println("\n❌ Failed to start coordinator:");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}