package coordinator;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import utils.User;
import utils.Token;

public class CoordinatorService extends UnicastRemoteObject implements ICoordinator {
    private final Map<String, String> credentials = new HashMap<>(); // username -> password
    private final Map<String, User> userProfiles = new HashMap<>();  // username -> User object
    private final Map<String, Integer> nodeLoadMap = new ConcurrentHashMap<>();
    private final List<String> storageNodes = Arrays.asList("127.0.0.1:3001", "127.0.0.1:3002", "127.0.0.1:3003");

    public CoordinatorService() throws RemoteException {
        credentials.put("admin", "admin123");
        credentials.put("yassen", "12345678");
        userProfiles.put("admin", new User("manager", "admin", "System"));
        userProfiles.put("yassen", new User("manager", "user", "all"));
    }

    @Override
    public String signIn(String username, String password) throws RemoteException {
        if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
            return Token.generateToken(userProfiles.get(username));
        }
        return null;
    }


    @Override
    public boolean createUser(User newUser, String adminToken) throws RemoteException {
        User admin = Token.validateToken(adminToken);
        if (admin != null && "admin".equals(admin.getRole())) {
            credentials.put(newUser.getUsername(), "12");
            userProfiles.put(newUser.getUsername(), newUser);
            return true;
        }
        return false;
    }
    @Override
    public boolean saveFile(String authToken, String filename, byte[] fileData) throws RemoteException {
        User user = Token.validateToken(authToken);
        if (user == null) return false;

        String department = user.getDepartment();
        int successCount = 0;
        int quorum = Math.max(1, storageNodes.size() / 2 + 1);

        for (String nodeAddress : storageNodes) {
            String[] parts = nodeAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (
                    Socket socket = new Socket(host, port);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream())
            ) {
                out.writeUTF("upload");
                out.writeUTF(department);
                out.writeUTF(filename);
                out.writeInt(fileData.length);
                out.write(fileData);

                if ("OK".equals(in.readUTF())) {
                    successCount++;
                    System.out.println("File saved to node: " + nodeAddress);
                }
            } catch (IOException e) {
                System.out.println("Node unreachable: " + nodeAddress);
            }

            if (successCount >= quorum) return true; // Quorum achieved
        }
        return false;
    }

    @Override
    public byte[] getFile(String authToken, String filename) throws RemoteException {
        User user = Token.validateToken(authToken);
        if (user == null) return null;

        // Initialize node loads if missing
        synchronized (nodeLoadMap) {
            for (String nodeAddress : storageNodes) {
                nodeLoadMap.putIfAbsent(nodeAddress, 0);
            }
        }

        // Prioritize nodes with the least load
        List<String> prioritizedNodes = new ArrayList<>(storageNodes);
        prioritizedNodes.sort(Comparator.comparingInt(nodeLoadMap::get));

        ExecutorService executor = Executors.newFixedThreadPool(storageNodes.size());
        CompletionService<byte[]> completionService = new ExecutorCompletionService<>(executor);

        for (String nodeAddress : prioritizedNodes) {
            completionService.submit(() -> {
                nodeLoadMap.compute(nodeAddress, (k, v) -> v + 1);

                String[] parts = nodeAddress.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                try (
                        Socket socket = new Socket(host, port);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        DataInputStream in = new DataInputStream(socket.getInputStream())
                ) {
                    out.writeUTF("read");
                    out.writeUTF("all");
                    out.writeUTF(filename);

                    int fileSize = in.readInt();
                    if (fileSize == -1) return null; // File not found

                    byte[] fileData = new byte[fileSize];
                    in.readFully(fileData);
                    return fileData;
                } catch (IOException e) {
                    return null;
                } finally {
                    nodeLoadMap.compute(nodeAddress, (k, v) -> v - 1); // Decrement load
                }
            });
        }
        try {
            for (int i = 0; i < prioritizedNodes.size(); i++) {
                byte[] result = completionService.take().get();
                if (result != null) {
                    executor.shutdownNow();
                    return result;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        return null;
    }


    @Override
    public boolean removeFile(String authToken, String filename) throws RemoteException {
        User user = Token.validateToken(authToken);
        if (user == null) return false;

        String department = user.getDepartment();
        int successCount = 0;
        int quorum = Math.max(1, storageNodes.size() / 2 + 1);

        for (String nodeAddress : storageNodes) {
            String[] parts = nodeAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (
                    Socket socket = new Socket(host, port);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream())
            ) {
                out.writeUTF("delete");
                out.writeUTF(department);
                out.writeUTF(filename);

                if ("Deleted".equals(in.readUTF())) {
                    successCount++;
                    System.out.println("File deleted from node: " + nodeAddress);
                }
            } catch (IOException e) {
                System.out.println("Failed to delete from node: " + nodeAddress);
            }

            if (successCount >= quorum) return true;
        }
        return false;
    }

    @Override
    public boolean updateFile(String authToken, String filename, byte[] updatedData) throws RemoteException {
        return saveFile(authToken, filename, updatedData);
    }
}
