import java.util.LinkedHashSet;
import java.util.Set;

public class User {

    /**
     * nickname dell'utente(univoco)
     */
    private String username;
    /**
     * password dell'utente
     */
    private String password;
    /**
     * insieme dei documenti che utente puo' modificare (perche' ne e' collaboratore/creatore)
     */
    private Set<String> set_docs;
    /**
     * insieme dei documenti a cui l'utente è stato invitato a modificare, mentre era offline
     */
    private Set<String> set_pendingDocs;
    /**
     * coppia (nome documento, sezione) che eventualmente l'utente sta editando
     */
    private Object[] documentAndSectionEditetd;
    /**
     * Ogetto per utilizzato per reperire la mutua esclusione sull'invitesSocketChannel dedicato
     * all'ascolto degli inviti del Client
     */
    private Object lockInvitesSocket;

    /**
     * Costruttore della classe User
     * @param username nome (univoco) dell'utente
     * @param password password associata all'utente
     */
    public User(String username, String password) {
        this.set_docs = new LinkedHashSet<>();
        this.set_pendingDocs = new LinkedHashSet<>();
        this.documentAndSectionEditetd =  new Object[2]; //deve contente solo (chiave, valore)
        this.documentAndSectionEditetd[0] = "";
        this.documentAndSectionEditetd[1] = -1;

        this.username = username;
        this.password = password;

        this.lockInvitesSocket = new Object();
    }

    /**
     * Funzione per reperire l'oggetto che da la mutua esclusione sulla scrittura
     * dell'invitesSocket del Client
     * @return this.lockInvitesSocket
     */
    public synchronized Object getLockInvitesSocket(){return this.lockInvitesSocket;}

    /**
     * Funzione per reperire l'eventuale documento e sezione editati dall'utente
     * @return this.documentAndSectionEditetd;
     */
    public synchronized Object[] getDocumentAndSectionEditetd(){return this.documentAndSectionEditetd;}

    /**
     * Funzione per settare quale documento e sezione l'utente ha editato, ha smesso di editare
     * @param document documento che utente ha editato / ha smesso di editare
     * @param section sezione che utente ha editato / ha smesso di editare
     */
    public synchronized void setDocumentAndSectionEditetd(String document, int section){
        this.documentAndSectionEditetd[0] = document;
        this.documentAndSectionEditetd[1] = section;
    }

    /**
     *Funzione che restituisce il nome dell'utente
     * @return this.username
     */
    public synchronized String getUsername() {
        return this.username;
    }

    /**
     * Funzione che restituisce la password associata all'utente
     * @return this.password
     */
    public synchronized String getPassword() {
        return this.password;
    }

    /**
     * Funzione che valida la password inserita dall'utente al momento della connessione al servizio
     * (la password inserita deve essere equivalente a quella asssociata all'utente nella ht_users, nel momento
     * di registrazione)
     * @param password passowrd inserita
     * @return true se le password corrispondono
     *  		false altrimenti
     */
    public synchronized boolean equalsPassword(String password) {
        return this.password.equals(password);
    }


    /**
     * Funzione che restituisce l'insieme dei documenti modificabili dall'utente (perche' ne e' collaboartore/creatore)
     * @return this.set_docs
     */
    public synchronized Set<String> getSetDocs() {
        return this.set_docs;
    }

    /**
     * Funzione che restituisce l'insieme dei documenti a cui l'utente e' stato invitato a collaborare mentre era offline
     * @return this.set_pendingDocs
     */
    public synchronized Set<String> getSetPendingDocs() {
        return this.set_pendingDocs;
    }

    /** Funzione che aggiunge un documento all'insieme dei documenti che l'utente può editare
     * @param document documento da aggiungere
     */
    public synchronized void addSetDoc(String document) {
        this.set_docs.add(document);
    }

    /**
     * Funzione che aggiunge un documento all'insieme dei documenti che l'utente può modificare (l'invito soppraggiunge
     * mentre utente e' offline)
     * @param document document da aggiungere
     */
    public synchronized void addSetPendingDocs(String document) {
        this.set_pendingDocs.add(document);
    }

    /**
     * Funzione che si occupa di eliminare un invito pendente (perche' inviato all'utente) dall'insieme
     * degli inviti pendenti
     * @param invite stringa che contiene invito pendente da cui estrappolare nome del documento
     */
    public synchronized void removePendingInvite(String invite){
        this.set_pendingDocs.remove(invite);
    }

    /**
     * Funzione restituisce le informazioni sulle variabili della classse sottoforma di stringa
     * @return una stringa che contiene le informazioni sulle variabili dell'oggetto corrente
     */
    public synchronized String printUser(){
        String stringToPrint = String.format("Username = %s  Password = %s  set_docs = |",
                this.username, this.password);

        StringBuilder tmpString = new StringBuilder();
        for(String doc: this.set_docs){
            tmpString.append(" ").append(doc);
        }
        tmpString.append("| pending_docs = |");

        for(String doc: this.set_pendingDocs){
            tmpString.append(" ").append(doc);
        }
        tmpString.append("|");

        return stringToPrint + tmpString;
    }
}
