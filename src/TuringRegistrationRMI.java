import java.rmi.RemoteException;

/**
 * Classe che implementa l'interfaccia di registrazione, ossia l'implementazione dei metodi del servizio remoto
 */
public class TuringRegistrationRMI implements TuringRegistrationInterface {
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
     * Funzione che verifica se l'username/password/documento passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param argument username/password/documento da verificare
     * @return SUCCESS se username/password/documento e' lecito
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkMinNumCharactersArg(String argument){
        int maxNumCharactersArg = this.serverConfigurationsManagement.getMinNumCharactersArg();
        if(argument.length() < maxNumCharactersArg)
            return FunctionOutcome.FAILURE;  //argomento inferiore num. minimo caratteri consentito
        else
            return FunctionOutcome.SUCCESS;  //argomento non inferiore num. minimo caratteri consentito
    }

    /**
     * Funzione che verifica se l'username/password/documento passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param argument username/password/documento da verificare
     * @return SUCCESS se username/password/documento e' lecito
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkMaxNumCharactersArg(String argument){
        int maxNumCharactersArg = this.serverConfigurationsManagement.getMaxNumCharactersArg();
        if(argument.length() > maxNumCharactersArg)
            return FunctionOutcome.FAILURE;  //argomento supera num. caratteri consentito
        else
            return FunctionOutcome.SUCCESS;  //argomento non supera num.caratteri consentito
    }

    /**
     * Funzione che verifica se il numero di sezione passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param numSections numero di sezione da verificare
     * @return SUCCESS se il numero di sezione e' lecito
     *         FAILURE altrimenti
     */
    private  FunctionOutcome checkMaxNumSectionsPerDocument(int numSections){
        int maxNumSectionsPerDocument = this.serverConfigurationsManagement.getMaxNumSectionsPerDocument();
        if(numSections > maxNumSectionsPerDocument)
            return FunctionOutcome.FAILURE;  //numero sezioni supera valore consentito
        else
            return FunctionOutcome.SUCCESS;  //numero sezioni non supera valore consentito
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di registrazione di un utente
     * @param username nome dell'utente da registrare
     * @param password password del nuovo utente da registrare
     * @return OP_OK se e' stato possibile registrare l'utente
     *         OP_USERNAME_ALREADY_TAKEN  se esiste gia' un username registrato al servizio con questo username
     *         OP_USER_MUST_LOGOUT se il Client che richiede la registrazione e' connesso (per effettuare una
     *         registrazione bisogna essere sloggati)
     */
    public synchronized ServerResponse registerTask(String username, String password) throws RemoteException {

        //verifico se username supera numero minimp caratteri consentito
        FunctionOutcome check = checkMinNumCharactersArg(username);
        if(check == FunctionOutcome.SUCCESS){ //numero caratteri lecito
            //verifico se username supera numero massimo caratteri consentito
            check = checkMaxNumCharactersArg(username);
            if(check == FunctionOutcome.SUCCESS) { //numero caratteri lecito
                //verifico se username contiene solo caratteri alfanumerici
                check = this.fileManagement.IsValidFilename(username);
                if (check == FunctionOutcome.SUCCESS) { //username contiene solo caratteri alfanumerici
                    //verifico se password supera numero minimo caratteri consentito
                    check = checkMinNumCharactersArg(password);
                    if(check == FunctionOutcome.SUCCESS){
                        //verifico se password supera numero massimo caratteri consentito
                        check = checkMaxNumCharactersArg(password);
                        if (check == FunctionOutcome.SUCCESS) { //numero caratteri lecito

                            //provo a soddisfare la richiesta del Client e ritorno esito
                            //verifico che l'utente NON sia connesso (Client deve fare logout per potersi registrare con nuovo username)
                            boolean online = this.serverDataStructures.checkIfUserIsOnline(username);
                            if(online)
                                return ServerResponse.OP_USER_MUST_LOGOUT; //utente connesso

                            //utente e' disconesso => verifico se username e' gia' stato preso da qualche altro utente
                            boolean alreadyTaken = this.serverDataStructures.checkIfUserIsRegister(username);
                            if(alreadyTaken)
                                return ServerResponse.OP_USERNAME_ALREADY_TAKEN; //username gia' in uso

                            //username non e' stato preso
                            //creo istanza dell'utente da registrare e da inserire nella HashTable
                            User newUser = new User(username, password);
                            this.serverDataStructures.insertHashUser(username, newUser);

                            //creo cartella in cui memorizzare i documenti dell'utente da salvare
                            //creo cartella in cui memorizzare i documenti che l'utente sta editando
                            //N.B. cartelle prendono il nome dell'utente perche' usernames e' univoco => nome cartelle univoci
                            String userSaveDirectoryPath = this.serverConfigurationsManagement.getServerSaveDocumentsDirectory() + username + "/";
                            String userEditDirectoryPath = this.serverConfigurationsManagement.getServerSaveDocumentsDirectory() + username + "/";
                            this.fileManagement.createDirectory(userEditDirectoryPath);
                            this.fileManagement.createDirectory(userSaveDirectoryPath);

                            return ServerResponse.OP_OK; //utente registrato con successo
                        }
                        return ServerResponse.OP_PASSWORD_TOO_LONG; //password troppo lunga
                    }
                    else return ServerResponse.OP_PASSWORD_TOO_SHORT; //password troppo corta
                }
                else return ServerResponse.OP_USERNAME_INAVLID_CHARACTERS; //username contiene caratteri speciali
            }
            else return ServerResponse.OP_USERNAME_TOO_LONG; //username troppo lungo
        }
        else return ServerResponse.OP_USERNAME_TOO_SHORT; //username troppo corto

        //@TODO incapsulare risposta in un msg da inviare al Client
    }
}