import java.io.File;
import java.rmi.Remote;

public interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    void create(String path) throws Exception;

    void create(String path, String blob) throws Exception;

    void del(String path) throws Exception;

    File get(String path) throws Exception;
}
