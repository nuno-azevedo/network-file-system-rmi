import java.rmi.Remote;
import java.rmi.RemoteException;

interface Hello extends Remote {
    String sayHello() throws RemoteException;
}
