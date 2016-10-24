import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSTree {
    FSNode root;

    public FSTree() {
        this.root = new FSNode(new String());
    }

    public void addElement(String path) {
        String list[] = splitPath(path);
        getNode(path).addElement(list[list.length - 1]);
    }

    public void delElement(String path) {
        String list[] = splitPath(path);
        getNode(path).delElement(list[list.length - 1]);
    }

    public void printTree() {
        printTree(root, "/");
    }

    private void printTree(FSNode current, String absolutePath) {
        System.out.println(absolutePath);
        for (FSNode node : current.childs)
            printTree(node, absolutePath.concat(node.name + "/"));
    }

    private FSNode getNode(String path) {
        String list[] = splitPath(path);
        FSNode target = root;
        for (String name : Arrays.copyOfRange(list, 0, list.length - 1))
            target = target.getChild(name);
        return target;
    }

    private String[] splitPath(String path) {
        String list[] = path.split("/");
        if (list[0].equals("")) list = Arrays.copyOfRange(list, 1, list.length);
        if (list[list.length - 1].equals("")) list = Arrays.copyOfRange(list, 0, list.length - 1);
        return list;
    }
}

class FSNode {
    String name;
    List<FSNode> childs;

    public FSNode(String name) {
        this.name = name;
        childs = new ArrayList<FSNode>();
    }

    public void addElement(String name) { childs.add(new FSNode(name)); }

    public void delElement(String name) {
        childs.remove(getChild(name));
    }

    public FSNode getChild(String name) {
        for (FSNode child : childs)
            if (child.name.equals(name))
                return child;
        return null;
    }
}
