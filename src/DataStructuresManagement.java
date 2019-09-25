import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataStructuresManagement {

    //strutture dati e mutua esclusione esplicita sono PUBBLICHE per renderle accessibili dalla classe ShutDownHook
    // e Worker

    //isieme degli utenti online
    public Set<String> onlineUsers;
    //insieme degli utenti offline
    public Set<String> offlineUsers;
    //insieme degli indirizzi di multicast
    public Set<InetAddress> multicast_set;
    //Tabella Hash che contiene le coppie: <nome_utente, utente>
    public ConcurrentHashMap<String, User> hash_users;
    //Tabella Hash che contiene le coppie: <nome_documento, documento>
    public ConcurrentHashMap<String, Document> hash_documents;
    // Oggetto utilizzato per gestire la sincronizzazione tra hash_users e hash_docs
    public Object lockHash;
    // Oggetto utilizzato per gestire la sincronizzazione tra onlineUsers ed offlineUsers
    public Object lockUser;

    public DataStructuresManagement(){
        //*************************************ALLOCAZIONE STRUTTURE DATI*********************************************//
        this.offlineUsers=new LinkedHashSet<String>();
        this.offlineUsers=new LinkedHashSet<String>();
        this.multicast_set=new LinkedHashSet<InetAddress>();
        this.hash_users=new ConcurrentHashMap<String,User>();
        this.hash_documents=new ConcurrentHashMap<String,Document>();

        //*****************************INIZIALIZZAZIONE MUTUA ESCLUSIONE ESPLICITA************************************//
        //inizializziamo gli oggetti utilizzati per la sincronizzazione
        this.lockHash = new Object();
        this.lockUser = new Object();
    }

    /**
     * Funzione che controlla se l'utente e' online o meno (e' connesso)
     * @param username username dell'utente da controllare se e' online o meno
     * @return true se l'utente e' connesso
     *   	   false altrimenti
     */
    public boolean checkIfUserIsOnline(String username) {
        return onlineUsers.contains(username);
    }

    /**
     * Funzione che controlla se l'utente e' offline o meno (e' disconnesso)
     * @param username username dell'utente da controllare se e' offline o meno
     * @return true se l'utente e' disconesso
     *   	   false altrimenti
     */
    public boolean checkIfUserIsOffline(String username) {
        return offlineUsers.contains(username);
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
     * Funzione che connette un utente (lo rimuove dagli utenti offline e lo aggiunge agli online)
     * @param username utente di cui cambiare lo stato
     */
    public void offlineToOnline(String username) {
        //rimuovo utente dall'insieme utenti offline
        offlineUsers.remove(username);
        //aggiungo utente all'insieme utenti online
        onlineUsers.add(username);
    }

    /**
     * Funzione che disconnette un utente (lo rimuove dagli utenti online e lo aggiunge agli offline)
     * @param username utente di cui cambiare lo stato
     */
    public void onlineToOffline(String username) {
        //rimuovo utente dall'insieme utenti online
        onlineUsers.remove(username);
        //aggiungo utente all'insieme utenti offline
        offlineUsers.add(username);
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
     * Funzione che restituisce l'istanza della classe corrispondete al docuemnto passato come argomento
     * @param document documento di cui bisogna restituire istanza
     * @return Document istanza del docuemnto corrispondente
     */
    public Document getDocumentFromHash(String document) {
        return hash_documents.get(document);
    }

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
     * Funzione che inserisce un nuovo utente nella Tabella Hash degli utente
     * @param username nome utente (CHIAVE)
     * @param usr istanza dell'utente (VALORE)
     */
    public void insertHashUser(String username, User usr) {
        this.hash_users.put(username, usr);
    }

    /**
     * Funzione che inserisce un nuovo documento nella Tabella Hash dei documenti
     * @param document nome documento (CHIAVE)
     * @param doc istanza del docuemnto (VALORE)
     */
    public void insertHashDocument(String document, Document doc) {
        this.hash_documents.put(document, doc);
    }

    //TO DO : implementare funzionalita' di deregistrazione + cancellazione documenti??
}
