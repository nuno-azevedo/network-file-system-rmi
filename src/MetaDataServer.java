import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

interface MetaDataInterface extends Remote {
    // CALLS FROM STORAGE SERVER
    void addStorageServer(String host, String top_dir) throws Exception;

    void delStorageServer(String top_dir) throws Exception;

    void addStorageItem(String item) throws Exception;

    void delStorageItem(String item) throws Exception;

    // CALLS FROM CLIENT
    String find(String path) throws Exception;

    Stat lstat(String path) throws Exception;
}

public class MetaDataServer implements MetaDataInterface {
    private static final Logger Log = Logger.getLogger(MetaDataServer.class.getName());

    private static FSTree FileSystem;
    private static HashMap<String, String> StorageServers;

    private MetaDataServer() {
        this.FileSystem = new FSTree();
        this.StorageServers = new HashMap<String, String>();
    }

    public static void main(String args[]) {
        try {
            MetaDataServer obj = new MetaDataServer();
            MetaDataInterface stub = (MetaDataInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("MetaData", stub);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "main: ‘" + e.toString() + "’", e);
            e.printStackTrace();
        }
    }

    // CALLS FROM STORAGE SERVER
    public void addStorageServer(String host, String top_dir) throws Exception {
        // Example: addStorageServer("machine1.dcc.fc.up.pt", "/courses");
        if (!checkTopDir(top_dir)) {
            Log.log(Level.SEVERE, "addStorageServer: invalid top directory", top_dir);
            throw new Exception("invalid top directory ‘" + top_dir + "’");
        }
        if (StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "addStorageServer: top directory already in use", top_dir);
            throw new Exception("top directory ‘" + top_dir + "’already in use");
        }

        try {
            FileSystem.addNode(top_dir);
            StorageServers.put(top_dir, host);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "addStorageServer: ‘" + e.toString() + "’", e);
            throw e;
        }
        Log.log(Level.INFO, "addStorageServer: ‘" + host + "’", top_dir);
    }

    public void delStorageServer(String top_dir) throws Exception {
        // Example: delStorageServer("/courses");
        if (!checkTopDir(top_dir)) {
            Log.log(Level.SEVERE, "delStorageServer: invalid top directory", top_dir);
            throw new Exception("invalid top directory ‘" + top_dir + "’");
        }
        if (!StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "delStorageServer: top directory not found", top_dir);
            throw new Exception("top directory ‘" + top_dir + "’not found");
        }

        try {
            FileSystem.delNode(top_dir);
            StorageServers.remove(top_dir);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "delStorageServer: ‘" + e.toString() + "’", e);
            throw e;
        }
        Log.log(Level.INFO, "delStorageServer: ‘" + top_dir + "’");
    }

    public void addStorageItem(String item) throws Exception {
        // Example: addStorageItem("/courses/video1.avi");
        if (!checkPath(item)) {
            Log.log(Level.SEVERE, "addStorageItem: invalid path", item);
            throw new Exception("invalid path ‘" + item + "’");
        }

        try {
            FileSystem.addNode(item);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "addStorageItem: ‘" + e.toString() + "’", e);
            throw e;
        }
        Log.log(Level.INFO, "addStorageItem: ‘" + item + "’");
    }

    public void delStorageItem(String item) throws Exception {
        // Example: delStorageItem("/courses/video1.avi");
        if (!checkPath(item)) {
            Log.log(Level.SEVERE, "delStorageItem: invalid path", item);
            throw new Exception("invalid path ‘" + item + "’");
        }

        try {
            FileSystem.delNode(item);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "delStorageItem: ‘" + e.toString() + "’", e);
            throw e;
        }
        Log.log(Level.INFO, "delStorageItem: ‘" + item + "’");
    }

    // CALLS FROM CLIENT
    public String find(String path) throws Exception {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "find: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        String top_dir = splitPath(path)[0];
        String host = StorageServers.get(top_dir);
        if (host == null) {
            Log.log(Level.SEVERE, "find: top directory not found", top_dir);
            throw new Exception("top directory ‘" + top_dir + "’ not found");
        }
        Log.log(Level.INFO, "find: ‘" + path + "’", host);
        return host;
    }

    public Stat lstat(String path) throws Exception {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "lstat: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        Stat stat = new Stat();
        try {
            FSNode target = FileSystem.getNode(path);
            stat.server = find(path);
            stat.items = target.getChildsNames();
        } catch (Exception e) {
            Log.log(Level.SEVERE, "lstat: ‘" + e.toString() + "’", e);
            throw e;
        }
        Log.log(Level.INFO, "lstat: ‘" + path + "’");
        return stat;
    }

    private boolean checkTopDir(String top_dir) {
        String valid_top_dir = "^/[^/]*/?$";
        if (top_dir.matches(valid_top_dir)) return true;
        return false;
    }

    private boolean checkPath(String path) {
        String valid_path = "^/([^/]+/?)+$";
        if (path.matches(valid_path)) return true;
        return false;
    }

    private String[] splitPath(String path) {
        return path.replaceAll("^/", "").replaceAll("/$", "").split("/");
    }
}
