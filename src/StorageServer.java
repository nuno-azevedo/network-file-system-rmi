import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    void create(String path) throws Exception;

    void create(String path, String blob) throws Exception;

    void del(String path) throws Exception;

    File get(String path) throws Exception;
}

public class StorageServer implements StorageInterface {
    private static final Logger Log = Logger.getLogger(StorageServer.class.getName());
    private static MetaDataInterface MetaData;
    private static String LocalPath;

    private StorageServer() {

    }

    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
            System.err.println("USAGE: java StorageServer $HOSTNAME $LOCAL_PATH $FILESYSTEM_PATH");
            System.exit(1);
        }

        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);
            MetaData = (MetaDataInterface) registry.lookup("MetaData");
            init(args[0], args[1], args[2]);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        registry.unbind(args[0]);
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

    // ON STARTUP
    private static void init(String hostname, String local_path, String filesystem_path) throws Exception {
        // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        if (!checkPath(local_path)) {
            Log.log(Level.SEVERE, "invalid local path ‘" + local_path + "’");
            throw new Exception("invalid local path ‘" + local_path + "’");
        }
        try {
            MetaData.addStorageServer(hostname, filesystem_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            throw e;
        }
        LocalPath = local_path;
        File local_dir = new File(local_path + filesystem_path);
        if (!local_dir.exists() && !local_dir.mkdirs()) {
            Log.log(Level.SEVERE, "could not create local path directory ‘" + local_path + "’");
            throw new Exception("could not create local path directory ‘" + local_path + "’");
        }
        if (!local_dir.isDirectory()) {
            Log.log(Level.SEVERE, "local path is not a directory ‘" + local_path + "’");
            throw new Exception("local path is not a directory ‘" + local_path + "’");
        }
        Log.log(Level.INFO, local_path + ", " + filesystem_path);
    }

    // ON CLOSE
    private static void close(String local_path, String filesystem_path) throws Exception {
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        if (!checkPath(local_path)) {
            Log.log(Level.SEVERE, "invalid local path ‘" + local_path + "’");
            throw new Exception("invalid local path ‘" + local_path + "’");
        }
        try {
            MetaData.delStorageServer(filesystem_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            throw e;
        }
        File local_dir = new File(local_path);
        if (!delRecursive(local_dir)) {
            Log.log(Level.SEVERE, "could not delete local directory ‘" + local_path + "’");
            throw new Exception("could not delete local directory ‘" + local_path + "’");
        }
        Log.log(Level.INFO, local_path + ", " + filesystem_path);
    }

    // CALLS FROM CLIENT
    public void create(String path) throws Exception {
        // Example: create("/courses"); -> Creates a directory
        try {
            MetaData.addStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            throw e;
        }
        File dir = new File(LocalPath + path);
        if (!dir.mkdir()) {
            Log.log(Level.SEVERE, "could not create directory ‘" + path + "’");
            throw new Exception("could not create directory ‘" + path + "’");
        }
        Log.log(Level.INFO, path);
    }

    public void create(String path, String blob) throws Exception {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        try {
            MetaData.addStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            throw e;
        }
        File file = new File(LocalPath + path);
        if (!file.createNewFile()) {
            Log.log(Level.SEVERE, "could not create file ‘" + path + "’");
            throw new Exception("could not create file ‘" + path + "’");
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(blob);
            writer.close();
        } catch (Exception e) {
            Log.log(Level.SEVERE, "could not write to file ‘" + path + "’");
            throw new Exception("could not write to file ‘" + path + "’");
        }
        Log.log(Level.INFO, path);
    }

    public void del(String path) throws Exception {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        try {
            MetaData.delStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.toString(), e);
            throw e;
        }
        File target = new File(LocalPath + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + path + "’");
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        if (!delRecursive(target)) {
            Log.log(Level.SEVERE, "could not delete file or directory ‘" + path + "’");
            throw new Exception("could not delete file or directory ‘" + path + "’");
        }
        Log.log(Level.INFO, path);
    }

    private static boolean delRecursive(File f) {
        if (f.isDirectory())
            for (File c : f.listFiles())
                delRecursive(c);
        return f.delete();
    }

    public File get(String path) throws Exception {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        if (!checkPath(path)) {
            Log.log(Level.SEVERE, "invalid path ‘" + path + "’");
            throw new Exception("invalid path ‘" + path + "’");
        }
        File target = new File(LocalPath + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "no such file or directory ‘" + path + "’");
            throw new Exception("no such file or directory ‘" + path + "’");
        }
        if (target.isDirectory()) {
            Log.log(Level.SEVERE, "can not download a directory ‘" + path + "’");
            throw new Exception("can not download a directory ‘" + path + "’");
        }
        return target;
    }

    private static boolean checkPath(String path) {
        String valid_path = "^/([^/]+/?)+$";
        if (path.matches(valid_path)) return true;
        return false;
    }
}
