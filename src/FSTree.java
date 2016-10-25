import java.util.ArrayList;
import java.util.List;

public class FSTree {
    private FSNode root;

    public FSTree() {
        this.root = new FSNode();
    }

    public void addNode(String path) {
        FSNode target = root;
        String nodes[] = splitPath(path);
        for (String node : nodes) {
            if (target.getChild(node) == null)
                target.addChild(node);
            target = target.getChild(node);
        }
    }

    public void delNode(String path) {
        getNode(path).delete();
    }

    public FSNode getNode(String path) {
        FSNode target = root;
        String list[] = splitPath(path);
        for (String node : list) {
            target = target.getChild(node);
            if (target == null) break;
        }
        return target;
    }

    public void printTree() {
        printTree(root, "/");
    }

    private void printTree(FSNode current, String absolutePath) {
        System.out.println(absolutePath);
        for (FSNode node : current.getChilds())
            printTree(node, absolutePath.concat(node.getName() + "/"));
    }

    private String[] splitPath(String path) {
        return path.replaceAll("^/", "").replaceAll("/$", "").split("/");
    }

    public static void main(String args[]) {
        FSTree fileSystem = new FSTree();
        fileSystem.addNode("/storage/data/sd/");
        fileSystem.printTree();
    }
}

class FSNode {
    private String name;
    private FSNode parent;
    private List<FSNode> childs;

    public FSNode() {
        this.childs = new ArrayList<FSNode>();
    }

    public FSNode(String name, FSNode parent) {
        this.name = name;
        this.parent = parent;
        this.childs = new ArrayList<FSNode>();
    }

    public String getName() { return name; }

    public FSNode getParent() { return parent; }

    public List<FSNode> getChilds() { return childs; }

    public void addChild(String name) { childs.add(new FSNode(name, this)); }

    public void delChild(String name) { childs.remove(getChild(name)); }

    public void delete() { parent.childs.remove(this); }

    public FSNode getChild(String name) {
        for (FSNode child : childs)
            if (child.name.equals(name))
                return child;
        return null;
    }
}
