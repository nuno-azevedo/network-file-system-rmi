import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FSTree {
    private FSNode root;

    public FSTree() {
        this.root = new FSNode();
    }

    public boolean addNode(String path, NodeType nodeType) {
        String nodes[] = splitPath(path);
        if (nodes.length == 1) {
            return root.addChild(nodes[0], nodeType);
        }
        FSNode target = root;
        for (String node : Arrays.copyOfRange(nodes, 0, nodes.length - 1)) {
            target = target.getChild(node);
            if (target == null) return false;
        }
        return target.addChild(nodes[nodes.length - 1], nodeType);
    }

    public boolean delNode(String path) {
        FSNode target = getNode(path);
        if (target == null) return false;
        return target.autoDelete();
    }

    public FSNode getNode(String path) {
        FSNode target = root;
        String nodes[] = splitPath(path);
        for (String node : nodes) {
            target = target.getChild(node);
            if (target == null) return null;
        }
        return target;
    }

    public void printTree() {
        printTree(root, "/");
    }

    private void printTree(FSNode current, String absPath) {
        System.out.println(absPath);
        for (FSNode node : current.getChilds())
            printTree(node, absPath + node.getName() + "/");
    }

    private String[] splitPath(String path) {
        return path.split("(?=/)");
    }
}

class FSNode {
    private String name;
    private NodeType type;
    private FSNode parent;
    private List<FSNode> childs;

    public FSNode() {
        this.name = null;
        this.type = NodeType.Dir;
        this.parent = null;
        this.childs = new ArrayList<FSNode>();
    }

    public FSNode(String name, NodeType nodeType, FSNode parent) {
        this.name = name;
        this.type = nodeType;
        this.parent = parent;
        this.childs = new ArrayList<FSNode>();
    }

    public boolean isDir() {
        return type == NodeType.Dir;
    }

    public boolean isFile() {
        return type == NodeType.File;
    }

    public String getName() {
        return name;
    }

    public FSNode getParent() {
        return parent;
    }

    public FSNode getChild(String name) {
        for (FSNode child : childs)
            if (child.name.equals(name)) return child;
        return null;
    }

    public List<FSNode> getChilds() {
        if (this.type == NodeType.File) return null;
        return childs;
    }

    public List<String> getChildsNames() {
        if (this.type == NodeType.File) return null;
        return childs.stream().map(FSNode::getName).collect(Collectors.toList());
    }

    public boolean addChild(String name, NodeType type) {
        if (this.type == NodeType.File || getChild(name) != null) return false;
        FSNode child = new FSNode(name, type, this);
        return childs.add(child);
    }

    public boolean delChild(String name) {
        FSNode child = getChild(name);
        if (child == null) return false;
        return childs.remove(child);
    }

    public boolean autoDelete() {
        return parent.childs.remove(this);
    }
}

enum NodeType {
    Dir, File
}
