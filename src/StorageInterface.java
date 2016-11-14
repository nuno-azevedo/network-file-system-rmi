import java.rmi.Remote;

public interface StorageInterface extends Remote {
    // CALLS FROM CLIENT
    void create(String item) throws Exception;

    void create(String item, byte bytes[]) throws Exception;

    void del(String item) throws Exception;

    byte[] get(String item) throws Exception;
}
