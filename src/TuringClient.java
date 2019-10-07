import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class TuringClient {
    private static String defaultConfFile = "/src/data/turingClient.conf";
    private static ClientConfigurationManagement configurationsManagement = new ClientConfigurationManagement();
    private static RMIRegistrationHandler rmiRegistrationHandler = new RMIRegistrationHandler();
    private static MulticastChatHandler multicastChatHandler = new MulticastChatHandler();
    private static SocketChannel clientSocket = null;      //client-socket
    private static String serverHost;
    private static int serverPort;
    private static InetSocketAddress serverAddress;  //indirizzo e porta del Server a cui connettersi

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
            FunctionOutcome parse = configurationsManagement.parseConf(defaultConfFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Client");
                System.exit(-1);
            }

            System.out.println("[Turing] >> Il client è stato eseguito con le configurazioni di default");
            System.out.println("[Turing] >> Se desidi personalizzare le configuarzioni, riesegui il codice inserendo tra gli argomenti il tuo file");
            System.out.println("[Turing] >> Per maggiori dettagli sul formato delle configurazioni, guardare il file <./data/turingClient.conf>");
        }

        //mostro a video le configurazioni con cui è stato eseguito il client Turing
        configurationsManagement.showConf();

        //alloco le risorse estrappolate dal file di configurazione
        FunctionOutcome allocate = configurationsManagement.allocateConf();

        if(allocate == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile allocare le risorse di configurazioni del Client");
            System.exit(-1);
        }

        System.out.println("[Turing] >> Caricamento delle configurazioni avvenuto con successo");

        //configurazioni settate correttamente
        //creo client-socket
        clientSocket = createClientSocket();

        if(clientSocket == null){
            System.err.println("[ERR] >> Impossibile creare client-socket");
            System.exit(-1); //termino programma con errore
        }

        System.out.println("[Turing] >> Client-socket creato con successo");

        //connetto Client al Server
        FunctionOutcome connect = connectToServer();

        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile connettersi al Server");
            closeClientSocket(); //chiudo client-socket e programma
        }

        System.out.println("[Turing] >> Connessione al Server avvenuta con successo");
        System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
        System.out.println("[Turing] >> turing --help");

        //connessione avvenuta con successo => posso iniziare a:
        //1. leggere comandi da tastiera
        //2. inviare richieste al Server
        //3. attendere risposte dal Server
        System.out.println("[Turing] >> Digita nuovo comando:");
        startLoopRequestsAndResponses();
    }

    /**
     * Funzione che crea il client-socket
     * @return SocketChannel client-socket che client utilizzera' per connettersi al Server
     *         null se e' subentrato qualche errore nella creazione del client-socket
     */
    private static SocketChannel createClientSocket(){

        System.out.println("[Turing] >> Fase di creazione del client-socket");

        SocketChannel client;
        try {
            client = SocketChannel.open();

            return client;

        } catch (IOException e) {
            e.printStackTrace();
            return null; //ritorno un client-socket vuoto
        }
    }

    /**
     * Funzione che connetto Client al Server
     * @return SUCCESS connessione al Server avvenuta con successo
     *         FAILURE altrimenti
     */
    private static FunctionOutcome connectToServer() {

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
            clientSocket.socket().connect(serverAddress, connectionTimeout);

            while (!clientSocket.finishConnect()) {
                System.out.println("[CLIENT] >> Mi sto connettendo ...");
            }

            return FunctionOutcome.SUCCESS;

        } catch (IOException e) {
            e.printStackTrace();
            return FunctionOutcome.FAILURE; //impossibile stabilire connessione / timeout scaduto
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

        while (true) {

            //leggo commando da tastiera
            ClientCommandLineManagement commandLineManagement = new ClientCommandLineManagement();
            FunctionOutcome check = commandLineManagement.readAndParseCommand();

            if(check == FunctionOutcome.SUCCESS){ //commando sintatticamente corretto
                //discrimino cosa fare in base al commando digitato
                CommandType currentCommand = commandLineManagement.getCurrentCommand();
                switch(currentCommand){
                    case HELP:{
                        System.out.println();  //spazio dopo aver stampato comando di aiuto
                        System.out.println("[Turing] >> Digita nuovo comando:");
                        continue; //digito comando successivo
                    }
                    case REGISTER:{
                        //registro Client al Server tramite stub RMI
                    }
                    case LOGIN:{}
                    case LOGOUT:{}
                    case CREATE:{}
                    case SHARE:{}
                    case SHOW_DOCUMENT:{}
                    case SHOW_SECTION:{}
                    case LIST:{}
                    case EDIT:{}
                    case END_EDIT:{}
                    case SEND:{
                        //multicast
                    }
                    case RECEIVE:{
                        //multicast
                    }
                    case EXIT:{
                        closeClientSocket(); //chiudo client-socket e programma
                    }
                    default: //TUTTI CASI SOPRA VUOTI @TODO
                        //recupero eventuali argomenti
                        String currentArg1 = commandLineManagement.getCurrentArg1();
                        String currentArg2 = commandLineManagement.getCurrentArg2();

                        //invio richiesta al Server
                        check = writeRequest(currentCommand, currentArg1, currentArg2);

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing >> Impossibile sottomettere la richiesta al Server]");
                           System.exit(-1);
                        }

                        //attendo risposta dal Server
                        check = readResponse(currentCommand, currentArg1, currentArg2);

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing >> Impossibile reperire la risposta del Server]");
                            System.exit(-1);
                        }
                }
            }
        }
    }

    /**
     * Funzione che si occupa di inviare:
     * 1. la dimensionde della richiesta al Server (per consentirgli di allocare un buffer di lettura opportuno)
     * 2. la richiesta al Server
     * @param command tipo di richiesta
     * @param arg1 eventuale primo argomento
     * @param arg2 eventulae secondo argomento
     * @return SUCCESS se l'invio della dimensione delle richiesta e la richiesta sono andati a buon fine
     *         FAILURE altrimenti
     */
    private static FunctionOutcome writeRequest(CommandType command, String arg1, String arg2){
        ByteBuffer buffer = ByteBuffer.allocate(4); //contiene dim. richiesta Client => intero => codificato con 4 bytes

        buffer.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        //costruisco risposta
        //inserisco degli spazi per distinguere componenti della richiesta
        String request = command.ordinal() + " " + arg1 + " " + arg2;  //ordinale() => reperisco valore numerico enum
        int requestLength = request.length(); //ricavo lunghezza della richiesta

        buffer.putInt(requestLength); //inserisco dim. richiesta nel buffer

        buffer.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        //invio  dimensione richiesta al Server
        while(buffer.hasRemaining()){
            try {
                clientSocket.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                return FunctionOutcome.FAILURE;
            }
        }

        //invio dim. richiesta avvenuta con successo
        //alloco nuovo buffer per inserirvi richiesta
        buffer = ByteBuffer.allocate(requestLength);

        buffer.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        buffer.put(request.getBytes());  //inserisco messaggio di richiesta nel buffer

        buffer.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        //invio richiesta al Server
        while(buffer.hasRemaining()){
            try {
                clientSocket.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                return FunctionOutcome.FAILURE;
            }
        }

        return FunctionOutcome.SUCCESS; //invio dim. richiesta e richiesta avenuto con successo
    }

    /**
     * Funzione che si occupa di leggere:
     * 1. la dimensionde della risposta del Server (per allocare un buffer di lettura opportuno)
     * 2. la risposta al Server
     * @param command tipo di richiesta
     * @param arg1 eventuale primo argomento
     * @param arg2 eventulae secondo argomento
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    private static FunctionOutcome readResponse(CommandType command, String arg1, String arg2){
        ByteBuffer buffer = ByteBuffer.allocate(4); //contiene dim. richiesta Client => intero => codificato con 4 bytes

        buffer.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        int responseLength = 0;  //lunghezza della risposta

        //leggo dimensione risposta
        try {
            while (clientSocket.read(buffer) > 0) {
                responseLength = buffer.getInt();
            }
        }catch (IOException e) {
            e.printStackTrace();
            return FunctionOutcome.FAILURE;
        }

        //alloco nuovo buffer per leggervi risposta, di dimensione letta sopra
        buffer = ByteBuffer.allocate(responseLength);

        buffer.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        String respone = "";

        //leggo risposta
        try {
            while (clientSocket.read(buffer) > 0) {
                buffer.flip();  //modalita' lettura (position=0, limit = bytesWritten)
                respone = new String(buffer.array(), StandardCharsets.UTF_8);
                buffer.clear(); //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)
            }
        }catch (IOException e) {
            e.printStackTrace();
            return FunctionOutcome.FAILURE;
        }
        return manageResponse(respone, command, arg1, arg2); //lettura risposta avvenuta con sucesso => stampa personalizzata
    }

    /**
     * Funzione che stampa la risposta personalizzata del Server alla richiesta fatta dal Client
     * @param respone risposta del Server (sottofroma di enum "ServerResponse")
     * @param command tipo di richiesta
     * @param arg1 eventuale primo argomento
     * @param arg2 eventulae secondo argomento
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     */
    private static FunctionOutcome manageResponse(String respone, CommandType command, String arg1, String arg2){

        //a seconda del comando richiesto posso avere differenti risposte di successo/fallimento da parte del Server
        // verifico che cosa mi ha risposto il Server  ed in base a cio', stampo un messaggio di esito
        ServerResponse serverResponse = ServerResponse.valueOf(respone);  //converto stringa in enum
        switch (serverResponse){
            case OP_OK:{  //discrimino quale operazione ha avuto successo
                switch (command){
                    case REGISTER: {
                        System.out.println(String.format("[Turing] >> Registrazione dell'utente |%s| avvenuta con " +
                                                                                                    "successo", arg1));
                    }
                    case LOGIN:{
                        System.out.println("[Turing] >> Login avvenuto con successo");
                    }
                    case LOGOUT:{
                        System.out.println(String.format("[Turing] >> Logout dell'utente |%s| avvenuto con " +
                                                                                                    "successo", arg1));
                    }
                    case CREATE:{
                        System.out.println(String.format("[Turing] >> Creazione del documento |%s| con |%s| sezioni" +
                                                                                " avvenuta con successo", arg1, arg2));
                    }
                    case SHARE:{
                        System.out.println(String.format("[Turing] >> Condivisione del documento |%s| con l'utente " +
                                                                            "|%s| avvenuto con successo", arg1, arg2));
                    }
                    case SHOW_DOCUMENT:{
                        System.out.println(String.format("[Turing] >> Contenuto del documento |%s|:", arg1));
                        System.out.println(respone);  //@TODO SPLIT PER STAMPARE CONTENUTO
                    }
                    case SHOW_SECTION:{
                        System.out.println(String.format("[Turing] >> Contenuto della sezione |%s| del documento" +
                                                                                                " |%s|:", arg2, arg1));
                        System.out.println(respone);  //@TODO SPLIT PER STAMPARE CONTENUTO
                    }
                    case LIST:{
                        System.out.println("[Turing] >> Recap tuoi documenti");
                        System.out.println(respone);  //@TODO SPLIT PER STAMPARE CONTENUTO
                    }
                    case EDIT:{
                        System.out.println(String.format("[Turing] >> Inizio modifica della sezione |%s| del" +
                                                                                        " documento |%s|", arg2, arg1));
                    }
                    case END_EDIT:{
                        System.out.println(String.format("[Turing] >> Fine modifica della sezione |%s| del" +
                                                                                        " documento |%s|", arg2, arg1));
                    }
                    case SEND:{
                        System.out.println("[Turing] >> Invio messaggio sulla Chat avvenuto corretamente");
                    }
                    case RECEIVE:{
                        System.out.println("[Turing] >> Hai un nuovo messaggio sulla Chat");
                        System.out.println(respone);  //@TODO SPLIT PER STAMPARE CONTENUTO
                    }
                }
            }
            case OP_USER_NOT_ONLINE:{
                System.err.println(String.format("[ERR] >> Utente |%s| NON connesso.", arg1));
            }
            case OP_USER_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Utente |%s| NON registrato.", arg1));
            }
            case OP_DOCUMENT_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| NON esistente.", arg1));
            }
            case OP_SECTION_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Sezione |%s| del documento |%s| NON " +
                                                                                            "registrato.", arg2, arg1));
            }
            case OP_USERNAME_ALREADY_TAKEN:{
                System.err.println(String.format("[ERR] >> Username |%s| GIA' in uso.", arg1));
            }
            case OP_USER_MUST_LOGOUT:{
                System.err.println("[ERR] >> Devi prima fare il logout per poterti registrate con un altro account.");
            }
            case OP_USER_ALREADY_ONLINE:{
                System.err.println(String.format("[ERR] >> Username |%s| GIA' connesso.", arg1));
            }
            case OP_USERNAME_INCORRECT:{
                System.err.println(String.format("[ERR] >> Username |%s| ERRATO.", arg1));
            }
            case OP_PASSWORD_INCORRECT:{
                System.err.println(String.format("[ERR] >> Password |%s| ERRATO.", arg2));
            }
            case OP_DOCUMENT_ALREADY_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| GIA' esistente.", arg1));
            }
            case OP_USER_NOT_CREATOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                                            " documento |%s|. Per farlo devi essere il suo creatore.", arg2, arg1));
            }
            case OP_USER_IS_DEST:{
                System.err.println(String.format("[ERR] >> Non puoi invitare te stesso a collaborare al documento |%s|.", arg1));
            }
            case OP_DEST_ALREADY_CONTRIBUTOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|, perche' e' gia' collaboratore / collaboratore.", arg2, arg1)); //@TODO VERIFICARE CHE SIA COLLABORATORE
            }
            case OP_DEST_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|. L'utente NON e' registrato alla nostra piattaforma.", arg2, arg1));
            }
            default:
                //return FunctionOutcome.FAILURE;
        }

        return FunctionOutcome.SUCCESS; //notifico al ciclo principale che la lettura della risposta e' andata a buon fine
    }

    /**
     * Funzione che fa terminare il Client, chiudendo il suo client-socket quando sopraggiunge il comando:
     * <<turing exit>>
     */
    private static void closeClientSocket(){
        try {
            clientSocket.close();

            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERR] >> Impossibile chiudere client-socket");
            System.exit(-1);
        }
    }
}
