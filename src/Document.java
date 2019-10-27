import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedHashSet;

public class Document {
    /**
     * costante che contiene riferimento alla stringa vuota
     */
    private static final String EMPTY_STRING = "";
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
     * array per ottenere mutua esclusione accesso sezioni documento => slot corrisponde all'username che ha
     * acquisito mutua esclusione sulla sezione, altrimenti "" se sezione e' libera
     */
    private String[] sectionsLockArray;
    /**
     *  indirizzo statico di multicast associato per la chat per questo documento
     */
    private String chatInd;

    /**
     * Ogetto per utilizzato per reperire la mutua esclusione sull'chatSocket del documento
     */
    private Object lockChatSocket;

    /**
     * Costruttore della classe
     * @param document nome del documento
     * @param creator  nome del creatore del documento
     * @param numSections numero sezioni del documento
     * @param chatInd indirizzo di multicast per la chat
     */
    public Document(String document, String creator, int numSections, String chatInd) {
        this.document = document;
        this.creator = creator;

        this.modifiers = new LinkedHashSet<>();

        //inizializzo l'array per la mutua esclsuione delle sezioni
        this.sectionsLockArray = new String[numSections];
        for(int k = 0; k < numSections; k++) {
            sectionsLockArray[k] = EMPTY_STRING;
        }

        this.chatInd = chatInd;
        this.lockChatSocket = new Object();
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
    public synchronized String getChatInd() {
        return this.chatInd;
    }

    /**
     * Funzione per reperire l'oggetto che da la mutua esclusione sulla scrittura dell'chatSocket del documento
     * @return this.lockInvitesSocket
     */
    public synchronized Object getLockChatSocket(){return this.lockChatSocket;}

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

    public synchronized String[] getSectionsLockArray(){return this.sectionsLockArray;}

    /**
     * Funzione che prova a richiedere la lock su una sezione
     * @param section sezione di cui acquisire la mutua esclusione
     * @param username utente che vuole acquisire  mutua esclusione sulla sezione
     * @return utente che ha in posseso la sezione (potrebbe essere l'utente che la desidera,
     *                 oppure un altro utente che l'aveva acquisita in precedenza)
     */
    public synchronized String lockSection(int section, String username) {
        int sectionInSectionsArray = section - 1; //ho numerato sezioni da 1
        if(checkIfSectionIsLocked(section).equals("")){
            this.sectionsLockArray[sectionInSectionsArray] = username; //acquisisco mutua esclsuione
            return username; //mutua esclusione acquisita
        }
        else return checkIfSectionIsLocked(section); //mutua esclusione gia' acquisita => ritorno utente che la possiede
    }

    /**
     * Funzione che controlla se una determinata sezione è bloccata o meno
     * @param section sezione da controllare
     * @return username dell'utente che ha acquisito la sezione
     *         "" altrimenti
     */
    public synchronized String checkIfSectionIsLocked(int section) {
        int sectionInSectionsArray = section - 1;
        if(sectionsLockArray[sectionInSectionsArray].isEmpty()){
            return "";
        }
        else {
            return sectionsLockArray[sectionInSectionsArray];
        }
    }

    /**
     * Funzione che rilascia la lock di una sezione
     * @param section sezione di cui rilasciare la lock
     * @param username utente che ha acquisito la sezione
     * @return SUCCESS se e' stato possibile rilasciare mutua esclusione
     *         FAILURE se non e' stato possibile rilasciare mutua esclusione (sezione non in editing mode o
     *         editata da qualcunaltro)
     */
    public synchronized ServerResponse unlockSection(int section, String username) {
        int sectionInSectionsArray = section - 1;
        if(checkIfSectionIsLocked(section).equals(username)){
            sectionsLockArray[sectionInSectionsArray] = EMPTY_STRING; //rilascio mutua esclusione
            return ServerResponse.OP_OK; //mutua esclusione rilasciata
        }
        else if(checkIfSectionIsLocked(section).equals(""))
            return ServerResponse.OP_SECTION_NOT_IN_EDITING_MODE; //sezione non in editing mode
        else return ServerResponse.OP_SECTION_EDITED_BY_SOMEONE_ELSE;  //SocketChannel non corrisponde con chi l'ha presa
    }

    /**
     * Funzione restituisce le informazioni sulle variabili della classse sottoforma di stringa
     * @return una stringa che contiene le informazioni sulle variabili dell'oggetto corrente
     */
    public synchronized String printDoc(){
        String stringToPrint = String.format("Document = %s  Creator = %s chatAddress = %s modifiers = |",
                this.document, this.creator, this.chatInd);

        StringBuilder tmpString = new StringBuilder();
        for(String modifier: modifiers){
            tmpString.append(" ").append(modifier);
        }
        tmpString.append("|");

        return stringToPrint + tmpString;
    }
}
