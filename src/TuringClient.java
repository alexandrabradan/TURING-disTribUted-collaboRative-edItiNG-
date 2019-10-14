import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TuringClient {
    private static String defaultConfFile = "/src/data/turingClient.conf";
    private static ClientConfigurationManagement configurationsManagement = new ClientConfigurationManagement();
    private static RMIRegistrationHandler rmiRegistrationHandler = new RMIRegistrationHandler();
    private static MulticastChatHandler multicastChatHandler = new MulticastChatHandler();
    private static SocketChannel clientSocket = null;      //client-socket
    private static String serverHost;
    private static int serverPort;
    private static InetSocketAddress serverAddress;  //indirizzo e porta del Server a cui connettersi

    private static ClientMessageManagement clientMessageManagement;

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

        System.out.println("[Turing] >> Fase di connessione al Server");

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

            //connessione al Server avvenuta con successo => creo istanze per scrivere richieste e leggere risposte
            clientMessageManagement = new ClientMessageManagement(clientSocket);

            return FunctionOutcome.SUCCESS;

        } catch (IOException e) {
            // e.printStackTrace();
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

            System.out.println();
            System.out.println("[Turing] >> Digita nuovo comando:");

            //leggo commando da tastiera
            ClientCommandLineManagement commandLineManagement = new ClientCommandLineManagement();
            FunctionOutcome check = commandLineManagement.readAndParseCommand();

            if(check == FunctionOutcome.SUCCESS){ //commando sintatticamente corretto
                //discrimino cosa fare in base al commando digitato
                CommandType currentCommand = commandLineManagement.getCurrentCommand();
                switch(currentCommand){
                    case HELP:{
                        System.out.println();  //spazio dopo aver stampato comando di aiuto
                        continue; //digito comando successivo
                    }
                    case EXIT:{
                        closeClientSocket(); //chiudo client-socket e programma
                    }
                    case REGISTER:{
                        //registrazione al servizio tramite stub RMI avvenuta => commando seguente
                        continue;
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
                    case SEND:        //multicast
                    case RECEIVE:{   //multicast

                        //recupero eventuali argomenti
                        String currentArg1 = commandLineManagement.getCurrentArg1();
                        String currentArg2 = commandLineManagement.getCurrentArg2();

                        //invio richiesta al Server
                        check = clientMessageManagement.writeRequest(currentCommand, currentArg1, currentArg2);

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing >> Impossibile sottomettere la richiesta al Server]");
                            closeClientSocket();
                        }

                        //attendo risposta dal Server
                        check = clientMessageManagement.readResponse();

                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing >> Impossibile reperire la risposta del Server]");
                            closeClientSocket();
                        }
                    }
                }
            }
        }
    }

    /**
     * Funzione che fa terminare il Client, chiudendo il suo client-socket quando sopraggiunge il comando:
     * <<turing exit>>
     */
    private static void closeClientSocket(){
        //controllo che il SocketChannel sia stato inizializzato, altrimenti esco semplicemente
        if(clientSocket != null){
            try {

                clientSocket.close();
                System.out.println("[Turing] >> Client-socket chiuso");

                System.exit(0);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("[ERR] >> Impossibile chiudere client-socket");
                System.exit(-1);
            }
        }
        else System.exit(0);
    }
}
