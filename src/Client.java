import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static Registry registry;
    private static MetaDataInterface MetaData;
    private static String CurrentDir;
    private static String LocalPath;

    private Client() {

    }

    public static void main(String args[]) throws RemoteException {
        if (args.length != 2) {
            System.err.println("USAGE: java Client $LOCAL_PATH $METADATA_HOSTNAME");
            System.exit(1);
        }

        try {
            registry = LocateRegistry.getRegistry();
            MetaData = (MetaDataInterface) registry.lookup(args[1]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        CurrentDir = "/";
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
        else if (cmd_list[0].equals("nano")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) nano(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else System.err.println(cmd_list[0] + ": command not found");
    }

    private static void pwd() {
        System.out.println(CurrentDir);
    }

    private static void ls(String dir) {
        String path = parsePath(dir);
        try {
            Stat stat = MetaData.lstat(path);
            java.util.Collections.sort(stat.items);
            for (int i = 0; i < stat.items.size(); i++) {
                if (i % 5 == 0 && i != 0) System.out.println();
                System.out.print(stat.items.get(i) + "\t");
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void cd(String dir) {
        // Changes the current directory to dir
        // dir can be a simple name or absolute or relative path
        String path = parsePath(dir);
        try {
            MetaData.find(path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        CurrentDir = path;
    }

    private static void mkdir(String dir) {
        String path = parsePath(dir);
        if (checkTopPath(path)) {
            System.out.println("cannot create directory ‘" + dir + "’: not allowed on root directory");
            return;
        }
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.create(path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void nano(String file) {
        String path = parsePath(file);
        if (checkTopPath(path)) {
            System.out.println("cannot create file ‘" + file + "’: not allowed on root directory");
            return;
        }
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            String line, blob = new String();
            while((line = systemIn.readLine()) != null) {
                blob = blob.concat(line).concat("\n");
            }
            stub.create(path, blob);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void mv(String file1, String file2) {
        // Copies file file1 to file2, overwriting if the latter exists
        // file can be a simple name or absolute or relative path
    }

    private static void open(String file) {
        System.out.println(parsePath(file));
        if (LocalPath != null);
        // Opens the file with the proper application, accordingly to its extension
        // file can be a simple name or absolute or relative path
    }

    private static String parsePath(String path) {
        if (!path.startsWith("/")) {
            if (CurrentDir.endsWith("/")) path = CurrentDir + path;
            else path = CurrentDir + "/" + path;
        }
        List<String> parsed = new ArrayList<String>();
        String nodes[] = splitPath(path);
        for (String node : nodes) {
            if (node.equals("/."));
            else if (node.equals("/..")) parsed.remove(parsed.size() - 1);
            else parsed.add(node);
        }
        return String.join("", parsed);
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

    private static String getTopPath(String path) {
        return path.split("(?=/)")[0];
    }

    private static String[] splitPath(String path) {
        return path.split("(?=/)");
    }
}
