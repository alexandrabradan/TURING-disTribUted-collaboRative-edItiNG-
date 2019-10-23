import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ClientMessageManagement {
    /**
     * SocketChannel del Client con il quale inviare richiesta al Server
     */
    private SocketChannel clientSocket;
    /**
     * richiesta del Client, sottoforma di ENUM
     */
    private CommandType currentCommand;
    /**
     * eventuale primo argomento della richiesta
     */
    private String currentArg1;
    /**
     * eventuale secondo argomento della richiesta
     */
    private String currentArg2;
    /**
     * ByteBuffer che contiene l'intestazione della richiesta
     */
    private ByteBuffer header;
    /**
     * ByteBuffer che contiene l'eventuale corpo della richiesta
     */
    private ByteBuffer body;
    /**
     * Classe per leggere il contenuto della risposta del Server dal SocketChannel
     */
    private SocketChannelReadManagement socketChannelReadManagement;
    /**
     * Classe per scrivere la richiesta al Server sul SocketChannel
     */
    private SocketChannelWriteManagement socketChannelWriteManagement;
    /**
     * Classe per reperire le variabili di configurazione del Client
     */
    private ClientConfigurationManagement configurationsManagement;
    /**
     * Classe per gestire  cartelle e file
     */
    private FileManagement fileManagement;
    /**
     * riferimento al chatListener del file
     */
    private ClientChatListener clientChatListenerThread;

    /**
     * Costruttore della classe RequestManagement
     * @param clientSocket SocketChannel del Client di cui bisogna inviare richiesta e leggere risposta
     * @param configurationsManagement per reperire var. di configurazione
     * @param clientChatListenerThread riferimento al chatListener del Client, da attivare e disattivare a seconda
     *                                 delle operazioni richieste dal client
     */
    public ClientMessageManagement(SocketChannel clientSocket, ClientConfigurationManagement configurationsManagement,
                                   ClientChatListener clientChatListenerThread){
        this.clientSocket = clientSocket;
        this.fileManagement = new FileManagement();
        this.configurationsManagement = configurationsManagement;
        this.clientChatListenerThread = clientChatListenerThread;
        this.socketChannelReadManagement = new SocketChannelReadManagement(this.clientSocket);
        this.socketChannelWriteManagement = new SocketChannelWriteManagement(this.clientSocket);

        setDefaultVariablesValues(); //resetto variabili della classe
    }

    /**
     * Funzione che si occupa di resettare ad ogni invocazione del metodo "writeRequest" le variabili di
     * condizione della classe
     */
    private void setDefaultVariablesValues(){
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    /**
     * Funzione che restituisci il contenuto del BODY sottoforma di stringa
     * @return contenuto del ByteBuffer BODY come stringa
     */
    public String getBodyMessage(){
        return new String(this.body.array(), StandardCharsets.UTF_8);
    }

    /**
     * Funzione che si occupa di inviare:
     * 1. l'HEADER della richiesta contenente:
     * a) CommandType codificato come ENUM (intero) della richiesta
     * b) il BODY della richiesta contenente:
     * a) gli eventuali argomenti della richiesta, separati da uno whitespace per distinguerli
     * @param command tipo di richiesta
     * @param arg1 eventuale primo argomento
     * @param arg2 eventulae secondo argomento
     * @return SUCCESS se l'invio della dimensione delle richiesta e la richiesta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome writeRequest(CommandType command, String arg1, String arg2){

        //resetto il commando corrente e gli eventuali argomenti per evitare malintesi con
        //invocazioni precedenti
        setDefaultVariablesValues();

        //invio HEADER contenente:
        //tipo di richiesta => e' ENUM => intero => codificato con 4 bytes
        // dim. body/argomenti richiesta => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        //inizializzo commando corrente ed eventuali argomenti
        this.currentCommand = command;
        this.currentArg1 = arg1;
        this.currentArg2 = arg2;

        String request =  ""; //richiesta vuota

        //costruisco body della richiesta (argomenti da inviare)
        //controllo se ho da inviare due argomenti
        if(!arg1.isEmpty() && !arg2.isEmpty()){ //ho 2 argomenti da inviare
            request =  arg1 + " " + arg2;   //inserisco degli spazi per distinguere argomenti della richiesta
        }
        else if(!arg1.isEmpty()){ //ho 1 argomento da inviare
            request = arg1;
        }

        byte[] requestBytes = request.getBytes(); //converto BODY in bytes per scoprire sua lunghezza
        int requestLength = requestBytes.length; //ricavo lunghezza del BODY

        this.header.putInt(command.ordinal()); //ordinale() => reperisco valore numerico ENUM
        this.header.putInt(requestLength); //inserisco dim. body

        this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        //invio HEADER
        FunctionOutcome check = this.socketChannelWriteManagement.write(this.header, 8);

        if(check == FunctionOutcome.SUCCESS){  //invio HEADER avvenuto con successo

            if(requestLength > 0){
                this.body = ByteBuffer.allocate(requestLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                this.body.put(request.getBytes());  //inserisco messaggio di richiesta nel buffer

                this.body.flip(); //modalita' lettura (position=0, limit = bytesWritten)

                //invio BODY al Server
                return this.socketChannelWriteManagement.write(this.body, this.body.limit()); //invio BODY
            }
            return FunctionOutcome.SUCCESS; //invio HEADER avvenuto con successo

        }
        else
            return FunctionOutcome.FAILURE; //invio HEADER fallito
    }

    /**
     * Funzione che si occupa di leggere:
     * 1. l'HEADER della risposta del Server, contenente: dimensionde della risposta del Server (per allocare un buffer di lettura opportuno)
     * a) ServerResponse codificato come ENUM (intero)
     * b) dimensione eventuale BODY della risposta
     * 2. il BODY delle risposta del Server
     * @param currentUser username corrente connesso, per effettuare stampe personalizzate
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome readResponse(String currentUser){
        //ricevo HEADER contenente:
        //tipo di risposta => e' ENUM => intero => codificato con 4 bytes
        // dim. body risposta => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        ServerResponse responseType; //tipo risposta
        int responseBodyLength;  //lunghezza BODY

        //leggo dimensione risposta
        FunctionOutcome check = this.socketChannelReadManagement.read(this.header, 8);

        if(check == FunctionOutcome.SUCCESS){ //lettura

            this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

            responseType = ServerResponse.values()[this.header.getInt()]; //converto valore numerico nel rispettivo ENUM
            responseBodyLength = this.header.getInt(); //reperisco dimensione BODY

            //leggo eventuale BODY della risposta
            if(responseBodyLength > 0){

                //alloco nuovo buffer per leggervi BODY, di dimensione letta sopra
                this.body = ByteBuffer.allocate(responseBodyLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                //leggo BODY della risposta
                check = this.socketChannelReadManagement.read(this.body, body.limit());

                if(check == FunctionOutcome.FAILURE)
                    return FunctionOutcome.FAILURE; //lettura BODY fallita
                else {
                    //lettura HEADER+BODY successo => stampa personalizzata
                    return manageResponse(responseType, responseBodyLength, currentUser);
                }
            }
            else {
                //lettura HEADER successo => stampa personalizzata
                return manageResponse(responseType, responseBodyLength, currentUser);
            }
        }
        else return FunctionOutcome.FAILURE; //lettura HEADER fallita
    }



    /**
     * Funzione che si occupa di attendere l'invio da parte del Server del contenuto delle sezioni
     * del documento passati come argomento
     * @param i numero della sezione da leggere ed eventualemente creare/sovvrascrivere
     * @return SUCCESS se la lettura e' andata a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome readAndCreateSectionsForClient(String document, int i) {

        //ricevo HEADER contenente:
        // OP_SECTION_IS_COMING => enum => flag
        // dim. body (contenuto file/sezione) => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        int contentLength;  //lunghezza BODY (contenuto del file)

        //leggo dimensione risposta
        FunctionOutcome check = this.socketChannelReadManagement.read(this.header, 8);

        if (check == FunctionOutcome.SUCCESS) { //lettura

            this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

            ServerResponse serverResponse = ServerResponse.values()[this.header.getInt()]; //converto valore numerico nel rispettivo ENUM
            contentLength = this.header.getInt(); //reperisco dimensione BODY

            //file/sezione vuoto
            if(contentLength == 0){
                //creo semplicemente sezione
                String sectionName = document + i + ".txt";
                this.fileManagement.createFile(sectionName);
                return FunctionOutcome.SUCCESS;
            }

            //leggo eventuale BODY della risposta
            if (contentLength > 0) {

                //alloco nuovo buffer per leggervi BODY, di dimensione letta sopra
                this.body = ByteBuffer.allocate(contentLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                //leggo BODY della risposta
                check = this.socketChannelReadManagement.read(this.body, body.limit());

                if (check == FunctionOutcome.FAILURE)
                    return FunctionOutcome.FAILURE; //lettura BODY fallita

                //creo file/sezione (controllo che non esistesse prima viene fatto in  ClientCommandLineManagement)
                //altrimenti lo sovrascrivo
                String sectionName = document + i + ".txt";
                this.fileManagement.createFile(sectionName);

                this.body.flip(); //modalita' lettura (position=0, limit = bytesWritten)

                //recupero contenuto del file
                String content = new String(this.body.array(), StandardCharsets.UTF_8);

                //scrivo contenuto sul file
                this.fileManagement.writeFile(sectionName, content);

                return FunctionOutcome.SUCCESS; //lettura HEADER+BODY successo
            }
            else return FunctionOutcome.SUCCESS; //lettura HEADER successo
        }
        else return FunctionOutcome.FAILURE; //lettura HEADER fallita
    }

    /**
     * Funzione che si occupa di inviare al Server la sezione aggiornata della sezione editata
     * @param currentUser utente attualmente connesso
     * @param document documento
     * @param section sezione
     * @return SUCCESS se invio ha avuto successo
     *         FAILURE altrimenti
     */
    public FunctionOutcome sendUpdateSection(String currentUser, String document, String section){

        //invio sezione aggiornta al Server
        //reperisco contenuto del file aggiornato e lo mando al Server
        String clientEditDirectory = this.configurationsManagement.getClientsEditDocumentsDirectory();
        String documentDirectory = clientEditDirectory + document + "/";
        String sectionName = documentDirectory + section + ".txt";
        String fileContent = fileManagement.readFile(sectionName);

        //invio richiesta al Server
        FunctionOutcome check = writeRequest(currentCommand, fileContent, "");

        if(check == FunctionOutcome.FAILURE){
            return FunctionOutcome.FAILURE;  //segnalo fallimento al Client
        }

        //cancello sezione dalla cartella di editing
        fileManagement.deleteFile(sectionName);

        //cancello documento che conteneva sezione da editare
        fileManagement.deleteDirectory(documentDirectory);

        //attendo risposta di esito dal Server
        return readResponse(currentUser);
    }

    /**
     * Funzione che stampa la risposta personalizzata del Server alla richiesta fatta dal Client
     * @param responeType risposta del Server (sottofroma di enum "ServerResponse")
     * @param responseBodyLenght eventuale contenuto della risposta del Server
     * @param currentUser username corrente connesso, per effettuare stampe personalizzate
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    private FunctionOutcome manageResponse(ServerResponse responeType, int responseBodyLenght, String currentUser){

        String responseBody = "";

        //reperisco il contenuto del BODY della risposta, se presente
        if(responseBodyLenght > 0){
            this.body.flip();  //modalita' lettura (position=0, limit = bytesWritten)
            responseBody = new String(this.body.array(), StandardCharsets.UTF_8);
        }

        //a seconda del comando richiesto posso avere differenti risposte di successo/fallimento da parte del Server
        // verifico che cosa mi ha risposto il Server  ed in base a cio', stampo un messaggio di esito
        switch (responeType){
            case OP_OK:{  //discrimino quale operazione ha avuto successo
                switch (this.currentCommand){
                    case LOGIN:{
                        System.out.println(String.format("[%s] >> Login avvenuto con successo", currentUser));
                        break;
                    }
                    case LOGOUT:{
                        System.out.println(String.format("[%s] >> Logout avvenuto con successo", currentUser));
                        break;
                    }
                    case CREATE:{
                        System.out.println(String.format("[%s] >> Creazione del documento |%s| con |%s| sezioni" +
                                " avvenuta con successo", currentUser, currentArg1, currentArg2));
                        break;
                    }
                    case SHARE:{
                        System.out.println(String.format("[%s] >> Condivisione del documento |%s| con l'utente " +
                                "|%s| avvenuto con successo", currentUser, currentArg1, currentArg2));
                        break;
                    }
                    case SHOW_DOCUMENT:{

                        //creo cartella/documento (eventualmente da cancellare se subbentrano errori)
                        //se non esiste gia'
                        String clientDownloadDirectory = this.configurationsManagement.getClientsDownloadsDocumentsDirectory();
                        String documentDirectory = clientDownloadDirectory + currentArg1 + "/";

                        if(!this.fileManagement.checkEsistenceDirectory(documentDirectory))
                            this.fileManagement.createDirectory(documentDirectory);

                        //ho letto numero sezioni del documento
                        String numSectionsInString = new String(this.body.array(), StandardCharsets.UTF_8);
                        int numSections = Integer.parseInt(numSectionsInString);

                        //mi appresto a fare un ciclo di "numSections" per scaricare files/sezioni
                        for(int i = 1; i <= numSections; i++){
                            FunctionOutcome check = readAndCreateSectionsForClient(documentDirectory, i);

                            if(check == FunctionOutcome.FAILURE){
                                System.out.println(String.format("[%s] >> Impossibile scaricare la sezione |%s| del " +
                                        "documento |%s|. Download del documento fallito", currentUser, i,  currentArg1));

                                //cancello documento/cartella creato
                                this.fileManagement.deleteDirectory(documentDirectory);
                                return FunctionOutcome.FAILURE;
                            }
                        }

                        //leggo quali sezioni sono editate e da chi
                        FunctionOutcome check = readResponse(currentUser);

                        if(check == FunctionOutcome.FAILURE){
                            System.out.println(String.format("[%s] >> Contenuto del documento |%s| scaricato con successo" +
                                            ", ma impossibile reperire informazioni su chi lo sta editando",
                                    currentUser, currentArg1));
                        }
                        else{
                            System.out.println(String.format("[%s] >> Contenuto del documento |%s| scaricato con successo",
                                    currentUser, currentArg1));

                            //recupero messaggio del Client
                            String whoIsEditing = getBodyMessage();
                            if(!whoIsEditing.equals(String.format("Nessuno sta editando il documento |%s| in questo momento",
                                    currentArg1))){
                                System.out.println(String.format("[%s] Documento |%s| ediatato da: ", currentUser, currentArg1));
                                System.out.println("      " + whoIsEditing);
                            }
                            else System.out.println(String.format("[%s] >> " + whoIsEditing, currentUser));
                        }

                        break;
                    }
                    case SHOW_SECTION:{
                        //creo cartella/documento se non esiste gia'
                        String clientDownloadDirectory = this.configurationsManagement.getClientsDownloadsDocumentsDirectory();
                        String documentDirectory = clientDownloadDirectory + currentArg1 + "/";

                        if(!this.fileManagement.checkEsistenceDirectory(documentDirectory))
                            this.fileManagement.createDirectory(documentDirectory);

                        //mi appresto a leggere la sezione richiesta
                        FunctionOutcome check = readAndCreateSectionsForClient(documentDirectory, Integer.parseInt(currentArg2));

                        if(check == FunctionOutcome.FAILURE){
                            System.out.println(String.format("[%s] >> Impossibile scaricare la sezione |%s| del " +
                                            "documento |%s|. Download della sezione fallito", currentUser,
                                    Integer.parseInt(currentArg2),  currentArg1));
                            return FunctionOutcome.FAILURE;
                        }
                        else{

                            //leggo se qualcuno sta editando la sezione
                            check = readResponse(currentUser);

                            if(check == FunctionOutcome.FAILURE){
                                System.out.println(String.format("[%s] >> Sezione |%d| del documento |%s| scaricata con successo," +
                                                " ma impossibile reperire informazioni su chi la sta editando",
                                        currentUser,  Integer.parseInt(currentArg2), currentArg1));
                            }
                            else{
                                System.out.println(String.format("[%s] >> Sezione |%d| del documento |%s| scaricata con successo",
                                        currentUser,  Integer.parseInt(currentArg2), currentArg1));

                                //recupero messaggio del Client
                                String whoIsEditing = getBodyMessage();
                                System.out.println(String.format("[%s] >> " + whoIsEditing, currentUser));
                            }
                        }
                        break;
                    }
                    case LIST:{
                        if(responseBody.isEmpty())
                            System.out.println(String.format("[%s] >> Non ci sono ancora documenti da mostrare", currentUser));
                        else{
                            System.out.println(String.format("[%s] >> Recap tuoi documenti:", currentUser));
                            System.out.println(responseBody);
                        }
                        break;
                    }
                    case EDIT:{
                        //scarico sezione del documento dal Sever per inserirlo nella cartella di editing
                        String clientEditDirectory = this.configurationsManagement.getClientsEditDocumentsDirectory();
                        String documentDirectory = clientEditDirectory + currentArg1 + "/";

                        if(!this.fileManagement.checkEsistenceDirectory(documentDirectory))
                            this.fileManagement.createDirectory(documentDirectory);

                        //mi appresto a leggere la sezione richiesta
                        FunctionOutcome check = readAndCreateSectionsForClient(documentDirectory, Integer.parseInt(currentArg2));

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println(String.format("[%s] >> Impossibile scaricare la sezione |%s| del " +
                                            "documento |%s| per ediatarla. Download della sezione fallito", currentUser,
                                    Integer.parseInt(currentArg2),  currentArg1));
                            return FunctionOutcome.FAILURE;
                        }
                        else{
                            //attendo risposta dal Server
                            check = readResponse(currentUser);

                            if(check == FunctionOutcome.FAILURE){
                                System.err.println(String.format("[%s] >> Impossibile leggere indirizzo di multicast del" +
                                        " documento per create chatListener.", currentUser));
                                return FunctionOutcome.FAILURE;
                            }

                            System.out.println(String.format("[%s] >> Inizio modifica della sezione |%s| del" +
                                    " documento |%s|", currentUser, currentArg2, currentArg1));
                        }

                        //apro sezione scaricata
                        String sectionName = documentDirectory + this.currentArg2 + ".txt";
                        fileManagement.openFile(sectionName);
                        break;
                    }
                    case END_EDIT:{
                        //faccio terminare chatListener del documento che ho terminato di editare
                        terminateChatListener();
                        break;
                    }
                    case RECEIVE:{
                        System.out.println(String.format("[%s] >> Hai un nuovo messaggio sulla Chat", currentUser));
                        System.out.println(responseBody);
                        break;
                    }
                    case SEND:
                    case I_AM_CLIENT_SOCKET:
                    case I_AM_INVITE_SOCKET:{
                        //System.out.println(String.format("[Turing] >> %s eseguito corretamente", currentCommand));
                        break;
                    }
                    default:
                        break;
                }

                //break;
                return FunctionOutcome.SUCCESS; //notifico al ciclo principale che la lettura della risposta e' andata a buon fine
            }
            case OP_ONLINE_INVITE_ADVERTISEMENT:{
                System.out.println();
                System.out.println("    " + this.getBodyMessage());
                System.out.println();
                return FunctionOutcome.SUCCESS; //notifico al ciclo principale che la lettura della risposta e' andata a buon fine
            }
            case OP_DOCUMENT_MULTICAST_IND_IS_COMING:{
                //lettura indirizzo di multicast andata a buon fine => lo recupero per attivare chatListener
                String multicastInd = getBodyMessage();

                //creo chatListener
                this.clientChatListenerThread = new ClientChatListener(currentUser, multicastInd,
                        configurationsManagement);

                //attivo chatListener
                this.clientChatListenerThread.start();
                return FunctionOutcome.SUCCESS; //notifico alla EDIT che creazione del chatListener ha avuto successo
            }
            case OP_WHO_IS_EDITING:{
                return FunctionOutcome.SUCCESS; //notifico alla SHOW_DOC / SHOW_SECTION lettura di chi sta editando
            }
            case OP_SERVER_READY_FOR_UPDATE:{
                return sendUpdateSection(currentUser, currentArg1, currentArg2);
            }
            case OP_USER_NOT_ONLINE:{
                System.err.println("[ERR] >> Utente NON connesso.");
                break;
            }
            case OP_USER_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Utente |%s| NON registrato.", currentUser));
                break;
            }
            case OP_DOCUMENT_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| NON esistente.", currentArg1));
                break;
            }
            case OP_DOCUMENT_PERMISSION_DENIED:{
                System.err.println(String.format("[ERR] >> Non hai i permessi per fare il download del documento |%s|" +
                        ".", currentArg1));
                break;
            }
            case OP_SECTION_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Sezione |%s| del documento |%s| NON " +
                        "esiste.", currentArg2, currentArg1));
                break;
            }
            case OP_USER_ALREADY_ONLINE:{
                System.err.println(String.format("[ERR] >> Username |%s| GIA' connesso.", currentUser));
                break;
            }
            case OP_PASSWORD_INCORRECT:{
                System.err.println(String.format("[ERR] >> Password |%s| ERRATA.", currentArg2));
                break;
            }
            case OP_DOCUMENT_ALREADY_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| GIA' esistente.", currentArg1));
                break;
            }
            case OP_USER_NOT_CREATOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|. Per farlo devi essere il suo creatore.", currentArg2, currentArg1));
                break;
            }
            case OP_USER_IS_DEST:{
                System.err.println(String.format("[ERR] >> Non puoi invitare te stesso a collaborare al documento |%s|.",
                        currentArg1));
                break;
            }
            case OP_DEST_ALREADY_CONTRIBUTOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|, perche' e' gia' collaboratore / creatore.", currentArg2, currentArg1));
                break;
            }
            case OP_DEST_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|. L'utente NON e' registrato alla nostra piattaforma.", currentArg2, currentArg1));
                break;
            }
            case OP_USER_NOT_ALLOWED_TO_EDIT:{
                System.err.println(String.format("[ERR] >> Non puoi editare la sezione|%s| del documento|%s|."
                        + "Per farlo devi essere suo creatore / collaboratore.", currentArg2, currentArg1));
                break;
            }
            case OP_SECTION_ALREADY_IN_EDITING_MODE:{
                System.err.println(String.format("[ERR] >> Non puoi editare la sezione|%s| del documento|%s|."
                        + " Sezione e' gia' editata da |%s|.", currentArg2, currentArg1, getBodyMessage()));
                break;
            }
            case OP_SECTION_NOT_IN_EDITING_MODE:{
                System.err.println(String.format("[ERR] >> Non puoi finire di fare l'editing della sezione|%s| del documento|%s|."
                        + " Sezione non settata per l'editing precedentemente.", currentArg2, currentArg1));
                break;
            }
            case OP_SECTION_EDITED_BY_SOMEONE_ELSE:{
                System.err.println(String.format("[ERR] >> Non puoi finire di fare l'editing della sezione|%s| del documento|%s|."
                        + "Sezione non e' editata da te.", currentArg2, currentArg1));
                break;
            }
            case OP_DOCUMENT_MULTICAST_ADDRESS_RUN_OUT:{
                System.err.println("[ERR] >> Indirizzi di multicast esauriti.");
                break;
            }
            case OP_DOCUMENT_ALREADY_EDIT_BY_USER:{
                System.err.println(String.format("[ERR] >> Stai gia' editando una sezione del documento |%s|." +
                        " Non puoi editare piu' sezioni dello stesso documento alla volta.", this.currentArg1));
                break;
            }
            case  OP_SECTION_IMPOSSIBLE_TO_UPDATE:{
                System.err.println(String.format("[ERR] >> Impossibile aggiornare la sezione |%s| del documento |%s|",
                        this.currentArg2, this.currentArg1));
                break;
            }
            case  OP_INVALID_REQUEST:{
                //non si verifica mai, ma inserito qui per scrupolo qual'ora check del parsing locale del Client fallise
                System.err.println("[ERR] >> Richiesta non rientra nei servizi offerti.");
                break;
            }
            case OP_SEND_FAILURE:{
                System.err.println("[ERR] >> Invio messaggio sulla chat fallito.");
                break;
            }
            case OP_SEND_IMPOSSIBLE_TO_READ_MESSAGE:{
                System.err.println("[ERR] >> Server incapacitato di leggere msg da inviare sulla chat.");
                break;
            }
            case OP_USERNAME_INAVLID_CHARACTERS:{
                System.err.println(String.format("[ERR] >> Username |%s| contiene caratteri speciali. Sceglierne" +
                        " uno che non li contenga.", currentArg1));
                break;
            }
            case OP_DOCUMENT_INAVLID_CHARACTERS:{
                System.err.println(String.format("[ERR] >> Documento |%s| contiene caratteri speciali. Sceglierne" +
                        " uno che non li contenga.", currentArg1));
                break;
            }
            case OP_USER_MUST_LOGOUT:{
                System.err.println("[ERR] >> Devi fare logout per poterti registrare con un altro username.");
                break;
            }
            case OP_USERNAME_ALREADY_TAKEN:{
                System.err.println(String.format("[ERR] Username |%s| gia' in uso.", currentArg1));
                break;
            }
            default:
                break;
        }

        //notifico al ciclo principale che richiesta e' stata scartata dal Server / Server si e' disconesso
        return FunctionOutcome.FAILURE;
    }

    /**
     * Funzione che stampa i messaggi ricevuti sulla chat
     */
    public void visualizeHistory(){
        if(this.clientChatListenerThread != null)
            this.clientChatListenerThread.printHistory();
    }

    /**
     * Funzione che fa terminare chatListener (se attivo)
     */
    public void terminateChatListener(){
        if(this.clientChatListenerThread != null)
            this.clientChatListenerThread.interruptClientChatListener();

        //resetto riferimento al chatListener
        this.clientChatListenerThread = null;
    }
}
