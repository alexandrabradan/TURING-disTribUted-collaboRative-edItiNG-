import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerDataStructures {

    /**
     * Tabella Hash che contiene degli utenti online
     */
    private ConcurrentHashMap<SocketChannel, String> online_users;
    /**
     * Tabella Hash che contiene le coppie: <nome_utente, utente>
     */
    private ConcurrentHashMap<String, User> hash_users;
    /**
     * Tabella Hash che contiene le coppie: <nome_documento, documento>
     */
    private ConcurrentHashMap<String, Document> hash_documents;
    /**
     * Tabella Hash che contiene le coppie: <indirizzo_multicast, documento>
     */
    private ConcurrentHashMap<String, String> hash_multicast;
    /**
     * Tabella Hash che contiene le coppie: <clientSocketName, clientSocketChannel>
     */
    private ConcurrentHashMap<String, SocketChannel> hash_socket_names;
    /**
     * Tabella Hash che contiene le coppie: <clientSocketChannel, invitesSocket>
     */
    private ConcurrentHashMap<SocketChannel, SocketChannel> hash_invites;

    /**
     * insieme che contiene i channels da riregistrare al selettore dopo che un worker ha soddisfatto una richiesta
     */
    private BlockingQueue<SocketChannel> selectorKeysToReinsert;


    public ServerDataStructures(){
        //*************************************ALLOCAZIONE STRUTTURE DATI*********************************************//
        this.online_users = new ConcurrentHashMap<>();
        this.hash_multicast = new ConcurrentHashMap<>();
        this.hash_users = new ConcurrentHashMap<>();
        this.hash_documents = new ConcurrentHashMap<>();
        this.hash_socket_names = new ConcurrentHashMap<>();
        this.hash_invites = new ConcurrentHashMap<>();
        this.selectorKeysToReinsert = new LinkedBlockingQueue<>();
    }

    //***********************************************METODI GETTER****************************************************//

    /**
     * Funzione che restituisce l'insieme dei canali da reinserire nel selettore dopo che vi sono stati tolti, per
     * soddisfare le loro richieste
     * @return this.selectorKeysToReinsert
     */
    public BlockingQueue<SocketChannel> getSelectorKeysToReinsert(){
        return this.selectorKeysToReinsert;
    }

    //********************************METODI PER GESTIRE INSIEME UTENTI ONLINE***************************************//

    /**
     * Funzione che restituisce la chiave associata al valore passato come argomento (se esiste)
     * @param map mappa nella quale cercare la chiave associata al valore
     * @param value valore di cui cercare la chiave
     * @return chiave associata al valore passato come argomento
     *          null se valore non esiste
     */
    private  <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Funzione che ricerca all'interno della HashTable la chiave associata al valore passato come argomento
     * @param username valore di cui ricercare la chiave
     * @return chiave associata al valore passato come argomento
     *          null se valore non esiste
     */
    public SocketChannel getSocketChannelFromUsername(String username){
        return getKey(this.online_users, username);
    }

    /**
     * Funzione che restituisce l'username/valore associato al SocketChannel/chiave il cui scopo e' verificare se
     * il SocketChannel passato come argomento e' associato ad un utente online o meno
     * @param client SocketChannel di cui verificare la connessione
     * @return username associato al SocketChannel, se il SocketChannel esiste nella ht e rapprenseta un utente onlie
     *         null se il SocketChannel e' presente nella ht (non rappresenta nessun utente online)
     */
    public String checkIfSocketChannelIsOnline(SocketChannel client){
        //verifico se Client e' connesso, provando a reperire il valore associato ad esso
        return online_users.get(client);
    }

    /**
     * Funzione che controlla se l'utente e' online o meno (e' connesso) con qualche Client
     * @param username username dell'utente da controllare se e' online o meno
     * @return true se l'utente e' connesso
     *   	   false altrimenti
     */
    public boolean checkIfUserIsOnline(String username) {
       //verifico che esiste qualche chiave/SocketChannel che ha come valore l'username di cui
        //voglio verificare connessione o meno (N.B. utente si puo' connettere con Clients diversi al servizio)
        return this.online_users.containsValue(username);
    }

    /**
     * Funzione che connette un utente (lo inserisce negli utenti online)
     * @param client socketchannel dell'utente di cui bisogna cambiare lo stato
     * @param username utente di cui cambiare lo stato
     * @return  SUCCESS se e' stato possibile connettere l'utente
     *         FAILURE se l'utente era gia' connesso
     */
    public FunctionOutcome putToOnlineUsers(SocketChannel client, String username) {
        //aggiungo SocketChannel dell'utente e l'utente nella ht degli utenti online
        String usernameToCheck = online_users.put(client, username);

        if(usernameToCheck == null)
            return FunctionOutcome.SUCCESS; //utente connesso (non era presente)
        else return FunctionOutcome.FAILURE; //utente gia' connesso
    }

    /**
     * Funzione che disconnette un utente (lo rimuove dagli utenti online)
     * @param client  socketchannel dell'utente di cui bisogna cambiare lo stato
     * @return username associato al SocketChannel, se il SocketChannel esiste nella ht
     *         null altrimenti
     */
    public String removeFromOnlineUsers(SocketChannel client) {
        //rimuovo SocketChannel dell'utente e l'utente dalla  dalla ht utenti online
        return online_users.remove(client);
    }

    public void printOnlineUsers(){
        System.out.println("STAMPA UTENTI ONLINE");

        for (Map.Entry<SocketChannel, String> entry : this.online_users.entrySet()) {
            SocketChannel key = entry.getKey();
            String value = entry.getValue();

            System.out.print ("socket: " + key + " username: " + value + " ");
        }
        System.out.println();
    }

    //**********************METODI PER GESTIRE INSIEME INDIRIZZI DI MULTICAST***************************************//

    /**
     * Funzione che controlla se l'indirizzo e' presente nella ht degli indirizzi di multicast o meno
     * @param ind indirizzo di cui bisogna verificare presenza nell'insieme degli indirizzi di multicast
     * @return true se l'indirizzo è presente nell'insieme di multicast
     *  	   false altrimenti
     */
    public boolean checkPresenceInMulticastAddress(String ind) {
        return this.hash_multicast.contains(ind);
    }

    /**
     * Funzione che aggiunge un indirizzo all'insieme degli indirizzi di multicast
     * @param ind indirizzo da aggiungere
     * @param  document nome del documento relativo all'indirizzo assegnato
     */
    public void addToMulticastAddress(String ind, String document) {
        this.hash_multicast.put(ind, document);
    }

    /**
     * Funzione che elimina indirizzo passato come argomento dall'insieme degli indirizzi di multicast
     * @param ind indirizzo da eliminare
     * @return chiave eliminata
     */
    public String removeFromMulticastAddress(String ind){return this.hash_multicast.remove(ind);}

    //**********************METODI PER ABILITARE UTENTE ALLA MODIFICA IN MUTUA ESCLUSIONE****************************//

    /**
     * 1. inserisco documento nell'insieme dei documenti che utente puo' modificare
     * 2. inserisco utente nell'insieme dei modificatori del documento (se non e' creatore)
     * Questi step vengono fatti in MUTUA ESCLUSIONE per garantire CONSITENZA tra ht utenti e ht documenti
     * @param username nome dell'utente
     * @param document nome del documento
     * @param isCreator flag che specifica se l'utente e' creatore o meno del documento
     */
    public synchronized void validateUserAsModifier(String username, String document, boolean isCreator){
        User user = getUserFromHash(username);
        user.addSetDoc(document);

        if(!isCreator){
            Document doc = getDocumentFromHash(document);
            doc.addUser(username);
        }
    }

    /**
     * Funzione che permette di creare un nuovo documento in MUTUA ESCLUSIONE, grazie alle lock implicite della
     * ConcurrentHashMap dei documenti
     * @param username nome dell'utente che vuole creare nuovo documento
     * @param document nome del documento
     * @param numSections numero sezioni del documento
     * @return OP_OK se il documento e le relative sezioni sono state create con successo
     *         OP_DOCUMENT_ALREADY_EXIST se il nuovo documento che si vuole creare esiste gia'
     */
    public ServerResponse registerNewDocument(String username, String document, int numSections){
        //verifico se documento e' gia' esistente (controllo presenza documento all'interno della ht dei documenti)
        boolean exist = checkIfDocumentExist(document);

        if(exist)
            return ServerResponse.OP_DOCUMENT_ALREADY_EXIST; //documento gia' esistente

        //documento non esiste di gia'
        //ricavo InetAddress da associare alla chat del documento
        String chatInd = new MulticastAddressRandomGenerator(this).getRandomAddress();

        //aggiungo indirizzo all'insieme degli indirizzi assegnati
        addToMulticastAddress(chatInd, document);

        //verifico che indirizzo di multicast non sia null (spazio degli indirizzi di multicast esaurito)
        if(chatInd.isEmpty())
           return ServerResponse.OP_DOCUMENT_MULTICAST_ADDRESS_RUN_OUT;

        //documento non esiste => creo nuova istanza di Document
        Document doc = new Document(document, username, numSections, chatInd);

        //inserisco istanza del documento nella HashTable dei documenti
        FunctionOutcome check = insertHashDocument(document, doc);

        if(check == FunctionOutcome.FAILURE)
            return ServerResponse.OP_DOCUMENT_ALREADY_EXIST; //documento gia' esistente

        //inserisco documento nell'insieme dei documenti che utente puo' modificare
        validateUserAsModifier(username, document, true);

        return ServerResponse.OP_OK;
    }

    /**
     * Funzione che garantisce la mutua esclusione dell'inserimento di un username all'interno della ht_users, grazie
     * al fatto che questa e' una ConcurrentHashMap. Questoe' necessario perche' l'accesso da remoto tramite RMI al
     * metodo di registrazione da parte di un utente, creare lato Server un thread. Se piu' utenti si registrano
     * nello stesso momento e' necessario sincronizzare le registrazioni per avere l'univocacita' dei nomi
     * @param username nome utente
     * @param password password al servizio
     * @return SUCCESS se e' stato possibile registrare l'utente
     *         FAILURE altrimenti
     */
    public  ServerResponse registerUser(String username, String password){
        //utente e' disconesso => verifico se username e' gia' stato preso da qualche altro utente
        boolean alreadyTaken = checkIfUserIsRegister(username);
        if(alreadyTaken)
            return ServerResponse.OP_USERNAME_ALREADY_TAKEN; //username gia' in uso

        //username non e' stato preso
        //creo istanza dell'utente da registrare e da inserire nella HashTable
        User newUser = new User(username, password);
        FunctionOutcome check = insertHashUser(username, newUser);

        if(check == FunctionOutcome.FAILURE)
            return ServerResponse.OP_USERNAME_ALREADY_TAKEN; //username gia' in uso

        return ServerResponse.OP_OK;
    }

    //***********************************METODI PER GESTIRE TABELLA HASH UTENTI*************************************//

    /**
     * Funzione che controlla se l'utente e' registarto o meno (controlla se l'utente e' presente nella Tabella
     * Hash degli utenti
     * @param username utente di cui bisogna controllare registrazione
     * @return true se l'utente è registrato
     *  	   false altrimenti
     */
    public boolean checkIfUserIsRegister(String username) {
        return hash_users.containsKey(username);
    }

    /**
     * Funzione che restituisce l'istanza della classe corrispondete all'utente passato come argomento
     * @param username utente di cui bisogna restituire istanza
     * @return User istanza dell'utente corrispondente
     */
    public User getUserFromHash(String username) {
        return hash_users.get(username);
    }

    /**
     * Funzione che inserisce un nuovo utente nella Tabella Hash degli utente
     * @param username nome utente (CHIAVE)
     * @param usr istanza dell'utente (VALORE)
     * @return SUCCESS se l'inserimento ha avuto successo
     *         FAILURE se username era gia' in uso
     */
    public FunctionOutcome insertHashUser(String username, User usr) {

        User userToCheck = this.hash_users.put(username, usr);
        if(userToCheck == null)
            return FunctionOutcome.SUCCESS; //username inserito con successo (non era presente)
        else return FunctionOutcome.FAILURE; //username gia' presente
    }

    /**
     * Funzione che stampa la tabella hash degli utenti
     */
    public void printHashUser(){
        System.out.println("STAMAPA TABELLA HASH UTENTI");
        this.hash_users.forEach((key, value) -> System.out.println(key + " " + value.printUser()));
    }

    //***********************************METODI PER GESTIRE TABELLA HASH DOCUMENTI***********************************//

    /**
     * Funzione che controlla esistenza del documento o meno (controlla se il documenti e' presente nella Tabella
     * Hash dei documenti
     * @param document documento di cui bisogna controllare creazione
     * @return true se il documento esiste
     *  	   false altrimenti
     */
    public boolean checkIfDocumentExist(String document) {
        return hash_documents.containsKey(document);
    }

    /**
     * Funzione che restituisce l'istanza della classe corrispondete al docuemnto passato come argomento
     * @param document documento di cui bisogna restituire istanza
     * @return Document istanza del docuemnto corrispondente
     *         null se documento non esiste
     */
    public Document getDocumentFromHash(String document) {
        return hash_documents.get(document);
    }

    /**
     * Funzione che inserisce un nuovo documento nella Tabella Hash dei documenti
     * @param document nome documento (CHIAVE)
     * @param doc istanza del docuemnto (VALORE)
     * @return  SUCCESS se inserimento e' andato a buon fine
     *          FAILURE  se documento esisteva gia'
     */
    public FunctionOutcome insertHashDocument(String document, Document doc) {

        Document valueToCheck = this.hash_documents.put(document, doc);
        if(valueToCheck == null)
            return FunctionOutcome.SUCCESS; //documento inserito (non esisteva)
        else return FunctionOutcome.FAILURE; //documento non inserito (esisteva gia')
    }

    /**
     * Funzione che stampa la tabella hash dei documenti
     */
    public void printHashDoc(){
        System.out.println("STAMAPA TABELLA HASH DOCUMENTI");
        this.hash_documents.forEach((key, value) -> System.out.println(key + " " + value.printDoc()));
    }

    //************************METODI PER GESTIRE TABELLA HASH DEGLI INVITI********************************//

    /**
     * Funzione che restituisce l'invitesSocket corrispondete al clientSocket passato come argomento
     * @param clientSocket SocketChannel di cui reperire porta assegnata
     * @return porta corrispondente
     *         null se clientSocket non esiste
     */
    public SocketChannel searchHashInvites(SocketChannel clientSocket) {
        return hash_invites.get(clientSocket);
    }

    /**
     * Funzione che inserisce una nuova associazioen tra clientSocket e l'invitesSocket nella Tabella Hash degli inviti
     * @param clientSocket (CHIAVE)
     * @param invitesSocket (VALORE)
     */
    public void insertHashInvites(SocketChannel clientSocket, SocketChannel invitesSocket) {
        this.hash_invites.put(clientSocket, invitesSocket);
    }

    /**
     * Funzione che elimina clientSocket e sua asscoiazione dalla ht degli inviti
     * @param clientSocket SocketChannel da eliminare
     * @return invitesSocket corrispondente
     *         null se clientSocket non esiste
     */
    public SocketChannel removeHashInvites(SocketChannel clientSocket) {
        return hash_invites.remove(clientSocket);
    }

    //************************METODI PER GESTIRE TABELLA HASH DEI NOMI DEI SOCKETS********************************//

    /**
     * Funzione che restituisce il SocketChannel relativo al corrispondente nome del Socket di tale SocketChannel
     * @param socketName nome del Socket di cui reperire SocketChannel
     * @return SocketChannel corrispondente
     *         null se socketName non esiste
     */
    public SocketChannel searchHashSocketNames(String socketName) {
        return hash_socket_names.get(socketName);
    }

    /**
     * Funzione che inserisce una nuova associazioen tra socketName e il relativo clientSocketChannel nella
     * Tabella Hash dei nomi dei sockets
     * @param socketName (CHIAVE)
     * @param clientSocketChannel (VALORE)
     */
    public void insertHashSocketNames(String socketName, SocketChannel clientSocketChannel) {
        this.hash_socket_names.put(socketName, clientSocketChannel);
    }

    /**
     * Funzione che elimina nameSocket e sua asscoiazione dalla ht dei nomi dei sockets
     * @param socketName  da eliminare
     * @return clientSocketChannel corrispondente
     *         null se socketName non esiste
     */
    public SocketChannel removeHashSocketNames(String socketName) {
        return hash_socket_names.remove(socketName);
    }

    /**
     * Funzione che stampa la tabella hash dei nomi dei sockets
     */
    public void printSocketNames(){
        System.out.println("STAMAPA TABELLA HASH NOMI SOCKETS");
        this.hash_socket_names.forEach((key, value) -> System.out.println(key + " " + value));
    }


    //**************************METODI PER GESTIRE INSIEME SOCKETS DA REINSERIRE NEL SELECTOR************************//

    /**
     * Funzione che inserisce un SocketChannel nell'insieme dei sockets da reiserire nel selettore
     * @param client socketchannel dell'utente da reiserire
     */
    public void addSelectorKeysToReinsert(SocketChannel client){
        this.selectorKeysToReinsert.add(client);
    }
}
