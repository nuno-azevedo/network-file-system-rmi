import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

interface MetaDataInterface extends Remote {
    // CALLS FROM STORAGE SERVER
    void addStorageServer(String hostname, String top_dir) throws Exception;

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
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        registry.unbind("MetaData");
                    } catch (Exception e) {
                        Log.log(Level.SEVERE, e.toString(), e);
                        System.exit(1);
                    }
                }
            });
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }

    }

    // CALLS FROM STORAGE SERVER
    public void addStorageServer(String hostname, String top_dir) throws Exception {
        // Example: addStorageServer("machine1.dcc.fc.up.pt", "/courses");
        if (!checkTopDir(top_dir)) {
            Log.log(Level.SEVERE, "invalid top directory ‘" + top_dir + "’");
            throw new Exception("invalid top directory ‘" + top_dir + "’");
        }
        if (StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "top directory already in use ‘" + top_dir + "’");
            throw new Exception("top directory already in use ‘" + top_dir + "’");
        }
        if (!FileSystem.addNode(top_dir)) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + top_dir + "’");
            throw new Exception("no such file or directory ‘" + top_dir + "’");
        }
        StorageServers.put(top_dir, hostname);
        Log.log(Level.INFO, hostname + ", " + top_dir);
    }

    public void delStorageServer(String top_dir) throws Exception {
        // Example: delStorageServer("/courses");
        if (!checkTopDir(top_dir)) {
            Log.log(Level.SEVERE, "invalid top directory ‘" + top_dir + "’");
            throw new Exception("invalid top directory ‘" + top_dir + "’");
        }
        if (!StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "top directory not found ‘" + top_dir + "’");
            throw new Exception("top directory not found ‘" + top_dir + "’");
        }
        if (!FileSystem.delNode(top_dir)) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + top_dir + "’");
            throw new Exception("no such file or directory ‘" + top_dir + "’");
        }
        StorageServers.remove(top_dir);
        Log.log(Level.INFO, top_dir);
    }

    public void addStorageItem(String item) throws Exception {
        // Example: addStorageItem("/courses/video1.avi");
        if (checkTopDir(item)) {
            Log.log(Level.SEVERE, "can not add item in root directory ‘" + item + "’");
            throw new Exception("can not add item in root directory ‘" + item + "’");
        }
        if (!checkPath(item)) {
            Log.log(Level.SEVERE, "invalid path ‘" + item + "’");
            throw new Exception("invalid path ‘" + item + "’");
        }
        if (!FileSystem.addNode(item)) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + item + "’");
            throw new Exception("no such file or directory ‘" + item + "’");
        }
        Log.log(Level.INFO, item);
    }

    public void delStorageItem(String item) throws Exception {
        // Example: delStorageItem("/courses/video1.avi");
        if (checkTopDir(item)) {
            Log.log(Level.SEVERE, "can not delete item in root directory ‘" + item + "’");
            throw new Exception("can not delete item in root directory ‘" + item + "’");
        }
        if (!checkPath(item)) {
            Log.log(Level.SEVERE, "invalid path ‘" + item + "’");
            throw new Exception("invalid path ‘" + item + "’");
        }
        if (!FileSystem.delNode(item)) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + item + "’");
            throw new Exception("no such file or directory ‘" + item + "’");
        }
        Log.log(Level.INFO, item);
    }

    // CALLS FROM CLIENT
    public String find(String path) throws Exception {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "invalid path ‘" + path + "’");
            throw new Exception("invalid path ‘" + path + "’");
        }
        if (FileSystem.getNode(path) == null) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + path + "’");
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        String top_dir = "/" + splitPath(path)[0];
        String storage = StorageServers.get(top_dir);
        if (storage == null) {
            Log.log(Level.SEVERE, "storage server not found ‘" + path + "’");
            throw new Exception("storage server not found ‘" + path + "’");
        }
        Log.log(Level.INFO, path, storage);
        return storage;
    }

    public Stat lstat(String path) throws Exception {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        if (path.equals("/")) {
            List<String> storage_hosts = new ArrayList<String>();
            storage_hosts.addAll(StorageServers.keySet());
            return new Stat("MetaData", storage_hosts);
        }
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "invalid path ‘" + path + "’");
            throw new Exception("invalid path ‘" + path + "’");
        }
        FSNode target = FileSystem.getNode(path);
        if (target == null) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + path + "’");
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        String top_dir = "/" + splitPath(path)[0];
        String storage = StorageServers.get(top_dir);
        if (storage == null) {
            Log.log(Level.SEVERE, "storage server not found ‘" + path + "’");
            throw new Exception("storage server not found ‘" + path + "’");
        }
        Log.log(Level.INFO, path);
        return new Stat(storage, target.getChildsNames());
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
