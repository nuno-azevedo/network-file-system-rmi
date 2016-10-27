import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    boolean init(String local_path, String filesystem_path) throws RemoteException;

    boolean close(String local_path) throws RemoteException;

    boolean create(String path) throws RemoteException;

    boolean create(String path, String blob) throws RemoteException;

    boolean del(String path) throws RemoteException;

    boolean get(String path) throws RemoteException;
}

public class StorageServer implements StorageInterface {
    HashMap<String, String> users = new HashMap<String, String>();

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
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    // CALLS FROM CLIENT
    public boolean init(String local_path, String filesystem_path) {  // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        users.put(cleanPath(local_path), cleanPath(filesystem_path));
        return create(filesystem_path);
    }

    public boolean close(String local_path) {  // On close
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        String file_system_path = users.get(cleanPath(local_path));
        users.remove(cleanPath(local_path));
        return del(file_system_path);
    }

    public boolean create(String path) {
        // Example: create("/courses"); -> Creates a directory
        return new File(cleanPath(path)).mkdirs();
    }

    public boolean create(String path, String blob) {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        try {
            Files.write(Paths.get(cleanPath(path)), blob.getBytes());
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
        return true;
    }

    public boolean del(String path) {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        return delete(new File(cleanPath(path)));
    }

    private boolean delete(File path) {
        if (path.isDirectory())
            for (File p : path.listFiles())
                delete(p);
        return path.delete();
    }

    public boolean get(String path) {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        return true;
    }

    private String cleanPath(String path) { return path.replaceAll("^/", "").replaceAll("/$", ""); }
}
