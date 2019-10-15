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
     *  insieme dei documenti a cui l'utente è stato invitato a modificare, mentre era online
     */
    private Set<String> set_liveDocs;

    /**
     * Costruttore della classe User
     * @param username nome (univoco) dell'utente
     * @param password password associata all'utente
     */
    public User(String username, String password) {
        this.set_docs = new LinkedHashSet<>();
        this.set_pendingDocs = new LinkedHashSet<>();
        this.set_liveDocs = new LinkedHashSet<>();

        this.username = username;
        this.password = password;
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

    /**
     * Funzione che restituisce l'insieme dei documenti a cui l'utente è stato invitato a collaborare mentre era online
     * @return this.set_liveDocs
     */
    public synchronized Set<String> getLiveDocs() {
        return this.set_liveDocs;
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
     * Funzione che aggiunge un documento all'insieme dei documenti che l'utente può modificare (l'invito soppraggiunge
     * mentre utente e' online)
     * @param document documento da aggiungere
     */
    public synchronized void addSetLiveDocs(String document) {
        this.set_liveDocs.add(document);
    }

    /**
     * Funzione che controlla se il documento è modificabile dall'utente (appartiene all'insieme dei documenti modificabili
     * dall'utente perche' ne' collaboratore/craetore
     * @param document documento da controllare
     * @return true se il doucmento e' modificabile dall'utente
     *  	   false altrimenti
     */
    public synchronized boolean documentIsInSetDocs(String document) {
        return this.set_docs.contains(document);
    }

    /**
     * Funzione che si occupa di svuotare si occupa di svuotare l'insieme dei documenti per cui l'utente ha ricevuto un
     * invito di collaborazione mentre era offline
     */
    public synchronized void clearPendingDocs() {
        this.set_pendingDocs.clear();
    }

    /**
     * Funzione che si occupa di svuotare si occupa di svuotare l'insieme dei documenti per cui l'utente ha ricevuto un
     * invito di collaborazione mentre era online
     */
    public synchronized void clearLiveDocs() {
        this.set_liveDocs.clear();
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
        tmpString.append("| live_docs = |");

        for(String doc: this.set_liveDocs){
            tmpString.append(" ").append(doc);
        }
        tmpString.append("|");

        return stringToPrint + tmpString;
    }
}
