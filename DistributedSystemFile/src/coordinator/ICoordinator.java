package coordinator;
import java.rmi.Remote;
import java.rmi.RemoteException;
import utils.User;

public interface ICoordinator extends Remote {
    String signIn(String username, String password) throws RemoteException;
    boolean createUser(User user, String token) throws RemoteException;
    boolean saveFile(String token, String filename, byte[] data) throws RemoteException;
    byte[] getFile(String token, String filename) throws RemoteException;
    boolean removeFile(String token, String filename) throws RemoteException;
    boolean updateFile(String token, String filename, byte[] updateData) throws RemoteException;
}
