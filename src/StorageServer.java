import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class StorageServer implements Hello {

    private StorageServer() {
        // Class constructor
    }

    public static void main(String args[]) {
        try {
            StorageServer obj = new StorageServer();
            Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    // CALLS FROM CLIENT
    public boolean init(String local_path, String filesystem_path) {  // On startup
        // Example: init("/home/student/courses", "/courses"); -> Local dir maps into global namespace
        //                                                        Must call add_storage_server on the metadata server
        return false;
    }

    public boolean close(String local_path) {  // On close
        // Example: close("/home/student/courses"); -> Closes local share
        //                                             Must call del_storage_server on the metadata server
        return false;
    }

    public boolean create(String path) {
        // Example: create("/courses"); -> Creates a directory
        return false;
    }

    public boolean create(String path, String blob) {
        // Example: create("/courses/file1.txt", ”A line in a text”); -> Creates a file
        return false;
    }

    public boolean del(String path) {
        // Example: del("/courses"); -> Removes sub-tree
        // Example: del("/courses/file1.txt"); -> Removes file
        return false;
    }

    public boolean get(String path) {
        // Example: get("/courses/file1.txt"); -> Downloads the file
        return false;
    }

    public String sayHello() {
        return "Hello, world!";
    }
}
