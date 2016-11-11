import java.rmi.Remote;

public interface MetaDataInterface extends Remote {
    // CALLS FROM STORAGE SERVER
    void addStorageServer(String hostname, String top_dir) throws Exception;

    void delStorageServer(String top_dir) throws Exception;

    void addStorageItem(String item) throws Exception;

    void delStorageItem(String item) throws Exception;

    // CALLS FROM CLIENT
    String find(String path) throws Exception;

    Stat lstat(String path) throws Exception;
}
