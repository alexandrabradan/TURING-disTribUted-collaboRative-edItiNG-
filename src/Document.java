import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;

public class Document {

    /**
     * nome del documento
     */
    private String document;
    /**
     * nome del creatore del documento
     */
    private String creator;

    /**
     * insieme degli utenti che possono modificare il documento (collaboratori/creatori)
     */
    private LinkedHashSet<String> modifiers;
    /**
     * array di locks per ottenere mutua esclusione accesso sezioni documento
     */
    private ReentrantLock[] sectionsLockArray;
    /**
     *  indirizzo statico di multicast associato per la chat per questo documento
     */
    private InetAddress chatAddress;

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

        this.modifiers = new LinkedHashSet<>();

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
    public synchronized String getCreatorName() {
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
    public synchronized LinkedHashSet<String> getModifiers(){
        return this.modifiers;
    }

    /**
     * Funzione che verifica se l'utente passato come argomento figura tra gli collaboratori del documento
     * (perche' vi e' creatore/collaboratore) o meno
     * @param userToCheck utente da verificare
     * @return true se l'utente e' collaboratore del documento
     *         false false
     */
    public synchronized boolean checkIfUserIsModifier(String userToCheck){
        return this.modifiers.contains(userToCheck);
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
     * @return true ssezione di cui acquisire la lock
     */
    public synchronized void lockSection(int section) {
        sectionsLockArray[section].lock();
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
    public synchronized void unlockSection(int section) {
        sectionsLockArray[section].unlock();
    }

    /**
     * Funzione restituisce le informazioni sulle variabili della classse sottoforma di stringa
     * @return una stringa che contiene le informazioni sulle variabili dell'oggetto corrente
     */
    public synchronized String printDoc(){
        String stringToPrint = String.format("Document = %s  Creator = %s chatAddress = %s modifiers = |",
                this.document, this.creator, this.chatAddress);

        StringBuilder tmpString = new StringBuilder();
        for(String modifier: modifiers){
            tmpString.append(" ").append(modifier);
        }
        tmpString.append("|");

        return stringToPrint + tmpString;
    }
}
