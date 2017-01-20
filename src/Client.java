import java.io.*;
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
    private static String CurrentDir = "/";
    private static HashMap<String, String> Applications = new HashMap<String, String>();

    private Client() {

    }

    public static void main(String args[]) throws RemoteException {
        if (args.length != 2) {
            System.err.println("USAGE: java Client $METADATA_HOSTNAME $CONFIG_FILE");
            System.exit(1);
        }

        parseConfFile(args[1]);
        try {
            registry = LocateRegistry.getRegistry();
            MetaData = (MetaDataInterface) registry.lookup(args[0]);
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
            System.err.println(e.getMessage());
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
        else if (cmd_list[0].equals("touch")) {
            if (cmd_list.length == 1) System.err.println(cmd_list[0] + ": missing arguments");
            else if (cmd_list.length == 2) touch(cmd_list[1]);
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
                System.out.print(stat.items.get(i).replace("/", "") + "\t");
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void cd(String dir) {
        // Changes the current directory to dir
        // dir can be a simple name or absolute or relative path
        String path = parsePath(dir);
        if (path.equals("/")) {
            CurrentDir = path;
            return;
        }
        try {
            if (!MetaData.checkExists(path)) {
                throw new Exception("cannot change to directory ‘" + dir + "’: no such file or directory");
            }
            if (!MetaData.isDir(path)) {
                throw new Exception("cannot change to directory ‘" + dir + "’: not a directory");
            }
            CurrentDir = path;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void mkdir(String dir) {
        String path = parsePath(dir);
        if (checkTopPath(path)) {
            System.err.println("cannot create directory ‘" + dir + "’: not allowed on root directory");
            return;
        }
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.create(path);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void touch(String file) {
        String path = parsePath(file);
        if (checkTopPath(path)) {
            System.err.println("cannot create file ‘" + file + "’: not allowed on root directory");
            return;
        }
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.create(path, new byte[0]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void mv(String file1, String file2) {
        // Copies file file1 to file2, overwriting if the latter exists
        // file can be a simple name or absolute or relative path
        String path1 = parsePath(file1);
        String path2 = parsePath(file2);
        try {
            if (MetaData.isDir(path2)) {
                String nodes[] = path1.split("/");
                String file = nodes[nodes.length - 1];
                path2 += "/" + file;
            } else if (checkTopPath(path2)) {
                System.err.println("cannot move file ‘" + file1 + "’ to file ‘" + file2 + "’: not allowed on root directory");
                return;
            }
            if (path1.equals(path2)) {
                System.err.println("cannot move file ‘" + file1 + "’ to file ‘" + file2 + "’: same location");
                return;
            }
            String top_dir = getTopPath(path1);
            String server = MetaData.find(top_dir);
            StorageInterface stub1 = (StorageInterface) registry.lookup(server);
            byte bytes[] = stub1.get(path1);

            top_dir = getTopPath(path2);
            server = MetaData.find(top_dir);
            StorageInterface stub2 = (StorageInterface) registry.lookup(server);
            stub2.create(path2, bytes);
            stub1.del(path1);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void rm(String item) {
        String path = parsePath(item);
        if (checkTopPath(path)) {
            System.err.println("cannot delete item ‘" + item + "’: not allowed on root directory");
            return;
        }
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            StorageInterface stub = (StorageInterface) registry.lookup(server);
            stub.del(path);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void open(String file) {
        // Opens the file with the proper application, accordingly to its extension
        // file can be a simple name or absolute or relative path
        String path = parsePath(file);
        if (checkTopPath(path)) {
            System.err.println("cannot open file ‘" + file + "’: not allowed on root directory");
            return;
        }
        StorageInterface stub = null;
        File target = null;
        FileOutputStream fos = null;
        try {
            String top_dir = getTopPath(path);
            String server = MetaData.find(top_dir);
            stub = (StorageInterface) registry.lookup(server);
            byte bytes[] = stub.get(path);
            String name = path.substring(path.lastIndexOf("/"));
            target = new File(System.getProperty("user.home") + "/.cache" + name);
            if (!target.exists() && !target.createNewFile()) {
                System.err.println("cannot open file ‘" + file + "’: failed to create local file");
                return;
            }
            fos = new FileOutputStream(target);
            fos.write(bytes);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        } finally {
           if (fos != null) {
                try { fos.close(); } catch (Exception e) { }
           }
        }
        Process process = null;
        try {
            String extension = path.substring(path.lastIndexOf(".") + 1);
            String application = Applications.get(extension);
            if (application == null) {
                System.err.println("cannot open file ‘" + file + "’: unknown file extension");
                return;
            }
            process = Runtime.getRuntime().exec(new String[] { application, target.getPath() });
            process.waitFor();
        } catch (Exception e) {
            System.err.println("cannot open file ‘" + file + "’: failed to open application");
            return;
        } finally {
            if (process != null) {
                try { process.destroy(); } catch (Exception e) { }
            }
        }
        FileInputStream fis = null;
        try {
            byte bytes[] = new byte[(int) target.length()];
            fis = new FileInputStream(target);
            fis.read(bytes);
            stub.create(path, bytes);
            target.delete();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { }
            }
        }
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
            else if (node.equals("/..")) {
                if (parsed.size() > 0) parsed.remove(parsed.size() - 1);
                else return new String();
            }
            else parsed.add(node);
        }
        if (parsed.size() == 0) return "/";
        return String.join("", parsed);
    }

    private static boolean checkTopPath(String top_dir) {
        String valid_top_dir = "^/([^/\0]+/?){0,1}$";
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
