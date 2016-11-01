import com.sun.corba.se.spi.orbutil.fsm.FSM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FSTree {
    private FSNode root;

    public FSTree() {
        this.root = new FSNode();
    }

    public void addNode(String path) throws Exception {
        if (!checkPath(path)) throw new Exception("invalid path ‘" + path + "’");

        String nodes[] = splitPath(path);
        if (nodes.length == 1) root.addChild(nodes[0]);
        else {
            FSNode target = root;
            for (String node : Arrays.copyOfRange(nodes, 0, nodes.length - 1)) {
                target = target.getChild(node);
                if (target == null) throw new Exception("no such file or directory ‘" + path + "’");
            }
            target.addChild(nodes[nodes.length - 1]);
        }
    }

    public void delNode(String path) throws Exception {
        if (!checkPath(path)) throw new Exception("invalid path ‘" + path + "’");

        try {
            FSNode target = getNode(path);
            target.autoDelete();
        } catch (Exception e) {
            throw e;
        }
    }

    public FSNode getNode(String path) throws Exception {
        String nodes[] = splitPath(path);
        FSNode target = root;
        for (String node : nodes) {
            target = target.getChild(node);
            if (target == null) throw new Exception("no such file or directory ‘" + path + "’");
        }
        return target;
    }

    public void printTree() {
        printTree(root, "/");
    }

    private void printTree(FSNode current, String absolutePath) {
        System.out.println(absolutePath);
        for (FSNode node : current.getChilds())
            printTree(node, absolutePath + node.getName() + "/");
    }

    private boolean checkPath(String path) {
        String valid_path = "^/([^/ ]+(/)?)+$";
        if (path.matches(valid_path)) return true;
        return false;
    }

    private String[] splitPath(String path) {
        return path.replaceAll("^/", "").replaceAll("/$", "").split("/");
    }
}

class FSNode {
    private String name;
    private FSNode parent;
    private List<FSNode> childs;

    public FSNode() {
        this.name = "root";
        this.parent = null;
        this.childs = new ArrayList<FSNode>();
    }

    public FSNode(String name, FSNode parent) {
        this.name = name;
        this.parent = parent;
        this.childs = new ArrayList<FSNode>();
    }

    public String getName() {
        return name;
    }

    public FSNode getParent() {
        return parent;
    }

    public List<FSNode> getChilds() {
        return childs;
    }

    public List<String> getChildsNames() {
        return childs.stream().map(FSNode::getName).collect(Collectors.toList());
    }

    public void addChild(String name) {
        FSNode child = new FSNode(name, this);
        childs.add(child);
    }

    public void delChild(String name) {
        FSNode child = getChild(name);
        childs.remove(child);
    }

    public void autoDelete() {
        parent.childs.remove(this);
    }

    public FSNode getChild(String name) {
        for (FSNode child : childs)
            if (child.name.equals(name)) return child;
        return null;
    }
}
