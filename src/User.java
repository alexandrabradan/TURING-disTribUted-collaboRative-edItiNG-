import java.util.LinkedHashSet;
import java.util.Set;

public class User {
    //insieme dei documenti che utente puo' modificare (perche' ne e' collaboratore/creatore)
    private Set<String> set_doc;
    //insieme dei documenti a cui l'utente è stato invitato a modificare, mentre era offline
    private Set<String> set_pendingDocs;
    // Insieme dei documenti a cui l'utente è stato invitato a modificare, mentre era online
    private Set<String> set_liveDocs;

    private String username; //univoco
    private String password;

    /**
     * Costruttore della classe User
     * @param username nome (univoco) dell'utente
     * @param password password associata all'utente
     */
    public User(String username, String password) {
        this.set_doc = new LinkedHashSet<String>();
        this.set_pendingDocs = new LinkedHashSet<String>();
        this.set_liveDocs = new LinkedHashSet<String>();

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
     * (la password inserita deve essere equivalente a quella asssociata all'utente nella ht_users)
     * @param password passowrd inserita
     * @return true se le password corrispondono
     *  		false altrimenti
     */
    public synchronized boolean equalsPassword(String password) {
        return this.password.equals(password);
    }


    /**
     * Funzione che restituisce l'insieme dei documenti modificabili dall'utente (perche' ne e' collaboartore/creatore)
     * @return this.set_doc
     */
    public synchronized Set<String> getSetDoc() {
        return this.set_doc;
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

    /**
     * Funzione che restituisce l'insieme dei documenti modificabili dall'utente (perche' ne e' collaboartore/creatore),
     * sottoforma di array
     * @return this.set_doc.toArray()
     */
    public synchronized Object[] getSetDocsToArray() {
        return this.set_doc.toArray();
    }

    /** Funzione che aggiunge un documento all'insieme dei documenti che l'utente può editare
     * @param document documento da aggiungere
     */
    public synchronized void addSetDoc(String document) {
        this.set_doc.add(document);
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
        return this.set_doc.contains(document);
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
}