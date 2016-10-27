import java.util.ArrayList;
import java.util.List;

public class Stat {
    public String server;
    public List<String> items;

    public Stat () {
        this.server = new String();
        this.items = new ArrayList<String>();
    }

    public String toString() {
        String str = new String();
        str = str.concat("{ " + server + ", { ");
        int i = 0;
        for (i = 0; i < items.size() - 1; i++)
            str = str.concat(items.get(i) + ", ");
        str = str.concat(items.get(i) + " } }");
        return str;
    }
}