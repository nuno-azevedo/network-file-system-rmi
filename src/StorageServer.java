import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageServer implements StorageInterface {
    private static final Logger Log = Logger.getLogger(StorageServer.class.getName());
    private static MetaDataInterface MetaData;
    private static String LocalPath;

    private StorageServer() {
        this.MetaData = null;
        this.LocalPath = new String();
    }

    public static void main(String args[]) {
        if (args.length != 4) {
            System.err.println("USAGE: java StorageServer $METADATA_HOSTNAME $HOSTNAME $LOCAL_PATH $FILESYSTEM_PATH");
            System.exit(1);
        }

        Registry registry = null;
        try {
            StorageServer obj = new StorageServer();
            StorageInterface stub = (StorageInterface) UnicastRemoteObject.exportObject(obj, 0);
            registry = LocateRegistry.getRegistry();
            registry.bind(args[1], stub);
            MetaData = (MetaDataInterface) registry.lookup(args[0]);
            init(args[1], args[2], args[3]);

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while(br.readLine() != null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (MetaData != null) close(args[1], args[2], args[3]);
                if (registry != null) registry.unbind(args[1]);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (MetaData == null || registry == null) System.exit(1);
            System.exit(0);
        }
    }

    // ON STARTUP
    private static void init(String hostname, String local_path, String filesystem_path) throws Exception {
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        if (!checkAbsPath(local_path)) {
            throw new Exception("cannot init storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
        }
        try {
            MetaData.addStorageServer(hostname, filesystem_path);
        } catch (Exception e) {
            throw e;
        }
        LocalPath = local_path;
        File local_dir = new File(local_path + filesystem_path);
        if (!local_dir.exists() && !local_dir.mkdirs()) {
            throw new Exception("cannot init storage server ‘" + hostname + "’: failed to create local directory ‘" + local_path + "’");
        }
        if (!local_dir.isDirectory()) {
            throw new Exception("cannot init storage server ‘" + hostname + "’: not a directory ‘" + local_path + "’");
        }
        addRecursive(local_dir);
        Log.log(Level.INFO, hostname + ", " + local_path + ", " + filesystem_path);
    }

    // ON CLOSE
    private static void close(String hostname, String local_path, String filesystem_path) throws Exception {
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        if (!checkAbsPath(local_path)) {
            throw new Exception("cannot close storage server ‘" + hostname + "’: invalid local directory ‘" + local_path + "’");
        }
        try {
            MetaData.delStorageServer(hostname, filesystem_path);
        } catch (Exception e) {
            throw e;
        }
        Log.log(Level.INFO, hostname + ", " + local_path + ", " + filesystem_path);
    }

    // CALLS FROM CLIENT
    public void create(String item) throws Exception {
        // Example: create("/courses"); -> Creates a directory
        try {
            MetaData.addStorageItem(item, NodeType.Dir);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage());
            throw e;
        }
        File dir = new File(LocalPath + item);
        if (dir.exists()) {
            try { MetaData.delStorageItem(item); } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create directory ‘" + item + "’: file exists");
            throw new Exception("cannot create directory ‘" + item + "’: file exists");
        }
        if (!dir.mkdir()) {
            try { MetaData.delStorageItem(item); } catch (Exception e) { }
            Log.log(Level.SEVERE, "cannot create directory ‘" + item + "’: failed to create");
            throw new Exception("cannot create directory ‘" + item + "’: failed to create");
        }
        Log.log(Level.INFO, item);
    }

    public void create(String item, byte bytes[]) throws Exception {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        try {
            MetaData.addStorageItem(item, NodeType.File);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage());
            throw e;
        }
        File file = new File(LocalPath + item);
        if (!file.exists() && !file.createNewFile()) {
            Log.log(Level.SEVERE, "cannot create file ‘" + item + "’: failed to create");
            throw new Exception("cannot create file ‘" + item + "’: failed to create");
        }
        if (file.isDirectory()) {
            Log.log(Level.SEVERE, "cannot create file ‘" + item + "’: not a file");
            throw new Exception("cannot create file ‘" + item + "’: not a file");
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "cannot write to file ‘" + item + "’: failed to write");
            throw new Exception("cannot write to file ‘" + item + "’: failed to write");
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception e) { }
            }
        }
        Log.log(Level.INFO, item);
    }

    public void del(String item) throws Exception {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        try {
            MetaData.delStorageItem(item);
        } catch (Exception e) {
            Log.log(Level.SEVERE, e.getMessage());
            throw e;
        }
        File target = new File(LocalPath + item);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + item + "’: no such file or directory");
            throw new Exception("cannot delete item ‘" + item + "’: no such file or directory");
        }
        if (!delRecursive(target)) {
            Log.log(Level.SEVERE, "cannot delete item ‘" + item + "’: failed to remove");
            throw new Exception("cannot delete item ‘" + item + "’: failed to remove");
        }
        Log.log(Level.INFO, item);
    }

    public byte[] get(String item) throws Exception {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        if (!checkAbsPath(item)) {
            Log.log(Level.SEVERE, "cannot get item ‘" + item + "’: invalid path");
            throw new Exception("cannot get item ‘" + item + "’: invalid path");
        }
        File target = new File(LocalPath + item);
        if (!target.exists()) {
            Log.log(Level.SEVERE, "cannot get item ‘" + item + "’: no such file or directory");
            throw new Exception("cannot get item ‘" + item + "’: no such file or directory");
        }
        if (target.isDirectory()) {
            Log.log(Level.SEVERE, "cannot get item ‘" + item + "’: not a file");
            throw new Exception("cannot get item ‘" + item + "’: not a file");
        }
        byte bytes[] = new byte[(int) target.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(target);
            fis.read(bytes);
        }catch(Exception e) {
            Log.log(Level.SEVERE, "cannot get item ‘" + item + "’: failed to read");
            throw new Exception("cannot get item ‘" + item + "’: failed to read");
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { }
            }
        }
        return bytes;
    }

    private static void addRecursive(File file) {
        try {
            String path = file.getPath().replace(LocalPath, "");
            if (checkTopPath(path));
            else if (file.isFile()) MetaData.addStorageItem(path, NodeType.File);
            else if (file.isDirectory()) MetaData.addStorageItem(path, NodeType.Dir);
        } catch (Exception e) { }
        if (file.isDirectory())
            for (File c : file.listFiles())
                addRecursive(c);
    }

    private static boolean delRecursive(File file) {
        System.out.println(file.getPath());
        if (file.isDirectory())
            for (File c : file.listFiles())
                delRecursive(c);
        return file.delete();
    }

    private static boolean checkTopPath(String path) {
        String valid_top_dir = "^/((?!/\\.{2,}(/|$)|//|/).)*$";
        if (path.matches(valid_top_dir)) return true;
        return false;
    }

    private static boolean checkAbsPath(String path) {
        String valid_abs_path = "^/((?!/\\.{2,}(/|$)|//).)*(?<!/)$";
        if (path.matches(valid_abs_path)) return true;
        return false;
    }
}
