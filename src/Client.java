import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {
        // Class constructor
    }

    public static void main(String args[]) {
        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            StorageInterface stub = (StorageInterface) registry.lookup("StorageServer");
            System.out.println(stub.init("lol", "/mydir1"));
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    private boolean pwd() {
        // Prints the current directory
        return false;
    }

    private boolean ls() {
        // Lists the current directory
        return false;
    }

    private boolean cd(String dir) {
        // Changes the current directory to dir
        // dir can be a simple name or absolute or relative path
        return false;
    }

    private boolean mv(String file1, String file2) {
        // Copies file file1 to file2, overwriting if the latter exists
        // file can be a simple name or absolute or relative path
        return false;
    }

    private boolean open(String file) {
        // Opens the file with the proper application, accordingly to its extension
        // file can be a simple name or absolute or relative path
        return false;
    }
}
