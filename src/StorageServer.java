import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageServer implements StorageInterface {
    private static final Logger Log = Logger.getLogger(StorageServer.class.getName());
    private static MetaDataInterface MetaData;
    private static String LocalPath;

    private StorageServer() {

    }

    public static void main(String args[]) throws Exception {
        if (args.length != 4) {
            System.err.println("USAGE: java StorageServer $HOSTNAME $LOCAL_PATH $FILESYSTEM_PATH $METADATA_HOSTNAME");
            System.exit(1);
        }

        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);
            MetaData = (MetaDataInterface) registry.lookup(args[3]);
            init(args[0], args[1], args[2]);

            Scanner scan = new Scanner(System.in);
            while (!scan.nextLine().equals("exit"));

            close(args[0], args[1], args[2]);
            registry.unbind(args[0]);
            System.exit(0);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }
    }

    // ON STARTUP
    private static void init(String hostname, String local_path, String filesystem_path) throws Exception {
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        if (!checkAbsPath(local_path)) {
            Log.log(Level.SEVERE, "cannot init storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
            throw new Exception("cannot init storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
        }
        try {
            MetaData.addStorageServer(hostname, filesystem_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        LocalPath = local_path;
        File local_dir = new File(local_path + filesystem_path);
        if (!local_dir.exists() && !local_dir.mkdirs()) {
            try {
                MetaData.delStorageServer(hostname, filesystem_path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot init storage server ‘" + hostname + "’: failed to create local directory ‘" + local_path + "’");
            throw new Exception("cannot init storage server ‘" + hostname + "’: failed to create local directory ‘" + local_path + "’");
        }
        if (!local_dir.isDirectory()) {
            try {
                MetaData.delStorageServer(hostname, filesystem_path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot init storage server ‘" + hostname + "’: not a directory ‘" + local_path + "’");
            throw new Exception("cannot init storage server ‘" + hostname + "’: not a directory ‘" + local_path + "’");
        }
        addRecursive(local_dir);
        Log.log(Level.INFO, local_path + ", " + filesystem_path);
    }

    // ON CLOSE
    private static void close(String hostname, String local_path, String filesystem_path) throws Exception {
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        if (!checkAbsPath(local_path)) {
            Log.log(Level.SEVERE, "cannot close storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
            throw new Exception("cannot close storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
        }
        try {
            MetaData.delStorageServer(hostname, filesystem_path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        Log.log(Level.INFO, local_path + ", " + filesystem_path);
    }

    // CALLS FROM CLIENT
    public void create(String path) throws Exception {
        // Example: create("/courses"); -> Creates a directory
        try {
            MetaData.addStorageItem(path, NodeType.Dir);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        File dir = new File(LocalPath + path);
        if (dir.exists()) {
            try {
                MetaData.delStorageItem(path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create directory ‘" + path + "’: file exists");
            throw new Exception("cannot create directory ‘" + path + "’: file exists");
        }
        if (!dir.mkdir()) {
            try {
                MetaData.delStorageItem(path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create directory ‘" + path + "’: failed to create");
            throw new Exception("cannot create directory ‘" + path + "’: failed to create");
        }
        Log.log(Level.INFO, path);
    }

    public void create(String path, String blob) throws Exception {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        try {
            MetaData.addStorageItem(path, NodeType.File);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        File file = new File(LocalPath + path);
        if (!file.exists() && !file.createNewFile()) {
            try {
                MetaData.delStorageItem(path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create file ‘" + path + "’: failed to create");
            throw new Exception("cannot create file ‘" + path + "’: failed to create");
        }
        if (file.isDirectory()) {
            try {
                MetaData.delStorageItem(path);
            } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create file ‘" + path + "’: not a file");
            throw new Exception("cannot create file ‘" + path + "’: not a file");
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(blob);
            writer.close();
        } catch (Exception e) {
            Log.log(Level.SEVERE, "cannot write to file ‘" + path + "’: failed to write");
            throw new Exception("cannot write to file ‘" + path + "’: failed to write");
        }
        Log.log(Level.INFO, path);
    }

    public void del(String path) throws Exception {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        try {
            MetaData.delStorageItem(path);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        File target = new File(LocalPath + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + path + "’: no such file or directory");
            throw new Exception("cannot delete item ‘" + path + "’: no such file or directory");
        }
        if (!delRecursive(target)) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + path + "’: failed to remove");
            throw new Exception("cannot delete item ‘" + path + "’: failed to remove");
        }
        Log.log(Level.INFO, path);
    }

    public File get(String path) throws Exception {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        if (!checkAbsPath(path)) {
            Log.log(Level.SEVERE, "cannot get item ‘" + path + "’: invalid path");
            throw new Exception("cannot get item ‘" + path + "’: invalid path");
        }
        File target = new File(LocalPath + path);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "cannot get item ‘" + path + "’: no such file or directory");
            throw new Exception("cannot get item ‘" + path + "’: no such file or directory");
        }
        if (target.isDirectory()) {
            Log.log(Level.SEVERE, "cannot get item ‘" + path + "’: not a file");
            throw new Exception("cannot get item ‘" + path + "’: not a file");
        }
        return target;
    }

    private static void addRecursive(File f) {
        try {
            String path = f.getPath().replace(LocalPath, "");
            if (checkTopPath(path));
            else if (f.isDirectory()) MetaData.addStorageItem(path, NodeType.Dir);
            else MetaData.addStorageItem(path, NodeType.File);
        } catch (Exception e) { }
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                addRecursive(c);
            }
        }
    }

    private static boolean delRecursive(File f) {
        System.out.println(f.getPath());
        if (f.isDirectory())
            for (File c : f.listFiles())
                delRecursive(c);
        return f.delete();
    }

    private static boolean checkAbsPath(String path) {
        String valid_path = "^/((?!/\\.{2,}(/|$)|//).)*(?<!/)$";
        if (path.matches(valid_path)) return true;
        return false;
    }

    private static boolean checkTopPath(String top_dir) {
        String valid_top_dir = "^/((?!/\\.{2,}(/|$)|//|/).)*$";
        if (top_dir.matches(valid_top_dir)) return true;
        return false;
    }
}
