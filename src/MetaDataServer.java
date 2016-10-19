public class MetaDataServer {

    // CALLS FROM STORAGE SERVER
    private boolean add_storage_server(String host, String top_dir) {
        // Example: add_storage_server("machine1.dcc.fc.up.pt", "/courses");
        return false;
    }

    private boolean del_storage_server(String top_dir) {
        // Example: del_storage_server("/courses");
        return false;
    }

    private boolean add_storage_item(String item) {
        // Example: add_storage_item("/courses/video1.avi");
        return false;
    }

    private boolean del_storage_item(String item) {
        // Example: del_storage_item("/courses/video1.avi");
        return false;
    }


    // CALLS FROM CLIENT
    public String find(String path) {
        // Example: find("/courses"); -> "machine1.dcc.fc.up.pt"
        return "";
    }

    public String lstat(String path) {
        // Example: lstat("/courses"); -> { "machine1.dcc.fc.up.pt", { "afile.txt", "bfile.txt", "..." } }
        return "";
    }
}
