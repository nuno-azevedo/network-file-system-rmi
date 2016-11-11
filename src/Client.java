import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Scanner;

public class Client {
    private static Registry registry;
    private static MetaDataInterface MetaData;
    private static HashMap<String, StorageInterface> StorageServers;
    private static String LocalPath;
    private static String CurrentDir;

    private Client() {
        CurrentDir = "/";
    }

    public static void main(String args[]) throws RemoteException {
        if (args.length != 1) {
            System.err.println("USAGE: java Client $LOCAL_PATH");
            System.exit(1);
        }

        try {
            registry = LocateRegistry.getRegistry();
            MetaData = (MetaDataInterface) registry.lookup("MetaData");
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }

        LocalPath = args[0];
        Scanner scan = new Scanner(System.in);
        String cmd = new String();
        while (!cmd.equals("exit")) {
            System.out.print(CurrentDir + "$ ");
            cmd = scan.nextLine();
            parse(cmd);
        }
    }

    private static void parse(String cmd) throws RemoteException {
        String cmd_list[] = cmd.split(" ");
        if (cmd_list.length == 0 || cmd_list[0].equals("")) {
            return;
        }
        else if (cmd_list[0].equals("pwd")) {
            if (cmd_list.length == 1) pwd();
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("ls")) {
            if (cmd_list.length == 1) ls(CurrentDir);
            else if (cmd_list.length == 2) ls(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("cd")) {
            if (cmd_list.length == 1) cd("~");
            else if (cmd_list.length == 2) cd(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("mkdir")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) mkdir(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("mv")) {
            if (cmd_list.length < 3) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 3) mv(cmd_list[1], cmd_list[2]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("open")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) open(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("touch")) {
            if (cmd_list.length < 3) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 3) touch(cmd_list[1], cmd_list[2]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else System.err.println(cmd_list[0] + ": command not found");
    }

    private static void pwd() {
        System.out.println(CurrentDir);
    }

    private static void ls(String dir) {
        try {
            System.out.println(MetaData.lstat(dir).toString());
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }
    }

    private static void cd(String dir) {
        // Changes the current directory to dir
        // dir can be a simple name or absolute or relative path
        try {
            MetaData.find(dir);
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }
        CurrentDir = dir;
    }

    private static void mkdir(String dir) {
        try {
            String server = MetaData.find(dir);
            StorageInterface stub = StorageServers.get(server);
            if (stub == null) {
                stub = (StorageInterface) registry.lookup(server);
                StorageServers.put(server, stub);
            }
            stub.create(dir);
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }
    }

    private static void touch(String file, String blob) {
        try {
            String server = MetaData.find(file);
            StorageInterface stub = StorageServers.get(server);
            if (stub == null) {
                stub = (StorageInterface) registry.lookup(server);
                StorageServers.put(server, stub);
            }
            stub.create(file, blob);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private static void mv(String file1, String file2) {
        // Copies file file1 to file2, overwriting if the latter exists
        // file can be a simple name or absolute or relative path
    }

    private static void open(String file) {
        // Opens the file with the proper application, accordingly to its extension
        // file can be a simple name or absolute or relative path
    }

    private static String parsePath(String path) {
        return path;
    }

    private static String[] splitPath(String path) {
        return path.replaceAll("^/", "").replaceAll("/$", "").split("/");
    }
}
