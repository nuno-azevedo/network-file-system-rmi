import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    void init(String local_path, String filesystem_path) throws Exception;

    void close(String local_path) throws Exception;

    void create(String path) throws Exception;

    void create(String path, String blob) throws Exception;

    void del(String path) throws Exception;

    File get(String path) throws Exception;
}

public class StorageServer implements StorageInterface {
    private static final Logger Log = Logger.getLogger(StorageServer.class.getName());
    private static String hostname;

    private static MetaDataInterface MetaData;
    private HashMap<String, String> Users;

    private StorageServer() {
        this.Users = new HashMap<String, String>();
    }

    public static void main(String args[]) throws Exception {
        try {
            hostname = args[0];
        } catch (Exception e) {
            Log.log(Level.SEVERE, "Missing hostname argument");
            return;
        }

        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(hostname, stub);

            MetaData = (MetaDataInterface) registry.lookup("MetaData");
        } catch (Exception e) {
            Log.log(Level.SEVERE, "main: ‘" + e.toString() + "’", e);
            e.printStackTrace();
        }
    }

    // CALLS FROM CLIENT
    public void init(String local_path, String filesystem_path) throws Exception {
        // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        if (!checkPath(local_path)) {
            Log.log(Level.SEVERE, "init: invalid local path", local_path);
            throw new Exception("invalid local path ‘" + local_path + "’");
        }
        if (!checkTopDir(filesystem_path)) {
            Log.log(Level.SEVERE, "init: invalid file system path", filesystem_path);
            throw new Exception("invalid file system path ‘" + filesystem_path + "’");
        }

        try {
            MetaData.addStorageServer(hostname, filesystem_path);
            Users.put(local_path, filesystem_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "init: ‘" + e.toString() + "’", e);
            throw e;
        }

        File top_dir = new File("." + filesystem_path);
        if (!top_dir.mkdir()) {
            Log.log(Level.SEVERE, "init: could not create directory", filesystem_path);
            throw new Exception("could not create directory ‘" + filesystem_path + "’");
        }
        Log.log(Level.INFO, "init: ‘" + filesystem_path + "’", local_path);
    }

    public void close(String local_path) throws Exception {
        // On close
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        if (!checkPath(local_path)) {
            Log.log(Level.SEVERE, "close: invalid local path", local_path);
            throw new Exception("invalid local path ‘" + local_path + "’");
        }

        String filesystem_path = Users.get(local_path);
        if (filesystem_path == null) {
            Log.log(Level.SEVERE, "close: local path not found", local_path);
            throw new Exception("local path ‘" + local_path + "’ not found");
        }

        try {
            MetaData.delStorageServer(filesystem_path);
            Users.remove(local_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "close: ‘" + e.toString() + "’", e);
            throw e;
        }

        File top_dir = new File("." + filesystem_path);
        if (!delRecursive(top_dir)) {
            Log.log(Level.SEVERE, "close: could not delete directory", filesystem_path);
            throw new Exception("could not delete directory ‘" + filesystem_path + "’");
        }
        Log.log(Level.INFO, "close: ‘" + filesystem_path + "’", local_path);
    }

    public void create(String path) throws Exception {
        // Example: create("/courses"); -> Creates a directory
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "create: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        try {
            MetaData.addStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "create: ‘" + e.toString() + "’", e);
            throw e;
        }

        File dir = new File("." + path);
        if (!dir.mkdir()) {
            Log.log(Level.SEVERE, "create: could not create directory", path);
            throw new Exception("could not create directory ‘" + path + "’");
        }
        Log.log(Level.INFO, "create: ‘" + path + "’");
    }

    public void create(String path, String blob) throws Exception {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "create: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        try {
            MetaData.addStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "create: ‘" + e.toString() + "’", e);
            throw e;
        }

        File file = new File("." + path);
        if (!file.createNewFile()) {
            Log.log(Level.SEVERE, "create: could not create file", path);
            throw new Exception("could not create file ‘" + path + "’");
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(blob);
            writer.close();
        } catch (Exception e) {
            Log.log(Level.SEVERE, "create: could not write to file", path);
            throw new Exception("could not write to file ‘" + path + "’");
        }
        Log.log(Level.INFO, "create: ‘" + path + "’");
    }

    public void del(String path) throws Exception {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "del: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        try {
            MetaData.delStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "del: ‘" + e.toString() + "’", e);
            throw e;
        }
        File target = new File("." + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "del: no such file or directory", path);
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        if (!delRecursive(target)) {
            Log.log(Level.SEVERE, "del: could not delete file or directory", path);
            throw new Exception("could not delete file or directory ‘" + path + "’");
        }
        Log.log(Level.INFO, "del: ‘" + path + "’");
    }

    private boolean delRecursive(File f) {
        if (f.isDirectory())
            for (File c : f.listFiles())
                delRecursive(c);
        return f.delete();
    }

    public File get(String path) throws Exception {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "get: invalid path", path);
            throw new Exception("invalid path ‘" + path + "’");
        }

        File target = new File("." + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "get: no such file or directory", path);
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        return target;
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
}
