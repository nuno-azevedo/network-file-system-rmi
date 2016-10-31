import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

interface MetaDataInterface extends Remote {
    // CALLS FROM STORAGE SERVER
    boolean addStorageServer(String host, String top_dir) throws RemoteException;

    boolean delStorageServer(String top_dir) throws RemoteException;

    boolean addStorageItem(String item) throws RemoteException;

    boolean delStorageItem(String item) throws RemoteException;

    // CALLS FROM CLIENT
    String find(String path) throws RemoteException;

    Stat lstat(String path) throws RemoteException;
}

public class MetaDataServer implements MetaDataInterface {
    private static FSTree fileSystem;
    private static HashMap<String, String> storageServers;

    private MetaDataServer() {
        this.fileSystem = new FSTree();
        this.storageServers = new HashMap<String, String>();
    }

    public static void main(String args[]) {
        try {
            MetaDataServer obj = new MetaDataServer();
            MetaDataInterface stub = (MetaDataInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("MetaDataServer", stub);
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    // CALLS FROM STORAGE SERVER
    public boolean addStorageServer(String host, String top_dir) {
        // Example: addStorageServer("machine1.dcc.fc.up.pt", "/courses");
        storageServers.put(top_dir, host);
        fileSystem.addNode(top_dir);
        return true;
    }

    public boolean delStorageServer(String top_dir) {
        // Example: delStorageServer("/courses");
        storageServers.remove(top_dir);
        fileSystem.delNode(top_dir);
        return true;
    }

    public boolean addStorageItem(String item) {
        // Example: addStorageItem("/courses/video1.avi");
        fileSystem.addNode(item);
        return true;
    }

    public boolean delStorageItem(String item) {
        // Example: delStorageItem("/courses/video1.avi");
        fileSystem.delNode(item);
        return true;
    }

    // CALLS FROM CLIENT
    public String find(String path) {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        String top_dir = splitPath(path)[0];
        return storageServers.get(top_dir);
    }

    public Stat lstat(String path) {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        Stat stat = new Stat();
        stat.server = find(path);
        for (FSNode child : fileSystem.getNode(path).getChilds())
            stat.items.add(child.getName());
        return stat;
    }

    private String[] splitPath(String path) {
        return path.replaceAll("^/", "").replaceAll("/$", "").split("/");
    }
}
