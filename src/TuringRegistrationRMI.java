import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe che implementa l'interfaccia di registrazione, ossia l'implementazione dei metodi del servizio remoto
 */
public class TuringRegistrationRMI implements TuringRegistrationInterface {
    private Set<String> onlineUsers;
    private ConcurrentHashMap<String,User> hash_users;

    /**
     * Costruttore della classe "TuringRegistrationRMI"
     * @param onlineUsers insieme degli utenti online
     * @param hash_users tabella hash che rappresenta il database degli utenti
     */
    protected TuringRegistrationRMI(Set<String> onlineUsers, ConcurrentHashMap<String,User> hash_users) {
        this.onlineUsers = onlineUsers;
        this.hash_users = hash_users;
    }

    /**
     * Funzione che consente ad un utente di registrarsi al servizio
     * @param username nome dell'utente
     * @param password password associata al nome
     * @return OP_OK se l'utente è stato registrato
     * 		   OP_REGISTER_USERNAME_ALREADY_TAKEN se l'utente era già registrato
     * 		   OP_REGISTER_USER_ALREADY_ONLINE bisogna prima fare logout per registrare nuovo utente
     * @throws RemoteException I metodi remoti devono dichiarare di sollevare eccezioni remote
     *
     * MUTAUA ESCLUSIONE IMPLICITA SULLE STRUTTURE DATI:
     * a) BlockingQueue : coda utenti online
     * b) ConcurrentHashTable : database utenti registrati al servizio
     */
    public synchronized ServerResponse registerUser(String username, String password) throws RemoteException {
        // posso registrare un nuovo utente solo se NON:
        // 1. e' online => DEVE FARE LOGOUT PRIMA
        // 2. e' gia' registrato => SCEGLIERE ALTRO USERNAME
        if (!onlineUsers.contains(username)) {  //utente non e' loggato

            //verifico se posso registrare utente (username NON deve essere gia' stato preso)
            boolean check = hash_users.containsKey(username);

            if (check) {  //username gia' in uso => utente gia' registrato
                return ServerResponse.OP_REGISTER_USERNAME_ALREADY_TAKEN;
            } else {  //utente non registratrato
                //creo utente
                User u = new User(username, password);

                //inserisco utente nella hash_users
                this.hash_users.put(username, u);

                return ServerResponse.OP_REGISTER_OK;
            }
        }
        else  //utente e' logato => deve fare logout per poter fare nuova registrazione
            return ServerResponse.OP_REGISTER_USER_ALREADY_ONLINE;
    }
}