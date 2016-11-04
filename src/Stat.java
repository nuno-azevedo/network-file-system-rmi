import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stat implements Serializable {
    public String server;
    public List<String> items;

    public Stat() {
        this.server = new String();
        this.items = new ArrayList<String>();
    }

    public Stat(String server, List<String> items) {
        this.server = server;
        this.items = items;
    }

    public String toString() {
        String stat = new String();
        stat = stat.concat("{ " + server + ", { ");
        if (items.size() == 0) {
            stat = stat.concat(" } }");
            return stat;
        }
        int i = 0;
        for (i = 0; i < items.size() - 1; i++)
            stat = stat.concat(items.get(i) + ", ");
        stat = stat.concat(items.get(i) + " } }");
        return stat;
    }
}