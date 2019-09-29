import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TuringClient {
    //istanza classe che si occupa del file di configurazione e delle variabili di configurazione
    private static ConfigurationsManagement configurationsManagement = new ConfigurationsManagement();
    private static SocketChannel clientSocket;      //client-socket
    private static String serverHost;
    private static int serverPort;
    private static InetSocketAddress serverAddress;  //indirizzo e porta del Server a cui connettersi

    /**
     * Ciclo principale che si occupa di:
     * 1. aprire il client-socket per comunicare con il Server
     * 2. leggere commandi da tastiera ed inviarli, sottoforma di rrichieste, al Server
     * 3. attendere la risposta del Server
     * 4. chiudere il client-socket quando Client si disconette
     * @param args file di configurazione da parsare
     */
    public static void main(String[] args){

        System.out.println("--- Benvenuto in TURING (disTribUted collaboRative edItiNG) ---");
        System.out.println();

        //creo client-socket
        clientSocket = createClientSocket();

        if(clientSocket == null){
            System.err.println("[ERR] >> Impossibile creare client-socket");
            System.exit(-1);
        }

        //connetto Client al Server
        FunctionOutcome connect = connectToServer();

        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile connettersi al Server");
            System.exit(-1);
        }

        System.out.println("[Turing] >> Connessione avvenuta con successo");
        System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
        System.out.println("[Turing] >> turing --help");
        System.out.println("[Turing] >> Digita nuovo comando:");

        //connessione avvenuta con successo => posso iniziare a:
        //1. leggere comandi da tastiera
        //2. inviare richieste al Server
        //3. attendere risposte dal Server
        startLoopRequestsAndResponses();
    }

    /**
     * Funzione che crea il client-socket e che tenta di stabilire una connessione con il Server
     * @return SocketChannel client-socket che client utilizzera' per connettersi al Server
     */
    private static SocketChannel createClientSocket(){

        //verifico che il Server abbia fatto il parsing del file di configurazione
        FunctionOutcome check = configurationsManagement.checkConf();

        if(check == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> File di configurazione non trovato oppure configurazioni mancanti");
            System.err.println("[ERR] >> Impossibile connettersi al Server");
            System.exit(-1);
        }

        SocketChannel client;
        try {
            client = SocketChannel.open();

            return client;

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null; //ritorno un client-socket vuoto (situazione che non si verifica per try-catch soprastante)
    }


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
            System.err.println("[ERR] >> Impossibile aprire client-socket");
            return FunctionOutcome.FAILURE;
        }
    }

    /**
     * Funzione che si occupa di:
     *1. leggere comandi da tastiera
     *2. inviare richieste al Server
     *3. attendere risposte dal Server
     * per tutta la durata del ciclo di vita del Client
     */
    private static void startLoopRequestsAndResponses(){

        while (true) {
            //leggo commando da tastiera
            CommandLineManagement commandLineManagement = new CommandLineManagement();
            FunctionOutcome check = commandLineManagement.readAndParseCommand();

            if(check == FunctionOutcome.SUCCESS){ //commando sintatticamente corretto

                //discrimino se Client ha richiesto comando di aiuto (gia' gestito da CommandLineManagement);
                if(commandLineManagement.getCurrentCommand() == CommandType.HELP){
                    System.out.println();
                    System.out.println("[Turing] >> Digita nuovo comando:");
                    continue; //digito comando successivo
                }

                //recupero comando corrente ed eventuali argomenti
                CommandType currentCommand = commandLineManagement.getCurrentCommand();
                String currentArg1 = commandLineManagement.getCurrentArg1();
                String currentArg2 = commandLineManagement.getCurrentArg2();

                //invio richiesta al Server
                check = writeRequest(currentCommand, currentArg1, currentArg2);

                if(check == FunctionOutcome.FAILURE){
                    continue; //digito comando successivo
                }

                //attendo risposta dal Server
                check = readResponse();

                if(check == FunctionOutcome.FAILURE){
                    continue; //digito comando successivo
                }
            }
        }
    }

    private static FunctionOutcome writeRequest(CommandType command, String arg1, String arg2){

        //creo messaggio di richiesta

        //invio richiesta al Server
        return FunctionOutcome.SUCCESS;
    }

    private static FunctionOutcome readResponse(){
        //invio richiesta al Server
        return FunctionOutcome.SUCCESS;

    }
}
