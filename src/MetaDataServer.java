import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetaDataServer implements MetaDataInterface {
    private static final Logger Log = Logger.getLogger(MetaDataServer.class.getName());
    private static HashMap<String, String> StorageServers;
    private static String Hostname;
    private static FSTree FileSystem;

    private MetaDataServer() {
        this.Hostname = new String();
        this.FileSystem = new FSTree();
        this.StorageServers = new HashMap<String, String>();
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("USAGE: java MetaDataServer $HOSTNAME");
            System.exit(1);
        }

        try {
            MetaDataServer obj = new MetaDataServer();
            MetaDataInterface stub = (MetaDataInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);
            Hostname = args[0];

            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry, obj)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exit(Registry registry, MetaDataServer obj) {
        try {
            if (registry != null) registry.unbind(Hostname);
            UnicastRemoteObject.unexportObject(obj, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CALLS FROM STORAGE SERVER
    public void addStorageServer(String hostname, String top_dir) throws Exception {
        // Example: addStorageServer("machine1.dcc.fc.up.pt", "/courses");
        if (!checkTopPath(top_dir)) {
            Log.log(Level.SEVERE, "cannot add storage server ‘" + hostname + "’: invalid top directory ‘" + top_dir + "’");
            throw new Exception("cannot add storage server ‘" + hostname + "’: invalid top directory ‘" + top_dir + "’");
        }
        if (StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "cannot add storage server ‘" + hostname + "’: top directory ‘" + top_dir + "’ already in use");
            throw new Exception("cannot add storage server ‘" + hostname + "’: top directory ‘" + top_dir + "’ already in use");
        }
        if (!FileSystem.addNode(top_dir, NodeType.Dir)) {
            Log.log(Level.SEVERE, "cannot add storage server ‘" + hostname + "’:  file ‘" + top_dir + "’ exists");
            throw new Exception("cannot add storage server ‘" + hostname + "’:  file ‘" + top_dir + "’ exists");
        }
        StorageServers.put(top_dir, hostname);
        Log.log(Level.INFO, hostname + ", " + top_dir);
    }

    public void delStorageServer(String hostname, String top_dir) throws Exception {
        // Example: delStorageServer("/courses");
        if (!checkTopPath(top_dir)) {
            Log.log(Level.SEVERE, "cannot delete storage server ‘" + hostname + "’: invalid top directory ‘" + top_dir + "’");
            throw new Exception("cannot delete storage server ‘" + hostname + "’: invalid top directory ‘" + top_dir + "’");
        }
        if (!StorageServers.containsKey(top_dir)) {
            Log.log(Level.SEVERE, "cannot delete storage server ‘" + hostname + "’: top directory ‘" + top_dir + "’ not found");
            throw new Exception("cannot delete storage server ‘" + hostname + "’: top directory ‘" + top_dir + "’ not found");
        }
        if (!FileSystem.delNode(top_dir)) {
            Log.log(Level.SEVERE, "cannot delete storage server ‘" + hostname + "’: no such file or directory ‘" + top_dir + "’");
            throw new Exception("cannot delete storage server ‘" + hostname + "’: no such file or directory ‘" + top_dir + "’");
        }
        StorageServers.remove(top_dir);
        Log.log(Level.INFO, hostname + ", " + top_dir);
    }

    public void addStorageItem(String item, NodeType type) throws Exception {
        // Example: addStorageItem("/courses/video1.avi");
        if (!checkAbsPath(item)) {
            Log.log(Level.SEVERE, "cannot add item ‘" + item + "’: invalid path");
            throw new Exception("cannot add item ‘" + item + "’: invalid path");
        }
        if (checkTopPath(item)) {
            Log.log(Level.SEVERE, "cannot add item ‘" + item + "’: not allowed on root directory");
            throw new Exception("cannot add item ‘" + item + "’: not allowed on root directory");
        }
        if (!FileSystem.addNode(item, type)) {
            if (!checkExists(item)) {
                Log.log(Level.SEVERE, "cannot add item ‘" + item + "’: no such file or directory");
                throw new Exception("cannot add item ‘" + item + "’: no such file or directory");
            }
            if (isDir(item) || (type == NodeType.Dir && isFile(item))) {
                Log.log(Level.SEVERE, "cannot add item ‘" + item + "’: file exists");
                throw new Exception("cannot add item ‘" + item + "’: file exists");
            }
        }
        Log.log(Level.INFO, item);
    }

    public void delStorageItem(String item) throws Exception {
        // Example: delStorageItem("/courses/video1.avi");
        if (!checkAbsPath(item)) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + item + "’: invalid path");
            throw new Exception("cannot delete item ‘" + item + "’: invalid path");
        }
        if (checkTopPath(item)) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + item + "’: not allowed on root directory");
            throw new Exception("cannot delete item ‘" + item + "’: not allowed on root directory");
        }
        if (!FileSystem.delNode(item)) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + item + "’: no such file or directory");
            throw new Exception("cannot delete item ‘" + item + "’: no such file or directory");
        }
        Log.log(Level.INFO, item);
    }

    // CALLS FROM CLIENT
    public String find(String item) throws Exception {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        if (item.equals("/")) {
            return Hostname;
        }
        if (!checkAbsPath(item)) {
            Log.log(Level.SEVERE, "cannot find item ‘" + item + "’: invalid path");
            throw new Exception("cannot find item ‘" + item + "’: invalid path");
        }
        if (!checkExists(item)) {
            Log.log(Level.SEVERE, "cannot find item ‘" + item + "’: no such file or directory");
            throw new Exception("cannot find item ‘" + item + "’: no such file or directory");
        }
        String top_dir = getTopPath(item);
        String storage = StorageServers.get(top_dir);
        if (storage == null) {
            Log.log(Level.SEVERE, "cannot find item ‘" + item + "’: storage server not found");
            throw new Exception("cannot find item ‘" + item + "’: storage server not found");
        }
        Log.log(Level.INFO, storage + ", " + item);
        return storage;
    }

    public Stat lstat(String item) throws Exception {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        if (item.equals("/")) {
            return new Stat(Hostname, new ArrayList<String>(StorageServers.keySet()));
        }
        if (!checkAbsPath(item)) {
            Log.log(Level.SEVERE, "cannot access item ‘" + item + "’: invalid path");
            throw new Exception("cannot access item ‘" + item + "’: invalid path");
        }
        if (!checkExists(item)) {
            Log.log(Level.SEVERE, "cannot access item ‘" + item + "’: no such file or directory");
            throw new Exception("cannot access item ‘" + item + "’: no such file or directory");
        }
        String top_dir = getTopPath(item);
        String storage = StorageServers.get(top_dir);
        if (storage == null) {
            Log.log(Level.SEVERE, "cannot access item ‘" + item + "’: storage server not found");
            throw new Exception("cannot access item ‘" + item + "’: storage server not found");
        }
        FSNode target = FileSystem.getNode(item);
        Log.log(Level.INFO, item);
        if (target.isDir()) return new Stat(storage, target.getChildsNames());
        return new Stat(storage, new ArrayList<String>(Arrays.asList(target.getName())));
    }

    public boolean isDir(String item) throws Exception {
        FSNode target = FileSystem.getNode(item);
        return target != null && target.isDir();
    }

    public boolean isFile(String item) throws Exception {
        FSNode target = FileSystem.getNode(item);
        return target != null && target.isFile();
    }

    public boolean checkExists(String item) throws Exception {
        FSNode target = FileSystem.getNode(item);
        return target != null;
    }

    private boolean checkAbsPath(String path) {
        String valid_abs_path = "^/([^/\0]+/?)+$";
        return path.matches(valid_abs_path);
    }

    private boolean checkTopPath(String path) {
        String valid_top_path = "^/([^/\0]+/?){0,1}$";
        return path.matches(valid_top_path);
    }

    private String getTopPath(String path) {
        return path.split("(?=/)")[0];
    }
}
