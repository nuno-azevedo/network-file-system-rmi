import java.util.Arrays;
import java.util.HashMap;

public class MetaDataServer {
    private static FSTree fileSystem = new FSTree();
    private static HashMap<String, String> storageServers = new HashMap<String, String>();

    private MetaDataServer() {
        // Class constructor
    }

    public static void main(String args[]) {

    }

    // CALLS FROM STORAGE SERVER
    public static void addStorageServer(String host, String top_dir) {
        // Example: addStorageServer("machine1.dcc.fc.up.pt", "/courses");
        storageServers.put(top_dir, host);
        fileSystem.addElement(top_dir);
    }

    public static void delStorageServer(String top_dir) {
        // Example: delStorageServer("/courses");
        storageServers.remove(top_dir);
        fileSystem.delElement(top_dir);
    }

    public static void addStorageItem(String item) {
        // Example: addStorageItem("/courses/video1.avi");
        fileSystem.addElement(item);
    }

    public static void delStorageItem(String item) {
        // Example: delStorageItem("/courses/video1.avi");
        fileSystem.delElement(item);
    }

    // CALLS FROM CLIENT
    public String find(String path) {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        String top_dir = splitPath(path)[0];
        return storageServers.get(top_dir);
    }

    public String lstat(String path) {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        return "";
    }

    private static String[] splitPath(String path) {
        String list[] = path.split("/");
        if (list[0].equals("")) list = Arrays.copyOfRange(list, 1, list.length);
        if (list[list.length - 1].equals("")) list = Arrays.copyOfRange(list, 0, list.length - 1);
        return list;
    }
}
