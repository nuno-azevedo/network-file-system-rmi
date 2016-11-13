import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static Registry registry;
    private static MetaDataInterface MetaData;
    private static String LocalPath;
    private static String CurrentDir = "/";
    private static HashMap<String, String> Applications = new HashMap<String, String>();

    private Client() {

    }

    public static void main(String args[]) throws RemoteException {
        if (args.length != 3) {
            System.err.println("USAGE: java Client $LOCAL_PATH $CONFIG_FILE $METADATA_HOSTNAME");
            System.exit(1);
        }

        LocalPath = args[0];
        parseConfFile(args[1]);

        try {
            registry = LocateRegistry.getRegistry();
            MetaData = (MetaDataInterface) registry.lookup(args[2]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        Scanner scan = new Scanner(System.in);
        String cmd = new String();
        while (!cmd.equals("exit")) {
            System.out.print(CurrentDir + "$ ");
            cmd = scan.nextLine();
            parseCommand(cmd);
        }
    }

    private static void parseConfFile(String path) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#")) lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception e) { }
            }
        }
        for (String line : lines) {
            String extensions[] = line.split(", +| +");
            for (int i = 0; i < extensions.length - 1; i++)
                Applications.put(extensions[i], extensions[extensions.length - 1]);
        }
    }

    private static void parseCommand(String cmd) {
        String cmd_list[] = cmd.split(" +");
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
            if (cmd_list.length == 1) cd("/");
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
        else if (cmd_list[0].equals("rm")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) rm(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("open")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) open(cmd_list[1]);
            else System.err.println(cmd_list[0] + ": too many arguments");
        }
        else if (cmd_list[0].equals("touch")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) touch(cmd_list[1]);
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
            if (!MetaData.checkExists(path)) {
                System.out.println("cannot change to directory ‘" + dir + "’: no such file or directory");
                return;
            }
            if (!MetaData.isDir(path)) {
                System.out.println("cannot change to directory ‘" + dir + "’: not a directory");
                return;
            }
        } catch (Exception e) {
            return;
        }
        CurrentDir = path;
    }

    private static void mkdir(String dir) {
        String path = parsePath(dir);
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

    private static void touch(String file) {
        String path = parsePath(file);
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.create(path, new String());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void nano(String file) {
        String path = parsePath(file);
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

    private static void rm(String item) {
        String path = parsePath(item);
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.del(path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void open(String file) {
        // Opens the file with the proper application, accordingly to its extension
        // file can be a simple name or absolute or relative path
        if (!file.contains(".")) {
            System.out.println("cannot open file ‘" + file + "’: extension not found");
            return;
        }
        String extension = file.substring(file.lastIndexOf(".") + 1);
        try {
            String application = Applications.get(extension);
            Process process = Runtime.getRuntime().exec(new String[] { application, file });
            process.waitFor();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static String parsePath(String path) {
        if (!path.startsWith("/")) {
            if (CurrentDir.endsWith("/")) path = CurrentDir + path;
            else path = CurrentDir + "/" + path;
        }
        if (path.equals("/.")) return CurrentDir;
        List<String> parsed = new ArrayList<String>();
        String nodes[] = splitPath(path);
        for (String node : nodes) {
            if (node.equals("/."));
            else if (node.equals("/..")) {
                if (parsed.size() > 1) parsed.remove(parsed.size() - 1);
                else if (parsed.size() == 1) parsed.set(0, "/");
                else return path;
            }
            else parsed.add(node);
        }
        return String.join("", parsed).replaceAll("//", "/");
    }

    private static String getTopPath(String path) {
        return path.split("(?=/)")[0];
    }

    private static String[] splitPath(String path) {
        return path.split("(?=/)");
    }
}
