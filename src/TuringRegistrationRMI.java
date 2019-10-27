import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

/**
 * Classe che implementa l'interfaccia di registrazione, ossia l'implementazione dei metodi del servizio remoto
 */
public class TuringRegistrationRMI implements TuringRegistrationRMIInterface {
    private ServerConfigurationsManagement serverConfigurationsManagement;
    private ServerDataStructures serverDataStructures;
    private FileManagement fileManagement;

    /**
     * Costruttore della classe "TuringRegistrationRMI"
     * @param configurationsManagement classe che contiene le variabili di configurazione del Server
     * @param dataStructures classe che contiene le strutture dati del Server
     */
    public TuringRegistrationRMI(ServerConfigurationsManagement configurationsManagement, ServerDataStructures dataStructures) {
        this.serverConfigurationsManagement = configurationsManagement;
        this.serverDataStructures = dataStructures;
        this.fileManagement = new FileManagement();
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di registrazione di un utente
     * @param username nome dell'utente da registrare
     * @param password password del nuovo utente da registrare
     * @param currentUser utente, eventuale, attualmente connesso
     * @return OP_OK se e' stato possibile registrare l'utente
     *         OP_USERNAME_ALREADY_TAKEN  se esiste gia' un username registrato al servizio con questo username
     *         OP_USER_MUST_LOGOUT se il Client che richiede la registrazione e' connesso (per effettuare una
     *         registrazione bisogna essere sloggati)
     */
    public synchronized ServerResponse registerTask(String username, String password, String currentUser){

        //verifico se username contiene solo caratteri alfanumerici
        FunctionOutcome check = this.fileManagement.IsValidFilename(username);

        if(check == FunctionOutcome.SUCCESS){
            //provo a soddisfare la richiesta del Client e ritorno esito
            //verifico che l'utente NON sia connesso (Client deve fare logout per potersi registrare con nuovo username)
            boolean online = this.serverDataStructures.checkIfUserIsOnline(currentUser);

            if(online)
                return ServerResponse.OP_USER_MUST_LOGOUT; //utente connesso
            return this.serverDataStructures.registerUser(username, password);
        }
        else return ServerResponse.OP_USERNAME_INAVLID_CHARACTERS; //username contiene caratteri speciali
    }
}