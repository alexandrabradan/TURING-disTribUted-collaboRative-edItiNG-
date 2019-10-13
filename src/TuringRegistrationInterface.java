import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia (Stub) di registrazione al servizio Turing che utilizza RMI. La classe è un' interfaccia remota, dato che
 * estende Remote e serve per identificare gli oggetti che possono essere utilizzati in remoto
 */
public interface TuringRegistrationInterface extends Remote {

    /**
     * Funzione che consente ad un utente di registrarsi al servizio
     * @param username nome dell'utente
     * @param password password associata al nome
     * @return OP_OK se l'utente è stato registrato
     *         OP_REGISTER_USERNAME_ALREADY_TAKEN se l'utente era già registrato
     *         OP_REGISTER_USER_ALREADY_ONLINE bisogna prima fare logout per registrare nuovo utente
     * @throws RemoteException I metodi remoti devono dichiarare di sollevare eccezioni remote
     */
    public ServerResponse registerTask(String username, String password) throws RemoteException;
}