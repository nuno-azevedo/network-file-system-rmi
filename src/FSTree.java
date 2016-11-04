import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FSTree {
    private FSNode root;

    public FSTree() {
        this.root = new FSNode();
    }

    public boolean addNode(String path) {
        String nodes[] = splitPath(path);
        if (nodes.length == 1) {
            return root.addChild(nodes[0]);
        }
        FSNode target = root;
        for (String node : Arrays.copyOfRange(nodes, 0, nodes.length - 1)) {
            target = target.getChild(node);
            if (target == null) return false;
        }
        return target.addChild(nodes[nodes.length - 1]);
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

    private void printTree(FSNode current, String absolutePath) {
        System.out.println(absolutePath);
        for (FSNode node : current.getChilds())
            printTree(node, absolutePath + node.getName() + "/");
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

    public boolean addChild(String name) {
        FSNode child = new FSNode(name, this);
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

    public FSNode getChild(String name) {
        for (FSNode child : childs)
            if (child.name.equals(name)) return child;
        return null;
    }
}
