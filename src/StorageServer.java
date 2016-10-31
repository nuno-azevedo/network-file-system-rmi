import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    boolean init(String local_path, String filesystem_path) throws RemoteException, UnknownHostException;

    boolean close(String local_path) throws RemoteException;

    boolean create(String path) throws RemoteException;

    boolean create(String path, String blob) throws IOException;

    boolean del(String path) throws RemoteException;

    File get(String path) throws RemoteException;
}

public class StorageServer implements StorageInterface {
    private static MetaDataInterface metaData;
    private HashMap<String, String> users;

    private StorageServer() {
        this.users = new HashMap<String, String>();
    }

    public static void main(String args[]) {
        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("StorageServer", stub);

            metaData = (MetaDataInterface) registry.lookup("MetaDataServer");
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    // CALLS FROM CLIENT
    public boolean init(String local_path, String filesystem_path) throws UnknownHostException, RemoteException {
        // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        String clean_local_path = cleanPath(local_path);
        String clean_filesystem_path = cleanPath(filesystem_path);
        String hostname = InetAddress.getLocalHost().getHostName();
        users.put(clean_local_path, clean_filesystem_path);
        metaData.addStorageServer(hostname, clean_filesystem_path);
        return create(clean_filesystem_path);
    }

    public boolean close(String local_path) throws RemoteException {
        // On close
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        String clean_local_path = cleanPath(local_path);
        String file_system_path = users.get(clean_local_path);
        metaData.delStorageServer(file_system_path);
        users.remove(clean_local_path);
        return del(file_system_path);
    }

    public boolean create(String path) throws RemoteException {
        // Example: create("/courses"); -> Creates a directory
        String clean_path = cleanPath(path);
        metaData.addStorageItem(clean_path);
        return new File(clean_path).mkdirs();
    }

    public boolean create(String path, String blob) throws IOException {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        String clean_path = cleanPath(path);
        metaData.addStorageItem(clean_path);
        Files.write(Paths.get(clean_path), blob.getBytes());
        return true;
    }

    public boolean del(String path) throws RemoteException {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        String clean_path = cleanPath(path);
        metaData.delStorageItem(clean_path);
        return delete(new File(clean_path));
    }

    private boolean delete(File f) {
        if (f.isDirectory())
            for (File c : f.listFiles())
                delete(c);
        return f.delete();
    }

    public File get(String path) {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        String clean_path = cleanPath(path);
        return new File(clean_path);
    }

    private String cleanPath(String path) { return path.replaceAll("^/", "").replaceAll("/$", ""); }
}
