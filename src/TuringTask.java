import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashSet;
import java.util.Set;

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
     * Funzione privata chiamata da "sendPendingInvites" per reperire il nome del documento contenuto nella stringa-invito
     * presente all'interno dell'insieme degli inviti pendenti di un utente
     * @param invite incito-stringa da cui reperire il nome del documento
     * @return nome del documento
     *         null se subentra qualche errore
     */
    private String getDocumentFromInvite(String invite){
        char[] strIntoArray = invite.toCharArray();
        StringBuilder document = new StringBuilder();
        int pipeCount = 0; //contatore pipe contenute nella stringa
        int pipePosition = -1; //posizione pipe

        for(int i = 0; i < strIntoArray.length; i++){
            if(strIntoArray[i] == '|'){
                pipePosition = i;
                pipeCount = pipeCount + 1;
            }

            if(pipeCount == 3 && i > pipePosition){
                document.append(String.valueOf(strIntoArray[i]));
            }
        }
        return document.toString();
    }

    /**
     * Funzione privata che viene chiamata dalla "loginTask" ogni volta che un utente fa il LOGIN con successo
     * alla piattafroma, per controllare se mentre l'utente eraa offfline sono sopraggiunti inviti e inviarglieli
     * @param usr istanza dell'utente dalla quale recuperare insieme inviti pendenti
     */
    private void sendPendingInvites(User usr){
        //recupero canale di invio inviti dell'utente
        SocketChannel invitesChannel = this.serverDataStructures.searchHashInvites(this.client);

        if(invitesChannel == null) { //utente  si e' disconesso / prima volta che fa LOGIN
            return;
        }

        //creo nuova istanza di ServerMessageManagement per mandargli msg
        ServerMessageManagement smmForDest = new ServerMessageManagement(invitesChannel);

        //acquisisco mutua esclusione sul canale di invio degli inviti dell'utente e gli invio l'invito
        synchronized (usr.getLockInvitesSocket()){
            //reperisco l'insieme delle notifiche pendenti dell'utente
            Set<String> pendingIvites = usr.getSetPendingDocs();

            Set<String> tmpSendInvites = new LinkedHashSet<>();

            //itero sugli inviti pendenti e provo ad inviarli
            for(String invite: pendingIvites){
                //provo ad inviargli invito
                FunctionOutcome check = smmForDest.writeResponse(ServerResponse.OP_ONLINE_INVITE_ADVERTISEMENT, invite);

                if(check == FunctionOutcome.SUCCESS){ //invio ha avuto successo
                    usr.removePendingInvite(invite);   //elimino invito pendente dall'insieme inviti pendenti

                    //ricavo nome del documento a cui utente e' stato invitato a collaborare
                    String document = getDocumentFromInvite(invite);

                    if(document.isEmpty())
                        continue; //itero invito successivo

                    this.serverDataStructures.validateUserAsModifier(usr.getUsername(), document, false);
                }
                else{  //check == FunctionOutcome.FAILURE => utente si e' discoesso
                    // lascio invito nell'insieme e provero' ad inviarglielo al successivo LOGIN
                    return;
                }
            }
        }
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

        //invio eventuali inviti pendenti
        //N.B. La prima volta che utente fara' LOGIN suo invitesSocket non sara' attivo => viene attivato
        // infatti con il suo primo LOGIN andato a successo (non avra' comunque inviti pendenti proprio perche' ha appena
        // effetuato la registrazione e la connessione)
        sendPendingInvites(user);

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione privata chiamata da "logoutTask" che si occupa di liberare le eventuali sezioni
     * acquisite dal Client
     * @param username nome dell'utente di cui si e' fato il logout
     */
    private void freeAcquiredSections(String username){
        //verifico se il Client ha acquisito mutua sezione su qualche sezione e la rilascio, in caso affermativo
        //AVENDO TOLTO CLIENT DAI CONNESSI => INVITI A COLLABORARE A NUOVI DOCUMENTI VANNO NEI PENDING_INVITES
        // => NUOVI DOCUMENTI INSERITI NELL'INSIEME DEI DOCUMENTI DELL'UTENTE QUANDO FA LOGIN => insieme consistente
        //itero sull'insieme dei documenti del Client
        User usr = this.serverDataStructures.getUserFromHash(username); //recupero istanza dell'utente
        Set<String> usrDocs =  usr.getSetDocs(); //recupero insieme documenti dell'utente
        for(String document: usrDocs){
            Document doc = this.serverDataStructures.getDocumentFromHash(document); //recupero istanza del documento
            //itero sull'array di lock per verificare se Client ne ha acquisita qualcuna

            //NON MI INTERESSA CONSISTENZA (devo solo trovare slots eventualmente occupati dal clientSocket)

            String[] sectionsLockArray = doc.getSectionsLockArray();
            for(int i = 0; i < sectionsLockArray.length; i++){
                if(sectionsLockArray[i].equals(username)){
                    //libero sezione
                    doc.unlockSection(i + 1, username); //loop parte da 0, conteggio sezioni da 1
                }
            }
        }
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

        //libero le eventuali sezioni acquisite dall'utente
        freeAcquiredSections(username);

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
        //1. verifico che nuovo documento che vuole creare non esista gia'
        //2. creo nuovo documento ed aggiorno la sua istanza
        //3. aggiorno HashTable dei documenti
        //4. recupero dalla HashTable degli utenti registrati la sua istanza di User
        //5. aggiungo documento ai documenti modificabili dall'utente

        ServerResponse serverResponse = this.serverDataStructures.registerNewDocument(username, document, numSections);

        if(serverResponse != ServerResponse.OP_OK)
            return this.serverMessageManagement.writeResponse(serverResponse, "");

        //recupero cartella dedicata alla memorizzazione dei files dell'utente
        String userSaveDirectoryPath = this.configurationsManagement.getServerSaveDocumentsDirectory();

        //recupero path della cartella/documento
        String userDocumentPath = userSaveDirectoryPath + document +  "/"; //documento e' una cartella

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
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. verifico che documento che vuole condividere esista (sia presente nella ht documenti)
        //2. verifico che utente sia il creatore del documento
        //3. verifico che il destinatario non sia l'utente stesso
        //5. verifico che il destinatario non sia gia' collaboratore
        //5. verifico che il destinatario sia registrato al servizio
        //6. inserisco destinatario tra i contribuenti del documento
        //7. inserisco notifica dell'invito alla collaborazione nel set del destinatario, a secondda che sia
        //   offline (notifica ricevuta quando fa LOGIN) oppure online (notifica ricevuta istantanemente grazie al
        //   Listener thread degli inviti del Client)

        //verifico se documento esiste (provo a reperirlo nella ht dei documenti)
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null)  //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //verifico se il destinatario e' registato al servizio (cercandolo nella ht degli utenti)
        User receiver = this.serverDataStructures.getUserFromHash(dest);
        if(receiver == null) //destinatario non e'registrato al servizio
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DEST_NOT_REGISTERED, "");

        //verifico che l'utente sia il creatore del documento (altrimenti non lo puo' condividere)
        if(!doc.isCreator(username))
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_CREATOR, "");

        //utente e' il creatore del documento e dest non e' suo creatore
        //verifico che destinatario non sia l'utente stesso
        if(username.equals(dest))
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_IS_DEST, "");

        //verifico che il destinatario non sia il creatore del documento (in tal caso e' gia' collaboratore)
        if(doc.isCreator(dest))
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DEST_ALREADY_CONTRIBUTOR, "");

        //verifico che il destinatario non sia gia' collaboratore del documento (no MUTUA ESCLUSIONE perche'
        // nessun altro lo potra' inserire tra i collaboratori, perche' io sono creatore)
        if(doc.checkIfUserIsModifier(dest))
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DEST_ALREADY_CONTRIBUTOR, "");

        //creo stringa da inviargli come notifica di invito
        String invite = String.format("[%s] >> Sei stato invitato da |%s| a collaborare al documento " +
                "|%s|", dest, username, document);

        //verifico se destinatario e' online, per differenziare come comportarmi:
        //1. se e' online => gli notifico immediatamente invito
        //2. se e' offline => inserisco invito nell'insieme degli inviti pendenti e appena fa il login glielo
        //                      faccio sapere
        SocketChannel destSocket = this.serverDataStructures.getSocketChannelFromUsername(dest);

        if(destSocket == null){  //dest si e' disconesso
            receiver.addSetPendingDocs(invite);   //inserisco invito nell'insieme dei pendenti
        }
        else{  //dest e' connesso
            //recupero canale di invio del destinatario
            SocketChannel destInvitesChannel = this.serverDataStructures.searchHashInvites(destSocket);

            if(destInvitesChannel == null){ //dest si e' disconesso
                receiver.addSetPendingDocs(invite);   //inserisco invito nell'insieme dei pendenti
            }
            //creo nuova istanza di ServerMessageManagement per mandargli msg
            ServerMessageManagement smmForDest = new ServerMessageManagement(destInvitesChannel);

            //acquisisco mutua esclusione sul canale di invio degli inviti dell'utente e gli invio l'invito
            synchronized (receiver.getLockInvitesSocket()){
                //provo ad inviargli invito
                FunctionOutcome check = smmForDest.writeResponse(ServerResponse.OP_ONLINE_INVITE_ADVERTISEMENT, invite);

                if(check == FunctionOutcome.FAILURE) //invio fallito (forse Client si e' disconess)
                    receiver.addSetPendingDocs(invite);   //inserisco invito nell'insieme dei pendenti
                else{ //invio invito ha avuto successo
                    //inserisco il destinatario come collaboratore del documento
                    //inserisco documento nell'insieme dei documenti modificabili dal destinatario
                    this.serverDataStructures.validateUserAsModifier(dest, document, false);

                }
            }
        }

        //invio invito andato a buon fine
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di inviare il contenuto di una sezione (acceduta e letta in mutua esclsione)
     * ad un client, con il seguente formato di messaggio:
     * 1. l'HEADER contenente:
     * a) OP_SECTION_IS_COMING
     * b) la dim. del BODY (contenuto del file)
     * 2. il BODY (contenuto del file)
     * @param document documento a cui appartiene la sezione
     * @param i numero della sezione da inviare
     * @return SUCCESS se l'invio della dimensione del file/sezione e il contenuto del file/sezione sono andati a
     *                 buon fine
     *         FAILURE altrimenti
     */
    private FunctionOutcome sendSection(String document, int i){

        //recupero cartella dedicata alla memorizzazione dei files
        String userSaveDirectoryPath = this.configurationsManagement.getServerSaveDocumentsDirectory();

        //recupero path della cartella/documento
        String userDocumentPath = userSaveDirectoryPath + document +  "/"; //documento e' una cartella

        //ricavo nome del file del file/sezione
        String userSectionFile = userDocumentPath + i + ".txt";

        String content = "";

        //apro un FileChannel per leggere il contenuto del file/sezione
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(userSectionFile, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        FileChannel outChannel = randomAccessFile.getChannel(); //ricavo channel del file

        //FileLock => mutua esclusione sul file con JavaNIO
        try (outChannel; FileLock fileLock = outChannel.lock()) {

            FileManagement fileManagement = new FileManagement();
            content = fileManagement.readFile(userSectionFile);

            //rilascio mutua esclusione sul file/sezione
            fileLock.release();

        } catch (OverlappingFileLockException | IOException ex) {
            System.err.println("Exception occured while trying to get a lock on File... " + ex.getMessage());
            System.exit(-1);
        }
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_IS_COMING, content);
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
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste (non rientra nella lista dei documenti creati /
     *          condivisi con l'utente)
     */
    public FunctionOutcome showDocumentTask(String document){
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. controllo che documento esista
        //2. controllo che utente abbia permessi per visualizzarlo (sia suo creatore/collaboratore)
        //3.invio contentuo del file all'utente

        //controllo se documento esiste
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null) //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //documento esiste
        //verifico se utente ha i permessi per visualizzarlo
        if(!doc.isCreator(username)){ //utente non e' creatore
            //verifico se utente e' collaboratore
            if(!doc.checkIfUserIsModifier(username)){ //utente non e' collaboratore
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_PERMISSION_DENIED, "");
            }
        }

        //utente e creatore/collaboratore del documento => lo puo' visualizzare
        //recupero numero sezioni del documento
        int numSections = doc.getNumberSections();

        //invio numero delle sezioni al Client (cosi sa quanto deve attendere per leggere i vari files)
        String body = String.valueOf(numSections);
        FunctionOutcome check =  this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, body);


        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //invio numero sezioni fallita

        //invio numero sezioni/files andato a buon fine
        //itero sulle sezioni
        for(int i = 1; i <= numSections; i++){

            //provo ad inviare dimensione del file/sezione e contenuto del file/sezione al Client
            check = sendSection(document, i);

            if(check == FunctionOutcome.FAILURE){
                return FunctionOutcome.FAILURE; //invio dim.sezione / sezione fallito
            }
        }

        //reperisco informazioni su chi sta editando il documento
        StringBuilder builder = new StringBuilder();

        for(int i = 1; i <= numSections; i++){
            String userWhoIsModifingSection = doc.checkIfSectionIsLocked(i);
            if(!userWhoIsModifingSection.isEmpty()){
                builder.append("\n");
                builder.append(String.format("        |%s| sta modificando la sezione |%s|", userWhoIsModifingSection, i));
            }
        }

        String msg = builder.toString();

        if(msg.isEmpty())
            msg = String.format("Nessuno sta editando il documento |%s| in questo momento", document);

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_WHO_IS_EDITING, msg);
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
     */
    public FunctionOutcome showSectionTask(String document, int numSection){
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. controllo che documento esista
        //2. controllo che sezione richiesta esista
        //3. controllo che utente abbia permessi per visualizzarla (sia  creatore/collaboratore del documento)
        //3. invio contentuo del file all'utente

        //controllo se documento esiste
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null) //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //documento esiste
        //verifico se utente ha i permessi per visualizzarlo
        if(!doc.isCreator(username)){ //utente non e' creatore
            //verifico se utente e' collaboratore
            if(!doc.checkIfUserIsModifier(username)){ //utente non e' collaboratore
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_PERMISSION_DENIED, "");
            }
        }

        //utente e creatore/collaboratore del documento => lo puo' visualizzare
        //recupero numero sezioni del documento, per verificare che la sezione esisti
        int numSections = doc.getNumberSections();

        if(numSection < 1 || numSection > numSections){ //sezione non fa parte del documento
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_NOT_EXIST, "");
        }

        //invio buon esito lettura richiesta al Client
        FunctionOutcome check = this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");

        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //problemi con invio acknowledgement al Client

        //provo ad inviare dimensione del file/sezione e contenuto del file/sezione al Client
        check = sendSection(document, numSection);

        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //segnalo errore al Worker

        //reperisco informazioni su chi sta editando il documento
        String msg;
        String userWhoIsModifingSection = doc.checkIfSectionIsLocked(numSection);

        if(userWhoIsModifingSection.isEmpty()){
            msg = String.format("Nessuno sta editando la sezione |%s| in questo momento", numSection);
        }
        else{
            msg = String.format("|%s| sta modificando la sezione |%s| in questo momento", userWhoIsModifingSection, numSection);
        }

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_WHO_IS_EDITING, msg);
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di visualizzazione dei documenti di cui l'utente che richiede
     * l'operazione e' creato oppure collaboratore (vengono fornite anche le informazioni sugli altri collaboratori/
     * creatori)
     * @return OP_OK se la visualizzazione delle informazioni dei documenti dell'utente hanno avuto successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     */
    public FunctionOutcome listTask(){

        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. recupero sua istanza di User dalla ht degli utenti
        //2. recupero lista documenti che puo' modificare (perche' collaboratore/creatore)
        //3. per ogni documento, recupero istanza Doc dalla ht dei documenti e concatento le sue informazioni da
        //   mandare al Client come risposta

        StringBuilder body = new StringBuilder(); //BODY della risposta da mandare al Client

        //recupero istanza dell'utente
        User user = this.serverDataStructures.getUserFromHash(username);

        Set<String> userDocs = user.getSetDocs();

        for(String document: userDocs){

            body.append("\n");
            body.append(document);
            body.append(":\n");

            //recupero istanza del documento
            Document doc = this.serverDataStructures.getDocumentFromHash(document);

            //recupero creatore del documento
            String creator = doc.getCreatorName();

            body.append("    creatore : ");
            body.append(creator);
            body.append("\n");
            body.append("    collaboratori : ");

            //recupero altri collaboratori
            LinkedHashSet<String> modifiers = doc.getModifiers();

            //itero sui collaboratori del documento
            for(String modifier: modifiers){
                body.append(modifier);
                body.append(" ");
            }
        }

        //invio informazioni dei documenti dell'utente sottoforma di stringa, incapsulata nel body
        //della risposta del Server
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, body.toString());
    }

    /**
     * Funzione che si occupa di soddisfare la richiesta di editare la sezione del documento passati come argomento
     * @param document documento di cui editare la sezione
     * @param numSection sezione da editare
     * @return OP_OK se la modifica della sezione del documento ha avuto successo
     *         OP_USER_NOT_ALLOWED_TO_EDIT se l'utente non e' collaboratore/creatore del documento
     *         OP_SECTION_ALREADY_IN_EDITING_MODE se un altro utente sta gia' editando la sezione
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste
     *         OP_SECTION_NOT_EXIST se la sezione non esiste
     */
    public FunctionOutcome editTask(String document, int numSection){
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. verifico che documento esista
        //2. verifico che sezione faccia parte del documento richiesto
        //3. verifico se l'utente ha il permesso per editare sezione (e' creatore/collaboratore documento)
        //3. verifico che sezione non sia gia' in fase di editing da qualcun'altro
        //4. acuisisco mutua esclusione sulla sezione

        //controllo se documento esiste
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null) //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //documento esiste
        //verifico se utente ha i permessi per visualizzarlo
        if(!doc.isCreator(username)){ //utente non e' creatore
            //verifico se utente e' collaboratore
            if(!doc.checkIfUserIsModifier(username)){ //utente non e' collaboratore
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_PERMISSION_DENIED, "");
            }
        }

        //utente e creatore/collaboratore del documento => lo puo' visualizzare
        //recupero numero sezioni del documento, per verificare che la sezione esisti
        int numSections = doc.getNumberSections();

        if(numSection < 1 || numSection > numSections){ //sezione non fa parte del documento
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_NOT_EXIST, "");
        }

        //verifico che utente non stia gia' editando una sezione di questo documento
        //NON MI INTERESSA CONSISTENZA (devo solo trovare slots eventualmente occupati dal clientSocket)

        String[] sectionsLockArray = doc.getSectionsLockArray();
        for(int i = 0; i < sectionsLockArray.length; i++){
            if(sectionsLockArray[i].equals(username)){
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_ALREADY_EDIT_BY_USER, "");
            }
        }

        //documento non e' editato dall'utente
        //provo ad acquisire la mutua esclusione sulla sezione che utente vuole editare
        String lock = doc.lockSection(numSection, username);

        if(!lock.equals(username)) //sezione acquisita gia' da qualcunaltro => invio nome di chi l'ha gia' acquisita
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_ALREADY_IN_EDITING_MODE, lock);

        //sezione acquisita
        FunctionOutcome check = this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");

        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //problemi con invio acknowledgement al Client

        //provo ad inviare dimensione del file/sezione e contenuto del file/sezione al Client
        check = sendSection(document, numSection);

        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //segnalo errore al Worker

        //se invio sezione ha avuto successo, devo inviare al Client l'indirizzo di multicast del documento
        //per consentirgli di attivare chatListener
        String multicastInd = doc.getChatInd();
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_MULTICAST_IND_IS_COMING, multicastInd);
    }

    /**
     * Funzione privata chiamata da "endEditTask" che si occupa di scrivere in MUTUA ESCLUSIONE
     * l'aggiornamento mandato dal Client sulla sezione/file
     * @param document documento di cui la sezione in cui salvare le modifiche
     * @param numSection sezione in cui salvare le modifiche
     * @return SUCCESS sezione aggiornata con successo
     *        FAILURE impossibile aggiornare la sezione
     */
    private FunctionOutcome updateSection(String document, int numSection){
        //recupero cartella dedicata alla memorizzazione dei files
        String userSaveDirectoryPath = this.configurationsManagement.getServerSaveDocumentsDirectory();

        //recupero path della cartella/documento
        String userDocumentPath = userSaveDirectoryPath + document +  "/"; //documento e' una cartella

        //ricavo nome del file del file/sezione
        String userSectionFile = userDocumentPath + numSection + ".txt";

        //ricavo contentuo della sezione aggiornata
        String content = serverMessageManagement.getBodyMessage();

        //apro un FileChannel per leggere il contenuto del file/sezione
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(userSectionFile, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        FileChannel outChannel = randomAccessFile.getChannel(); //ricavo channel del file

        //FileLock => mutua esclusione sul file con JavaNIO
        try (outChannel; FileLock fileLock = outChannel.lock()) {

            FileManagement fileManagement = new FileManagement();
            fileManagement.writeFile(userSectionFile, content);

            //rilascio mutua esclusione sul file/sezione
            fileLock.release();

        } catch (OverlappingFileLockException | IOException ex) {
            System.err.println("Exception occured while trying to get a lock on File... " + ex.getMessage());
            System.exit(-1);
        }

        return FunctionOutcome.SUCCESS;
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
        //verifico se utente e' connesso
        String username = this.serverDataStructures.checkIfSocketChannelIsOnline(client);

        if(username == null)
            //utente non e' connesso
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_USER_NOT_ONLINE, "");

        //utente connesso => tramite verifica connessione ho recuperato il suo nome utente
        //1. verifico che documento esista
        //2. verifico che sezione faccia parte del documento richiesto
        //3. verifico se l'utente ha il permesso per editare sezione (e' creatore/collaboratore documento)
        //3. verifico che sezione non sia gia' in fase di editing da qualcun'altro
        //4. acuisisco mutua esclusione sulla sezione

        //controllo se documento esiste
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null) //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //documento esiste
        //verifico se utente ha i permessi per visualizzarlo
        if(!doc.isCreator(username)){ //utente non e' creatore
            //verifico se utente e' collaboratore
            if(!doc.checkIfUserIsModifier(username)){ //utente non e' collaboratore
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_PERMISSION_DENIED, "");
            }
        }

        //utente e creatore/collaboratore del documento => lo puo' visualizzare
        //recupero numero sezioni del documento, per verificare che la sezione esisti
        int numSections = doc.getNumberSections();

        if(numSection < 1 || numSection > numSections){ //sezione non fa parte del documento
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_NOT_EXIST, "");
        }

        String locked = doc.checkIfSectionIsLocked(numSection);

        if(locked.isEmpty()) //sezione non e' in editing mode
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_NOT_IN_EDITING_MODE, "");
        else if(!locked.equals(username)) //sezione editata da qualcuno diverso dall'utente => invio chi la sta editando
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_EDITED_BY_SOMEONE_ELSE, locked);

        //invio buon esito al Client per segnalargli di mandarmi contenuto aggiornato
        FunctionOutcome check =  this.serverMessageManagement.writeResponse(ServerResponse.OP_SERVER_READY_FOR_UPDATE, "");

        if(check == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE; //segnalo al Worker errore

        //attendo contenuto aggiornato dal Client
        //leggo richiesta del Client
        FunctionOutcome readRequest = this.serverMessageManagement.readRequest();

        if(readRequest == FunctionOutcome.FAILURE)
            return FunctionOutcome.FAILURE;  //segnalo al Worker errore

        //nel BODY della richiesta e' contenuto l'aggiornamento della sezione
        //aggiorno sezione con contenuto mandato dal Client, prima di rilasciare la mutua esclusione sulla sezione
        check = updateSection(document, numSection);

        //impossibile aggiornare la sezione per qualche problema
        if(check == FunctionOutcome.FAILURE)
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_IMPOSSIBLE_TO_UPDATE, "");

        //sezione aggiornata
        //rilascio la mutua esclusione
        ServerResponse serverResponse = doc.unlockSection(numSection, username);

        if(serverResponse == ServerResponse.OP_SECTION_NOT_IN_EDITING_MODE)
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_NOT_IN_EDITING_MODE, "");
        else if(serverResponse == ServerResponse.OP_SECTION_EDITED_BY_SOMEONE_ELSE)
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_EDITED_BY_SOMEONE_ELSE, "");

        //sezione rilasciata
        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che si occupa di inviare il messaggio dell'utente sulla chat
     * @param document nome del documento del quale reperire la chat
     * @return OP_OK se la modifica della sezione del documento ha avuto successo
     *         OP_USER_NOT_ONLINE se l'utente che richiede operazione non e' connesso
     *         OP_OP_DOCUMENT_NOT_EXIST se il documento non esiste
     */
    public FunctionOutcome sendTask(String document){

        //controllo se documento esiste
        Document doc = this.serverDataStructures.getDocumentFromHash(document);

        if(doc == null) //documento non esiste
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_NOT_EXIST, "");

        //recupero indirizzo di multicat del documento
        String multicastInd = doc.getChatInd();

        FunctionOutcome check = this.serverMessageManagement.readRequest();

        if(check == FunctionOutcome.FAILURE)
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SEND_IMPOSSIBLE_TO_READ_MESSAGE, "");

        //recupero messaggio da inviare sulla chat
        String message = this.serverMessageManagement.getBodyMessage();

        try {
            InetAddress group = InetAddress.getByName(multicastInd);
            MulticastSocket chatSocket = new MulticastSocket(this.configurationsManagement.getMulticastPort());

            //ricavo byte del messaggio specificato
            byte[] buf = message.getBytes();
            //creo DatagramPacket corrispondente
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, this.configurationsManagement.getMulticastPort());

            //invio sul Socket multicast il messaggio (inserito in un datagramPacket)
            chatSocket.send(packet);

        } catch (IOException e) {
            //e.printStackTrace();
            return this.serverMessageManagement.writeResponse(ServerResponse.OP_SEND_FAILURE, "");
        }

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    public FunctionOutcome iAmClientSocketTask(){
        //inserisco nome del Socket relativo al SocketChannel dell'utente connesso;
        String hostAndPort;
        try {
            hostAndPort = this.client.getRemoteAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return FunctionOutcome.FAILURE; //notifico al Worker fallimento inserimento
        }

        //inserisco associazione tra nome del Socket e clientSocketChannel nella ht corrispondente
        //(questo mi consete di individuare SocketChannel da utilizzare per inviare inviti al Client,
        // quando tale SocketChannel si connetera' al Server)
        this.serverDataStructures.insertHashSocketNames(hostAndPort, this.client);

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }

    /**
     * Funzione che rileva che un SocketChannel connesso al Server deve essere utilizzato come
     * channel per inviare gli inviti alle collaborazioni al Client specificato come argomento
     * @param hostAndPort stringa che contiene nome del Socket del Client di cui questo SocketChannel e' canale di invio
     * @return SUCCESS se SochetChannel di invio e' stato rilevato con successo
     *         FAILURE se subentrano errori nel rilevamento del SocketChannel di invio
     */
    public FunctionOutcome iAmAnInvitesSocketTask(String hostAndPort){

        this.serverDataStructures.printSocketNames();

        //verifico quale clientSocketChannel corrisponde al nome del Socket del Client
        SocketChannel clientSocketChannel = this.serverDataStructures.searchHashSocketNames(hostAndPort);

        if(clientSocketChannel == null){
            System.err.println("[ERR] >> Impossibile che non esista clientSocketChannel nella ht dei nomi dei sockets" +
                    "relativo al nome Socket = " + hostAndPort);
            System.exit(-1);
        }

        //ho reperito clientSocketChannel di cui questo SocketChannel e' canale di invio => inserisco associazione
        //tra i due nella ht corrispondente
        this.serverDataStructures.insertHashInvites(clientSocketChannel, this.client);

        return this.serverMessageManagement.writeResponse(ServerResponse.OP_OK, "");
    }
}
