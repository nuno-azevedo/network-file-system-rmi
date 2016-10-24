import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    public boolean init(String local_path, String filesystem_path) throws RemoteException;

    public boolean close(String local_path) throws RemoteException;

    public boolean create(String path) throws RemoteException;

    public boolean create(String path, String blob) throws RemoteException;

    public boolean del(String path) throws RemoteException;

    public boolean get(String path) throws RemoteException;
}

public class StorageServer implements StorageInterface {
    private StorageServer() {
        // Class constructor
    }

    public static void main(String args[]) {
        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("StorageServer", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    // CALLS FROM CLIENT
    public boolean init(String local_path, String filesystem_path) {  // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        String path = "storage" + (filesystem_path.startsWith("/") ? filesystem_path : "/" + filesystem_path);
        boolean success = (new File(path)).mkdirs();
        if (!success) {
            System.err.println("Failed to create filesystem dir.");
            return false;
        }
        return true;
    }

    public boolean close(String local_path) {  // On close
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        return false;
    }

    public boolean create(String path) {
        // Example: create("/courses"); -> Creates a directory
        return false;
    }

    public boolean create(String path, String blob) {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        return false;
    }

    public boolean del(String path) {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        return false;
    }

    public boolean get(String path) {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        return false;
    }
}
