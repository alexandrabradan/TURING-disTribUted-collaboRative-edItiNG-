import java.net.InetAddress;
import java.nio.channels.SocketChannel;

public class TuringTask {
    /**
     * Classe che contiene variabili di configurazione
     */
    private ServerConfigurationsManagement configurationsManagement;
    /**
     * Classe che contiene le strutture dati del Server
     */
    private ServerDataStructures serverDataStructures;
    /**
     * Classe che contiene metodi per leggere richieste e scrivere risposte di esito ai Clients
     */
    private ServerMessageManagement serverMessageManagement;
    /**
     * SocketChannel del Client di cui bisogna soddisfare richiesta e scrivere esito
     */
    private SocketChannel client;
    /**
     * Classe per gestire files e directories
     */
    private FileManagement fileManagement;

    /*
        N.B. Eccedenza numero caratteri consentiti e limite massimo sezioni e' stata fatta durante il parsing della
             richiesta letta sul SocketChannel => nomi username/password/documenti corretti e numero max. sezioni lecite
     */


    /**
     * Costruttore della classe TuringTask
     * @param configurationsManagement classe che contiene le variabili di configurazione del Server
     * @param serverDataStructures classe che contiene le strutture dati del Server
     * @param serverMessageManagement classe che contiene i metodi per leggere richieste ed inviare risposte
     * @param client SocketChannel del Client di cui bisogna soddisfare la richiesta ed inviare risposta di esito
     */
    public TuringTask(ServerConfigurationsManagement configurationsManagement, ServerDataStructures serverDataStructures,
                                                ServerMessageManagement serverMessageManagement, SocketChannel client){
        this.configurationsManagement = configurationsManagement;
        this.serverDataStructures = serverDataStructures;
        this.serverMessageManagement = serverMessageManagement;
        this.client = client;
        this.fileManagement = new FileManagement();
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di connessione di un utente al servizio
     * @param username nome dell'utente da connettere
     * @param password password dell' utente da connettere
     * @return OP_OK se la connessione ha avuto successo
     *         OP_USER_ALREADY_ONLINE se l'utente era gia' connesso
     *         OP_USER_NOT_REGISTERED se non esiste nessun utente registrato con questo username
     *         OP_PASSWORD_INCORRECT se la password non corrisponde al quella fornita dall'utente in fase di
     *         registrazione
     */
    public FunctionOutcome loginTask(String username, String password){

        //verifico che l'utente non sia gia' connesso
        boolean online = this.serverDataStructures.checkIfUserIsOnline(username);
        if(online)
            //utente gia' connesso (eventualmente con altro Client)
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_ALREADY_ONLINE, "");

        //utente disconesso => verifico se e' registrato al sevizio
        boolean register = this.serverDataStructures.checkIfUserIsRegister(username);
        if(!register)
            //utente non registrato al servizio
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_REGISTERED, "");

        //utente registrato al servizio => verifico se la password del login corrisponde a quella fornita in fase
        //di registrazione
        User user = this.serverDataStructures.getUserFromHash(username);  //recupero istanza dell'utente
        boolean checkPassword = user.equalsPassword(password); //verifico password
        if(!checkPassword)
            //password non corrisponde a quella fornita fase di registrazione
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_PASSWORD_INCORRECT, "");

        //password corrisponde a quella fornita in fase di registrazione
        //connetto utente => inserisco SocketChannel e utente nella HashTable degli utenti online
        this.serverDataStructures.putToOnlineUsers(client, username);

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di disconessione di un utente dal servizio
     * @return OP_OK se la disconessione dell'utente ha avuto successo
     *         OP_USER_NOT_ONLINE se l'utente era gia' disconesso
     */
    public FunctionOutcome logoutTask(){

        //disconnetto utente => rimuovo utente dalla ht utenti connessi
        String username = this.serverDataStructures.removeFromOnlineUsers(client);

        //se la chiave/SocketChannel non era presente => utente non connesso
        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //(chiave, valore) eliminati dalla ht utenti online
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di creazione  di un nuovo documento con numero di sezioni
     * esplicitato
     * @param document nome del nuovo documento da creare
     * @param numSections numero di sezioni che il documento deve avere
     * @return OP_OK se il documento e le relative sezioni sono state create con successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_DOCUMENT_ALREADY_EXIST se il nuovo documento che si vuole creare esiste gia'
     */
    public FunctionOutcome createTask(String document, int numSections){
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. recupero dalla HashTable degli utenti registrati la sua istanza di User
        //2. verifico che nuovo documento che vuole creare non esista gia'
        //3. creo nuovo documento ed aggiorno la sua istanza
        //4. aggiorno HashTable dei documenti
        User user = this.serverDataStructures.getUserFromHash(username);

        /*formato nome documento : NOMEDOCUMENTO_USERNAME*/
        String documentName = document + "_" + username;

        // acceddo tramite synchronized all'oggetto che garantisce sincronizzazione tra ht utenti e ht documenti
        synchronized(this.serverDataStructures.getLockHash()) {

            //verifico se documento e' gia' esistente (controllo presenza documento all'interno della ht dei documenti)
            boolean exist = this.serverDataStructures.checkIfDocumentExist(documentName);

            if(exist){
                //utente ha gia' creato un documento con lo stesso nome (dato che nome documento e' dato dalla
                // concatenazione del nome_documento e dell'username del creatore, e' sicuramente stato lui a crearlo)
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_ALREADY_EXIST, "");
            }
            else{
                //ricavo InetAddress da associare alla chat del documento
                InetAddress chatAddress = new MulticastAddressRandomGenerator(this.serverDataStructures).getRandomAddress();

                //verifico che indirizzo di multicast non sia null (spazio degli indirizzi di multicast esaurito)
                if(chatAddress == null)
                    return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_MULTICAST_ADDRESS_RUN_OUT,
                                                                                                        "");

                //documento non esiste => creo nuova istanza di Document
                Document doc = new Document(documentName, username, numSections, chatAddress);

                //inserisco istanza del documento nella HashTable dei documenti
                this.serverDataStructures.insertHashDocument(documentName, doc);

                //inserisco documento nell'insieme dei documenti che utente puo' modificare
                user.addSetDoc(documentName);
            }
        }

        //recupero cartella dedicata alla memorizzazione dei files dell'utente
        String userSaveDirectoryPath = this.configurationsManagement.getServerSaveDocumentsDirectory() + username + "/";

        //recupero path della cartella/documento
        String userDocumentPath = userSaveDirectoryPath + documentName +  "/"; //documento e' una cartella

        //creo la cartella/documento
        this.fileManagement.createDirectory(userDocumentPath);

        // cartella/documento creata con successo
        //creo le sezioni del documento => creo un numero di files appropiato
        for(int i = 1; i <= numSections; i++){
            String userSectionFile = userDocumentPath + i + ".txt";  //nome sezione corrisponde suo numero
            this.fileManagement.createFile(userSectionFile); //creo sezione "i"
        }

        // cartella(documento) e files(sezioni) creati con successo
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di condivisione del documento passato come argomento con
     * il destinatatio passato come argomento
     * @param document documento da condividere
     * @param dest destinatario con cui condividere documento
     * @return OP_OK se il documento e' stato condiviso con successo con il destinatario
     *          OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *          OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     *          OP_DOCUMENT_NOT_EXIST se il documento che si vuole condividere non esiste
     *          OP_USER_NOT_CREATOR se l'utente che richiede la condivisione non e' il creatore del documento
     *          OP_USER_IS_DEST se l'utente che richiede condivisione lo fa con se stesso
     *          OP_DEST_ALREADY_CONTRIBUTOR se il destinatario con cui si vuole condividere il documento e' gia' suo
     *           collaboratore
     *          OP_DEST_NOT_REGISTERED se il destinatario con cui si vuole condividere il documento non e' registrato
     *           al servizio (destinatario sconosciuto)
     */
    public FunctionOutcome shareTask(String document, String dest){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di visualizzazione del contenuto del documento passato come
     * argomento. La visualizzazione da parte del client del documento richiesto e' consentita grazie:
     * 1. al download dei files che compongono il documento/cartella
     * 2. alla lettura dei files scarivati
     * 3. alla visualizzaziopne del contenuto dei files letti
     * @param document documento di cui visualizzare il contenuto
     * @return OP_OK se la visualizzazione del documento ha avuto successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste (non rientra nella lista dei documenti creati /
     *          condivisi con l'utente)
     *           @TODO nel client verificare prima se download di qualche sezione (CHE DEVO NOTIFICARE AL CLIENT) non e'
     *           gia avvenuto (evito richiesta)
     */
    public FunctionOutcome showDocumentTask(String document){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di visualizzazione della sezione del del documento passati come
     * argomento. La visualizzazione da parte del client della sezione del documento richiesto e' consentita grazie:
     * 1. download del file corrispondente alla sezione richiesta (documento e' un insieme di files che sono le sezioni)
     * 2. lettura del file scaricato
     * 3. visualizzaziopne del contenuto del file letto
     * @param document documento di cui visualizzare il contenuto
     * @return OP_OK se la visualizzazione del documento ha avuto successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste (non rientra nella lista dei documenti creati /
     *          condivisi con l'utente)
     *         OP_SECTION_NOT_EXIST se la sezione richiesta per la visualizzazione non esiste (non rientra nel range
     *          delle sezioni associate al documento fornito)
     *          @TODO nel client verificare prima se download di tale sezione non e' gia avvenuto (evito richiesta)
     */
    public FunctionOutcome showSectionTask(String document, int numSection){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di visualizzazione dei documenti di cui l'utente che richiede
     * l'operazione e' creato oppure collaboratore (vengono fornite anche le informazioni sugli altri collaboratori/
     * creatori)
     * @return OP_OK se la visualizzazione delle informazioni dei documenti dell'utente hanno avuto successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     */
    public FunctionOutcome listTask(){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di editare la sezione del documento passati come argomento
     * @param document documento di cui editare la sezione
     * @param numSection sezione da editare
     * @return OP_OK se la modifica della sezione del documento ha avuto successo
     *         OP_USER_NOT_ALLOWED_TO_EDIT se l'utente non e' collaboratore/creatore del documento
     *         OP_SECTION_ALREADY_IN_EDITING_MODE se un altro utente sta gia' editando la sezione
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste
     *         OP_SECTION_NOT_EXIST se la sezione non esiste
     */
    public FunctionOutcome editTask(String document, int numSection){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di fine editing (salvataggio modifiche fatte sezione che utente
     * ha richiesto di editare precedentemenete, se lo ha richiesto) della sezione del documento passati come argomento
     * @param document documento di cui la sezione in cui salvare le modifiche
     * @param numSection sezione in cui salvare le modifiche
     * @return OP_OK se la modifica della sezione del documento ha avuto successo
     *         OP_USER_NOT_ALLOWED_TO_EDIT se l'utente non e' collaboratore/creatore del documento
     *         OP_SECTION_NOT_IN_EDITING_MODE se la sezione non era in modalita' editing
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_USER_NOT_REGISTERED se l'utente che richiede operazione non e' registrato
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste
     *         OP_SECTION_NOT_EXIST se la sezione non esiste
     */
    public FunctionOutcome endEditTask(String document, int numSection){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    public FunctionOutcome sendTask(String message){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    public FunctionOutcome receiveTask(){
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }
}
