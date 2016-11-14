import java.rmi.Remote;

public interface MetaDataInterface extends Remote {
    // CALLS FROM STORAGE SERVER
    void addStorageServer(String hostname, String top_dir) throws Exception;

    void delStorageServer(String hostname, String top_dir) throws Exception;

    void addStorageItem(String item, NodeType type) throws Exception;

    void delStorageItem(String item) throws Exception;

    // CALLS FROM CLIENT
    String find(String item) throws Exception;

    Stat lstat(String item) throws Exception;

    boolean isDir(String item) throws Exception;

    boolean isFile(String item) throws Exception;

    boolean checkExists(String item) throws Exception;
}
