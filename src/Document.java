import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;

public class Document {

    private String document; //nome del documento
    private String creator; //nome del creatore del documento

    private Set<String> modifiers; //insieme degli utenti che possono modificare il documento (collaboratori/creatori)
    private ReentrantLock[] sectionsLockArray; //Array di lock associate al documento (una per ogni sezione)
    private InetAddress chatAddress;  // indirizzo statico di multicast associato per la chat per questo documento

    /**
     * Costruttore della classe
     * @param document nome del documento
     * @param creator  nome del creatore del documento
     * @param numSections numero sezioni del documento
     * @param chatAddress indirizzo di multicast per la chat
     */
    public Document(String document, String creator, int numSections, InetAddress chatAddress) {
        this.document = document;
        this.creator = creator;

        this.modifiers = new LinkedHashSet<String>();

        //inizializzo l'array di locks
        this.sectionsLockArray = new ReentrantLock[numSections];
        for(int k = 0; k < numSections; k++) {
            sectionsLockArray[k] = new ReentrantLock(true);
        }

        this.chatAddress = chatAddress;
    }

    /**
     * Funzione che restituisce il nome del documento
     * @return this.documento
     */
    public synchronized String getDocumentName() {
        return this.document;
    }

    /**
     * Funzione che restituisce l'utente creatore del documento
     * @return this.creator
     */
    public synchronized String getUserName() {
        return this.creator;
    }

    /**
     * Funzione che controlla se l'utente passato come argomento è il creatore del documento o meno
     * @param username nome dell'utente
     * @return true se l'utente e' il creatore del documento
     *         false altrimenti
     */
    public synchronized boolean isCreator(String username) {
        return this.creator.equals(username);
    }

    /**
     * Funzione che restituisce l'indirizzo di multicast della chat associata al documento
     * @return this.chatAddress indirizzo di multicast
     */
    public synchronized InetAddress getChatAddress() {
        return this.chatAddress;
    }

    /**
     * Funzione che restituisce il numero di sezioni del documento
     * @return sectionsLockArray.length
     */
    public synchronized int getNumberSections() {
        return sectionsLockArray.length;
    }

    /**
     * Funzione che restituisce l'insieme degli utenti che hanno il permesso di editare il documento, perche' ne sono
     * collaboratori oppure creatori
     * @return this.modifiers
     */
    public synchronized Set<String> getModifiers(){
        return this.modifiers;
    }

    /**
     * Funzione che aggiunge un utente all'insieme degli utenti che possono modificare il documento
     * @param username nome dell'utente da aggiungere
     */
    public synchronized void addUser(String username) {
        this.modifiers.add(username);
    }

    /**
     * Funzione che prova a richiedere la lock su una sezione
     * @param section sezione di cui acquisire la mutua esclusione
     * @return true se è stato possibile richiedere la lock della sezione
     *  	   false altrimenti
     */
    public synchronized boolean lockSection(int section) {
        return sectionsLockArray[section].tryLock();
    }

    /**
     * Funzione che controlla se una determinata sezione è bloccata o meno
     * @param section sezione da controllare
     * @return true se la sezione è bloccata
     *  	   false altrimenti
     */
    public synchronized boolean checkIfSectionIsLocked(int section) {
        return sectionsLockArray[section].isLocked();
    }

    /**
     * Funzione che rilascia la lock di una sezione
     * @param section sezione di cui rilasciare la lock
     */
    public synchronized void unlockSezione(int section) {
        sectionsLockArray[section].unlock();
    }
}
