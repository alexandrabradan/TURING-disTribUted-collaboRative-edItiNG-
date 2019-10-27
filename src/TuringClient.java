import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TuringClient {
    /**
     * pah relativo, del file di configurazione di default del Client
     */
    private static String defaultConfFile = "/data/turingClient.conf";
    //private static String defaultConfFile = "/src/data/turingClient.conf";
    /**
     * classe che si occupa di fare il parsing del file di configurazione e memorizzarne i valori
     */
    private static ClientConfigurationManagement configurationsManagement = new ClientConfigurationManagement();
    /**
     * SocketChannel utilizzato per inviare richieste e leggere risposte
     */
    private static SocketChannel clientSocket = null;      //client-socket
    /**
     * indirizzo del SocketChannel  utilizzato per inviare richieste e leggere risposte
     */
    private static String serverHost;
    /**
     * porta del SocketChannel  utilizzato per inviare richieste e leggere risposte
     */
    private static int serverPort;
    /**
     * indirizzo a cui connettersi al Server
     */
    private static InetSocketAddress serverAddress;  //indirizzo e porta del Server a cui connettersi
    /**
     * Classe per gestire invio delle richieste e lettura delle resposte al/dal Server
     */
    private static ClientMessageManagement clientMessageManagement;

    /**
     * thread che ascolta sopraggiungere inviti online
     */
    private static ClientInvitesListenerThread invitesListenerThread;

    /**
     * SocketChannel utilizzato per ascoltare inviti
     */
    private static SocketChannel invitesSocket;
    /**
     * chatListenerThread attivato quando un client edita un documento e fatto terminare quando il Client
     * termina di editare il documento oppure termina per anomalia/volontariamente
     */
    private static ClientChatListener clientChatListenerThread = null;

    /**
     * Ciclo principale che si occupa di:
     * 1. aprire il client-socket per comunicare con il Server
     * 2. leggere commandi da tastiera ed inviarli, sottoforma di richieste, al Server
     * 3. attendere la risposta del Server
     * 4. chiudere il client-socket quando Client si disconette
     * @param args file di configurazione da parsare
     */
    public static void main(String[] args){

        System.out.println("--- Benvenuto in TURING (disTribUted collaboRative edItiNG) ---");
        System.out.println();

        //se il file di configurazioni è inserito al momento dell'esecuzione prendo questo
        if(args.length>0) {
            System.out.println("[Turing] >> Fase di caricamento delle configurazioni del Client");
            String confFile = args[0];

            FunctionOutcome parse = configurationsManagement.parseConf(confFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Client");
                System.exit(-1);
            }
        }
        else { //non  e' stato inserito nessun file di configurazione come argomento => parso quello di default

            //mi costruisco il path assoluto del file di configurazione
            FileManagement fileManagement = new FileManagement();
            String currentPath = fileManagement.getCurrentPath();
            defaultConfFile =  currentPath + defaultConfFile;

            FunctionOutcome parse = configurationsManagement.parseConf(defaultConfFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Client");
                System.exit(-1);
            }

            System.out.println("[Turing] >> Il client è stato eseguito con le configurazioni di default");
            System.out.println("[Turing] >> Se desideri personalizzare le configuarzioni, riesegui il codice inserendo tra gli argomenti il tuo file");
            System.out.println("[Turing] >> Per maggiori dettagli sul formato delle configurazioni, guardare il file <./data/turingClient.conf>");
        }

        //alloco le risorse estrappolate dal file di configurazione
        FunctionOutcome allocate = configurationsManagement.allocateConf();

        if(allocate == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile allocare le risorse di configurazioni del Client");
            //cancello cartella dedicata al Client in "/Turing_downloads/" e in "/Turing_edit_mode/"
            System.exit(-1);
        }

        //configurazioni settate correttamente
        //creo client-socket
        clientSocket = createSocketChannel();

        if(clientSocket == null){
            System.err.println("[ERR] >> Impossibile creare client-socket");
            System.exit(-1); //termino programma con errore
        }

        //System.out.println("[Turing] >> Client-socket creato con successo");
        //System.out.println("[Turing] >> Fase di connessione al Server");

        //connetto Client al Server
        FunctionOutcome connect = connectToServer(clientSocket, true);

        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile connettersi al Server");
            closeClienttSocketSimple(); //chiudo client-socket e segnalo errore
        }

       // System.out.println("[Turing] >> Connessione al Server avvenuta con successo");

       // System.out.println("[Turing] >> Fase di creazione delle cartelle dedicate al Client");
        String clientSocketName = getClientSocketName();

        if(clientSocketName.isEmpty()){
            System.err.println("[ERR] >> Impossibile repererire nome del clientSocket");
            closeClienttSocketSimple(); //chiudo client-socket e segnalo errore
        }

        //creo cartella dedicata al Client in "/Turing_downloads/" e in "/Turing_edit_mode/"
        configurationsManagement.setClientsDownloadsDocumentsDirectory(clientSocketName);
        configurationsManagement.setClientsEditDocumentsDirectory(clientSocketName);

        //alloco le cartelle dove salavre files e files da editare del Client
        allocate = configurationsManagement.allocateClientConf();

        if(allocate == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile allocare le risorse di configurazioni del Client");
            closeClienttSocketSimple(); //chiudo client-socket e segnalo errore
        }

        //mostro a video le configurazioni con cui è stato eseguito il client Turing
        configurationsManagement.showConf();
        System.out.println(String.format("--- Client |%s| connesso al Server ---", clientSocketName));

       // System.out.println("[Turing] >> Creazione delle cartelle dedicate al Client avvenuto con successo");
       // System.out.println("[Turing] >> Fase di creazione del thread degli inviti");

        //creo invitesSocket (SocketChannel utilizzato per ascoltare sopraggiungere inviti)
        invitesSocket = createSocketChannel();
        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile create invitesSocket");
            System.exit(-1);  //chiudo client-socket
        }

        //connetto invitesSocket al Server
        connect =  connectToServer(invitesSocket, false);

        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile connette invitesSocket al Server");
            System.exit(-1);  //chiudo client-socket
        }

        //creo invitesListerer
        invitesListenerThread = new ClientInvitesListenerThread(clientSocket, invitesSocket, configurationsManagement,
                                                                                            clientChatListenerThread);
        //attivo invitesListerer
        invitesListenerThread.start();

        //System.out.println("[Turing] >> Thread degli inviti creato con successo");
        //System.out.println("[Turing] >> Fase di creazione del ShutdownHook");

        //In concomitanza dei segnali ( SIGINT) || (SIGQUIT) || (SIGTERM) si effettuare GRACEFUL SHUTDOWN del Server, ossia:
        //1. si fa terminare thread chat
        //2. si fa terminare thread inviti
        //2. si cancellano cartelle dedicate al Client
        //3. si chiude client-socket
        //Per fare questo segnalo alla JVM che deve invocare il mio thread ShutDownHook come ultima istanza prima
        //di terminare il programma
        Runtime.getRuntime().addShutdownHook(new ClientShutdownHook(invitesListenerThread, clientMessageManagement,
                configurationsManagement, clientSocket));

        //System.out.println("[Turing] >> ShutdownHook creato con successo");
        //System.out.println("[Turing] >> Thread degli inviti creato con successo");

        //connessione avvenuta con successo => posso iniziare a:
        //1. leggere comandi da tastiera
        //2. inviare richieste al Server
        //3. attendere risposte dal Server
        startLoopRequestsAndResponses();
    }

    /**
     * Funzione che crea un SocketChannel
     * @return SocketChannel creato
     *         null se e' subentrato qualche errore nella creazione
     */
    private static SocketChannel createSocketChannel(){

        //System.out.println("[Turing] >> Fase di creazione del client-socket");

        SocketChannel client;
        try {
            client = SocketChannel.open();

            return client;

        } catch (IOException e) {
           // e.printStackTrace();
            return null; //ritorno un client-socket vuoto
        }
    }

    /**
     * Funzione che connetto un SocketChannel al Server
     * @param socket SocketChannel da connettere al Server
     * @param isClientSocket flag per differenziare clientSocket dall'inviteSocktet, perche' hanno comportamento diverso:
     *                       inviteSocket -> si connette al Server
     *                       clientSocket -> si connette al Server e fornisce al Server suo nome (indirizzo IP e porta)
     *                       per consentire successivamente all'inviteSocket di far sapere al Server che lui e' il
     *                       canele di ascolto inviti proprio di questo
     * @return SUCCESS connessione al Server avvenuta con successo clientSocket
     *         FAILURE altrimenti
     */
    private static FunctionOutcome connectToServer(SocketChannel socket, boolean isClientSocket) {

        //dal file di configurazione ricavo il serverHost e la serverPort
        serverHost = configurationsManagement.getServerHost();
        serverPort = configurationsManagement.getServerPort();
       //creo indirizzo al quale Client si puo' connettere
        serverAddress = new InetSocketAddress(serverHost, serverPort);

        try{
            //provo a connettermi al Server
            //dal file di configurazione ho ricavato il TIMEOUT della connessione (tempo massimo attesa Client prima
            //di affermare di non potersi connettere al Server)
            int connectionTimeout = configurationsManagement.getConnectionTimeout();
            socket.socket().connect(serverAddress, connectionTimeout);

            while (!socket.finishConnect()) {
                //System.out.println("[CLIENT] >> Mi sto connettendo ...");
            }

            //se ho connesso al Server il clientSocket gli devo inviare mio nome
            if(isClientSocket){

                //connessione al Server avvenuta con successo => creo istanze per scrivere richieste e leggere risposte
                clientMessageManagement = new ClientMessageManagement(clientSocket, configurationsManagement, clientChatListenerThread);

                FunctionOutcome check = clientMessageManagement.writeRequest(CommandType.I_AM_CLIENT_SOCKET, "", "");

                if(check == FunctionOutcome.FAILURE) //invio msg al Server fallito
                    System.exit(-1);  //chiudo client-socket

                return clientMessageManagement.readResponse("");
            }

            return FunctionOutcome.SUCCESS;

        } catch (IOException e) {
            // e.printStackTrace();
            return FunctionOutcome.FAILURE; //impossibile stabilire connessione / timeout scaduto
        }
    }

    /**
     * Funzione che restituisce il nome del SochetChannel connesso al Server => nome server per creare
     * cartella univoca Turing_downloads e Turing_edit_mode per il Client
     * @return nome del SocketChannel connesso al Server
     */
    private static String getClientSocketName(){
        String hostAndPort;
        try {
            hostAndPort = clientSocket.getLocalAddress().toString();
            String[] port = hostAndPort.split(":");

            return port[1];
        } catch (IOException e) {
            //e.printStackTrace();
           return "";
        }
    }

    /**
     * Funzione che si occupa di:
     *1. leggere comandi da tastiera
     *2. inviare richieste al Server
     *3. attendere risposte dal Server
     * per tutta la durata del ciclo di vita del Client (fino a quando Client non digita <<turing exit>>) o crasha per
     * anomalia
     */
    private static void startLoopRequestsAndResponses(){

        /*
          variabile nella quale memorizzo, di volta in volta, l'utente che si connette al servizio per
          personalizzare le stampe delle richieste e delle risposte ricevute dal Server
         */
        String currentUser = "";

        /*
          variabile nella quale memorizza il documento che l'utente sta editando
         */
        String documentToEdit = "";
        /*
          variabile nella quale memorizza la sezione che l'utente sta editando
         */
        int sectionToEdit = -1;

        while (true) {

            System.out.println();
            System.out.println("[Turing] >> Digita nuovo comando:");

            //leggo commando da tastiera
            ClientCommandLineManagement commandLineManagement = new ClientCommandLineManagement(configurationsManagement);
            FunctionOutcome check = commandLineManagement.readAndParseCommand(documentToEdit, sectionToEdit);

            if(check == FunctionOutcome.SUCCESS){ //commando sintatticamente corretto
                //discrimino cosa fare in base al commando digitato
                CommandType currentCommand = commandLineManagement.getCurrentCommand();
                switch(currentCommand){
                    case HELP:{
                        System.out.println();  //spazio dopo aver stampato comando di aiuto
                        continue; //digito comando successivo
                    }
                    case EXIT:{
                        System.exit(0); //chiudo client-socket e programma
                        continue;
                    }
                    case REGISTER:{
                        //registrazione al servizio tramite stub RMI
                        String currentArg1 = commandLineManagement.getCurrentArg1();
                        String currentArg2 = commandLineManagement.getCurrentArg2();

                        handleRegistration(currentArg1, currentArg2, currentUser);
                        continue;
                    }
                    case RECEIVE:{
                        if(currentUser.isEmpty()){
                            System.err.println("[ERR] >> Utente NON connesso.");
                            continue; //leggo comando successivo
                        }
                        else if(documentToEdit.isEmpty()){
                            System.err.println("[Turing] >> Non puoi visualizzare messaggi se non stai editando nessun documento.");
                            continue; //leggo comando successivo
                        }
                        else{
                            //stampo messaggi ricevuti sulla chat
                            clientMessageManagement.visualizeHistory();
                            continue;
                        }
                    }
                    case LOGIN:
                    case LOGOUT:
                    case CREATE:
                    case SHARE:
                    case SHOW_DOCUMENT:
                    case SHOW_SECTION:
                    case LIST:
                    case EDIT:
                    case END_EDIT:
                    case SEND:{

                        //in caso di LOGIN devo memorizzare username che si e' connesso per personalizzare
                        //sue stampe ed attivare invitesListerer (thraed che ascolto sopraggiungere
                        // degli inviti online)
                        if(currentCommand == CommandType.LOGIN){
                            if(!currentUser.isEmpty()){
                                System.err.println("[ERR] >> Devi prima fare il logout. Ora sei connesso come |" +
                                        currentUser + " |");
                                continue; //comando successivo
                            }
                        }

                        //recupero eventuali argomenti
                        String currentArg1 = commandLineManagement.getCurrentArg1();
                        String currentArg2 = commandLineManagement.getCurrentArg2();

                        if(currentCommand == CommandType.SEND){
                            if(currentUser.isEmpty()){
                                System.err.println("[ERR] >> Utente NON connesso.");
                                continue; //leggo comando successivo
                            }
                            else if(documentToEdit.isEmpty()){
                                System.err.println("[Turing] >> Non puoi mandare un messaggio se non stai editando nessun documento.");
                                continue; //leggo comando successivo
                            }
                            else {
                                //invio documento sulla cui chat voglio inviare msg
                                check = clientMessageManagement.writeRequest(currentCommand, documentToEdit, "");

                                if(check == FunctionOutcome.FAILURE){
                                    System.err.println("[Turing] >> Impossibile sottomettere la richiesta al Server");
                                    System.exit(-1);  //chiudo client-socket
                                }

                                //invio documento andato a buon fine
                                //invio messaggio che voglio inviare sulla chat
                                //ricavo tempo
                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                                String time = sdf.format(cal.getTime());

                                //aggiunto ora e mittente del msg, al msg
                                String msg = "    |" + time + "| " + currentUser + ": " + currentArg1;

                                //invio richiesta al Server
                                check = clientMessageManagement.writeRequest(currentCommand, msg, "");
                            }
                        }
                        else{
                            //invio richiesta al Server
                            check = clientMessageManagement.writeRequest(currentCommand, currentArg1, currentArg2);
                        }

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing] >> Impossibile sottomettere la richiesta al Server");
                            System.exit(-1);  //chiudo client-socket
                        }

                        //attendo risposta dal Server
                        check = clientMessageManagement.readResponse(currentUser);

                        if(check == FunctionOutcome.FAILURE){
                            //errore da parte dell'utente => deve digitare di nuovo commando
                            //nel caso Server si sia disconesso, Client lo capisce quando riprova a scrivere nuova
                            //richiesta ed esce
                            continue;
                        }

                        if(currentCommand == CommandType.LOGIN){
                            //memorizzousername connesso, per personalizzare le stampe
                            currentUser = commandLineManagement.getCurrentArg1();
                        }

                        if(currentCommand == CommandType.EDIT){
                            //setto documento che sto editando
                            documentToEdit = currentArg1;
                            //setto sezione che sto editando
                            sectionToEdit = Integer.parseInt(currentArg2);
                        }

                        //in caso di END-EDIT devo proveddere ad inviare versione aggiornata al Server
                        if(currentCommand == CommandType.END_EDIT){
                            System.out.println(String.format("[%s] >> Fine modifica della sezione |%s| del" +
                                    " documento |%s|", currentUser, currentArg2, currentArg1));

                            documentToEdit = ""; //resetto documento che sto editando
                            sectionToEdit = -1; //resetto sezione che sto editando
                        }

                        //in caso di LOGOUT devo resettare username connesso
                        if(currentCommand == CommandType.LOGOUT){
                            currentUser = "";  //resetto utente connesso
                            documentToEdit = ""; //resetto documento che sto editando
                            sectionToEdit = -1; //resetto sezione che sto editando
                        }
                    }
                }
            }
        }
    }

    /**
     * Funzione che si occupa di reperire lo stub RMI messo a disposizione dal Server e di effettuare
     * la chiamata del metodo remoto "registerTask" con il quale si tenta di registrare un nuovo utente
     * al servizio. Il metodo si occupa anche di stampare l'esito dell'operazione a schermo.
     * @param username nome utente
     * @param password password utente
     * @param currentUser utente, eventuale, attualmente connesso
     * @return SUCCESS se e' stato possibile registrare tramiet RMI l'utente al servizio
     *         FAILURE altrimenti
     */
    public static FunctionOutcome handleRegistration(String username, String password, String currentUser){

        Registry registry;
        try {
            //recupero Registro locale del Server
            registry = LocateRegistry.getRegistry(configurationsManagement.getServerHost(),
                    configurationsManagement.getRMIPort());

            //recupero stub (riferimento oggetto remoto del Server)
            TuringRegistrationRMIInterface stub;
            try {
                //provo a recuperare lo stub associato alla chiave univoca "TURING-RMI-REGISTRATION"
                stub = (TuringRegistrationRMIInterface) registry.lookup("TURING-RMI-REGISTRATION");

            } catch (NotBoundException e) {
                //e.printStackTrace();
                System.err.println("[ERR] >> Impossibile recuperare lo stub RMI per effettuare la registrazione");
                return FunctionOutcome.FAILURE;

            }

            //stub recuperato con successo => invoco metodo remoto per registrarmi
            ServerResponse serverResponse = stub.registerTask(username, password, currentUser);

            switch (serverResponse){
                case OP_OK:{
                    System.out.println(String.format("[Turing] >> Registrazione dell'utente |%s| avvenuta con " +
                            "successo", username));
                    break;
                }
                case OP_USERNAME_INAVLID_CHARACTERS:{
                    System.err.println(String.format("[ERR] >> Username |%s| NON valido. Il nome utente non puo' contentere" +
                            ": ['\\\\', '/', ':', '*', '?', '\"', '<', '>', '|']", username));
                    break;
                }
                case OP_USERNAME_ALREADY_TAKEN:{
                    System.err.println(String.format("[ERR] >> Username |%s| GIA' in uso.", username));
                    break;
                }
                case OP_USER_MUST_LOGOUT:{
                    System.err.println("[ERR] >> Devi prima fare il logout per poterti registrate con un altro account.");
                    break;
                }
            }

            return FunctionOutcome.SUCCESS; //notifico al ciclo principale che la lettura della risposta e' andata a buon fine

        } catch (RemoteException e) {
            //e.printStackTrace();
            System.err.println("[ERR] >> IMpossibile completare la rgistrazione tramite stub RMI");
            return FunctionOutcome.FAILURE;
        }
    }

    /**
     * Funzione che si occcupa di chiuede il client-socket se subentrano problemi
     * in fase di caricamento del Client
     */
    private static void closeClienttSocketSimple(){
        try {
            clientSocket.close();

            System.exit(-1);
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("[ERR] >> Errore chiusura clientSocket");
            System.exit(-1);
        }
    }
}
