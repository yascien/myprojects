package node;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeService implements Runnable {
    private static final Map<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private final String nodeStoragePath;

    public NodeService(Socket clientSocket, String nodeStoragePath) {
        this.clientSocket = clientSocket;
        this.nodeStoragePath = nodeStoragePath;
    }

    public static Object getFileLock(String filePath) {
        return FILE_LOCKS.computeIfAbsent(filePath, k -> new Object());
    }

    @Override
    public void run() {
        try (DataInputStream input = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

            String command = input.readUTF();
            String department = input.readUTF();
            String filename = input.readUTF();

            switch (command) {
                case "upload":
                    handleFileUpload(input, output, department, filename);
                    break;

                case "read":
                    handleFileRead(output, department, filename);
                    break;

                case "delete":
                    handleFileDeletion(output, department, filename);
                    break;

                case "depFiles":
                    sendDepartmentFilesList(output);
                    break;

                default:
                    output.writeUTF("Invalid command");
                    System.err.println("Received invalid command: " + command);
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFileUpload(DataInputStream input, DataOutputStream output,
                                  String department, String filename) throws IOException {
        File targetFile = new File(nodeStoragePath + "/" + department + "/" + filename);
        targetFile.getParentFile().mkdirs();
        int fileSize = input.readInt();
        byte[] fileData = new byte[fileSize];
        input.readFully(fileData);

        Object fileLock = getFileLock(targetFile.getAbsolutePath());
        synchronized (fileLock) {
            try (RandomAccessFile file = new RandomAccessFile(targetFile, "rw");
                 FileChannel channel = file.getChannel();
                 FileLock lock = channel.lock()) {

                file.setLength(0);
                file.write(fileData);
                output.writeUTF("OK");
                System.out.println("Successfully uploaded file: " + targetFile.getPath());
            } catch (IOException e) {
                output.writeUTF("Failed");
                System.err.println("Failed to upload file: " + targetFile.getPath());
                e.printStackTrace();
            }
        }
    }

    private void handleFileRead(DataOutputStream output, String department,
                                String filename) throws IOException {
        File targetFile = locateTargetFile(department, filename);

        if (targetFile == null || !targetFile.exists()) {
            output.writeInt(-1);
            System.out.println("File not found: " + filename + " in department: " + department);
            return;
        }

        Object fileLock = getFileLock(targetFile.getAbsolutePath());
        synchronized (fileLock) {
            try (RandomAccessFile file = new RandomAccessFile(targetFile, "r");
                 FileChannel channel = file.getChannel();
                 FileLock lock = channel.lock(0L, Long.MAX_VALUE, true)) {

                byte[] fileData = new byte[(int) file.length()];
                file.readFully(fileData);
                output.writeInt(fileData.length);
                output.write(fileData);
                System.out.println("Successfully served file: " + targetFile.getPath());
            }
        }
    }

    private File locateTargetFile(String department, String filename) {
        if (department.equals("all")) {
            File rootDir = new File(nodeStoragePath);
            File[] departmentDirs = rootDir.listFiles(File::isDirectory);

            if (departmentDirs != null) {
                for (File dir : departmentDirs) {
                    File candidateFile = new File(dir, filename);
                    if (candidateFile.exists()) {
                        return candidateFile;
                    }
                }
            }
            return null;
        }
        return new File(nodeStoragePath + "/" + department + "/" + filename);
    }

    private void handleFileDeletion(DataOutputStream output,
                                    String department, String filename) throws IOException {
        File targetFile = new File(nodeStoragePath + "/" + department + "/" + filename);

        if (targetFile.exists() && targetFile.delete()) {
            output.writeUTF("Deleted");
            System.out.println("Successfully deleted file: " + targetFile.getPath());
        } else {
            output.writeUTF("Not Found");
            System.err.println("Failed to delete file: " + targetFile.getPath());
        }
    }

    private void sendDepartmentFilesList(DataOutputStream output) throws IOException {
        File rootDir = new File(nodeStoragePath);
        Map<String, List<String>> departmentFiles = new HashMap<>();

        for (File departmentDir : rootDir.listFiles(File::isDirectory)) {
            List<String> files = new ArrayList<>();
            for (File file : departmentDir.listFiles()) {
                files.add(file.getName());
            }
            departmentFiles.put(departmentDir.getName(), files);
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {

            objectStream.writeObject(departmentFiles);
            objectStream.flush();
            byte[] serializedData = byteStream.toByteArray();

            output.writeInt(serializedData.length);
            output.write(serializedData);
            System.out.println("Sent department files list");
        }
    }
    private static Map<String, List<String>> deserializeFileMap(byte[] data)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Map<String, List<String>>) inputStream.readObject();
        }
    }
    public static void synchronizeNodeFiles(String localNodePath, int localNodePort,
                                            List<String> peerNodes) {
        System.out.printf("\n🔁 [Node %d] Starting file synchronization\n", localNodePort);

        for (String peerAddress : peerNodes) {
            try {
                String[] addressParts = peerAddress.split(":");
                String peerHost = addressParts[0];
                int peerPort = Integer.parseInt(addressParts[1]);

                if (peerPort == localNodePort) {
                    System.out.printf("⏩ [Node %d] Skipping self\n", localNodePort);
                    continue;
                }

                System.out.printf("🔗 [Node %d] Connecting to peer node at %s:%d\n",
                        localNodePort, peerHost, peerPort);

                synchronizeWithPeerNode(localNodePath, localNodePort, peerHost, peerPort);
            } catch (Exception e) {
                System.err.printf("❌ [Node %d] Error with peer %s: %s\n",
                        localNodePort, peerAddress, e.getMessage());
            }
        }
        System.out.printf("✅ [Node %d] Synchronization completed\n", localNodePort);
    }

    private static void synchronizeWithPeerNode(String localNodePath, int localNodePort,
                                                String peerHost, int peerPort) {
        try (Socket peerSocket = new Socket(peerHost, peerPort);
             DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
             DataInputStream in = new DataInputStream(peerSocket.getInputStream())) {

            out.writeUTF("depFiles");
            out.writeUTF("");
            out.writeUTF("");

            System.out.printf("📋 [Node %d] Requesting file list from %s:%d\n",
                    localNodePort, peerHost, peerPort);

            int dataSize = in.readInt();
            byte[] serializedData = new byte[dataSize];
            in.readFully(serializedData);

            Map<String, List<String>> peerFiles = deserializeFileMap(serializedData);
            System.out.printf("📥 [Node %d] Received %d departments from %s:%d\n",
                    localNodePort, peerFiles.size(), peerHost, peerPort);

            for (Map.Entry<String, List<String>> entry : peerFiles.entrySet()) {
                String department = entry.getKey();
                for (String filename : entry.getValue()) {
                    File localFile = Paths.get(localNodePath, department, filename).toFile();

                    if (!localFile.exists()) {
                        System.out.printf("⬇️ [Node %d] Downloading %s/%s from %s:%d\n",
                                localNodePort, department, filename, peerHost, peerPort);
                        downloadFileFromPeer(peerHost, peerPort, department, filename, localNodePath);
                    }
                }
            }
            System.out.printf("✔️ [Node %d] Sync complete with %s:%d\n",
                    localNodePort, peerHost, peerPort);
        } catch (Exception e) {

        }
    }
    private static void downloadFileFromPeer(String peerHost, int peerPort,
                                             String department, String filename,
                                             String localPath) {
        try (Socket socket = new Socket(peerHost, peerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("read");
            out.writeUTF(department);
            out.writeUTF(filename);

            int fileSize = in.readInt();
            if (fileSize == -1) {
                System.err.printf("⚠️ File %s/%s not found on %s:%d\n",
                        department, filename, peerHost, peerPort);
                return;
            }

            byte[] fileData = new byte[fileSize];
            in.readFully(fileData);

            File targetFile = Paths.get(localPath, department, filename).toFile();
            Files.createDirectories(targetFile.getParentFile().toPath());

            try (FileOutputStream fileOut = new FileOutputStream(targetFile)) {
                fileOut.write(fileData);
                System.out.printf("💾 Saved %s/%s (%d bytes) from %s:%d\n",
                        department, filename, fileSize, peerHost, peerPort);
            }
        } catch (IOException e) {
            System.err.printf("❌ Failed to download %s/%s from %s:%d: %s\n",
                    department, filename, peerHost, peerPort, e.getMessage());
        }
    }
}