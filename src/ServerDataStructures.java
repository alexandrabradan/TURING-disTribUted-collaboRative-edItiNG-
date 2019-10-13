import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerDataStructures {

    //isieme degli utenti online
    private ConcurrentHashMap<SocketChannel, String> online_users;
    //insieme degli indirizzi di multicast
    private BlockingQueue<InetAddress> multicast_set;
    //Tabella Hash che contiene le coppie: <nome_utente, utente>
    private ConcurrentHashMap<String, User> hash_users;
    //Tabella Hash che contiene le coppie: <nome_documento, documento>
    private ConcurrentHashMap<String, Document> hash_documents;
    // Oggetto utilizzato per gestire la sincronizzazione tra hash_users e hash_docs
    private Object lockHash;
    //insieme che contiene i channels da riregistrare al selettore dopo che un worker ha soddisfatto una richiesta
    private BlockingQueue<SocketChannel> selectorKeysToReinsert;

    public ServerDataStructures(){
        //*************************************ALLOCAZIONE STRUTTURE DATI*********************************************//
        this.online_users = new ConcurrentHashMap<>();
        this.multicast_set = new LinkedBlockingQueue<>();
        this.hash_users = new ConcurrentHashMap<>();
        this.hash_documents = new ConcurrentHashMap<>();
        this.selectorKeysToReinsert = new LinkedBlockingQueue<>();

        //*****************************INIZIALIZZAZIONE MUTUA ESCLUSIONE ESPLICITA************************************//
        //inizializziamo gli oggetti utilizzati per la sincronizzazione
        this.lockHash = new Object();
    }

    //***********************************************METODI GETTER****************************************************//

    /**
     * Funzione che restituisce l'insieme degli utenti online/connessi
     * @return this.onlineUsers
     */
    public ConcurrentHashMap<SocketChannel, String> getOnline_users(){
        return this.online_users;
    }

    /**
     * Funzione che restituisce l'insieme degli indirizzi da utilizzare per le Chat in multicast, quando si apre un
     * documento per editare
     * @return this.multicast_set
     */
    public BlockingQueue<InetAddress> getMlticast_set(){
        return this.multicast_set;
    }

    /**
     * Funzione che restituisce il database degli utenti registrati al servizio
     * @return this.hash_users
     */
    public ConcurrentHashMap<String, User> getHash_users(){
        return this.hash_users;
    }

    /**
     * Funzione che restituisce il database dei documenti memorizzati dal servizio
     * @return this.hash_documents
     */
    public ConcurrentHashMap<String, Document> getHash_documents(){
        return hash_documents;
    }

    /**
     * Funzione che restituisce la lock utilizzata per mantenere consistenti il database degli utenti registrati
     * e il database dei documenti
     * @return this.lockHash
     */
    public Object getLockHash(){
        return this.lockHash;
    }

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
     */
    public void putToOnlineUsers(SocketChannel client, String username) {
        //aggiungo SocketChannel dell'utente e l'utente nella ht degli utenti online
        online_users.put(client, username);
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

    //**********************METODI PER GESTIRE INSIEME INDIRIZZI DI MULTICAST***************************************//

    /**
     * Funzione che preleva il primo indirizzo di multicast disponibile per poterlo assegnare ad un documento
     * appena creato
     * @return indirizzo di multicast da assegnare alla chat di un nuovo documento
     *         null se subentra qualche errore
     */
    public InetAddress getMulticastAddress(){
        try {
            return this.multicast_set.take(); //prelevo primo indirizzo di multicast disponibile
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Funzione che controlla se l'indirizzo e' presente nell'insieme degli indirizzi di multicast o meno
     * @param address indirizzo di cui bisogna verificare presenza nell'insieme degli indirizzi di multicast
     * @return true se l'indirizzo è presente nell'insieme di multicast
     *  	   false altrimenti
     */
    public boolean checkPresenceInMulticastAddress(InetAddress address) {
        return this.multicast_set.contains(address);
    }

    /**
     * Funzione che aggiunge un indirizzo all'insieme degli indirizzi di multicast
     * @param address indirizzo da aggiungere
     */
    public void addToMulticastAddress(InetAddress address) {
        this.multicast_set.add(address);
    }

    /**
     * Funzione che elimina indirizzo passato come argomento dall'insieme degli indirizzi di multicast
     * @param address indirizzo da eliminare
     */
    public void removeFromMulticastAddress(InetAddress address){this.multicast_set.remove(address);}

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
     */
    public void insertHashUser(String username, User usr) {
        this.hash_users.put(username, usr);
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
     */
    public Document getDocumentFromHash(String document) {
        return hash_documents.get(document);
    }

    /**
     * Funzione che inserisce un nuovo documento nella Tabella Hash dei documenti
     * @param document nome documento (CHIAVE)
     * @param doc istanza del docuemnto (VALORE)
     */
    public void insertHashDocument(String document, Document doc) {
        this.hash_documents.put(document, doc);
    }

    //**************************METODI PER GESTIRE INSIEME SOCKETS DA REINSERIRE NEL SELECTOR************************//

    /**
     * Funzione che inserisce un SocketChannel nell'insieme dei sockets da reiserire nel selettore
     * @param client socketchannel dell'utente da reiserire
     */
    public void addSelectorKeysToReinsert(SocketChannel client){
        this.selectorKeysToReinsert.add(client);
    }

    /**
     * Funzione che restituisce tutti i socketchanels da reinserire nel selettore
     * @return la lista dei socketchannels da reinserire nel selettore
     */
    public SocketChannel[] getAllSocketsInSelectorKeysToReinsert(){
        return this.selectorKeysToReinsert.toArray(new SocketChannel[0]);
    }

    /**
     * Funzione che rimuove il SocketChannel passato come argomento dall'insieme dei sockets da reiserire nel selettore
     * @param client socketchannel dell'utente da rimuovere
     */
    public void removeSelectorKeysToReinsert(SocketChannel client){
        this.selectorKeysToReinsert.remove(client);
    }
}
