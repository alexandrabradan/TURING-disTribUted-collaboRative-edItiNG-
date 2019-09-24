import java.net.InetSocketAddress;
import java.net.Socket;

public class TuringClient {
    private static ConfigurationsManagement configurationsManagement = new ConfigurationsManagement();
    private static Socket client = new Socket();
    private static InetSocketAddress serverAddress;

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

        //connetto Client al Server
        FunctionOutcome connect = connectToServer();

        if(connect == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile connettersi al Server");
            System.exit(0); //non segnalo errore perche' non e' un errore interno
        }

        //connessione avvenuta con successo => posso iniziare a:
        //1. leggere comandi da linea di comando
        //2. inviare richieste al Server
        //3. attendere risposte dal Server
        requestsAndResponsesHandle();
    }

    /**
     * Funzione che stampa un messaggio di aiuto qualora il Client lo richieda tramite il comando:
     * >> usage turing
     *
     */
    private static void printHelp() {
        System.out.println("usage: turing COMMAND [ARGS...]");
        System.out.println();
        System.out.println("COMMANDS: ");
        System.out.println("	register <username> <password> | Registra l'user");
        System.out.println("	login <username> <password>    | Connette l'user");
        System.out.println("	logout                         | Disconnette l'user");
        System.out.println();
        System.out.println("	create <doc> <numsezioni>      | Crea un documento");
        System.out.println("	share <doc> <username>         | Condivide un documento");
        System.out.println("	show <doc> <sec>               | Mostra una sezione del documento");
        System.out.println("	show <doc>                     | Mostra l'intero documento");
        System.out.println("	list                           | Mostra la lista dei documenti");
        System.out.println();
        System.out.println("	edit <doc> <sec>               | Modifica una sezione del documento");
        System.out.println("	end-edit					   | Fine modifica della sezione del documento");
        System.out.println();
        System.out.println("	send <msg>                     | Invia un messaggio sulla chat");
        System.out.println("	receive                        | Visualizza i messaggi ricevuti sulla chat");
    }


    /**
     * Funzione che crea il client-socket e che tenta di stabilire una connessione con il Server
     * @return SUCCESS se la connessione con il Server ha avuto successo
     *         FAILURE se non e' stato possibile stabilire la connessione con il Server:
     *                 - Server inattivo
     *                 - scadenza TIMEOUT
     */
    private static FunctionOutcome connectToServer(){

        //verifico che il Server abbia fatto il parsing del file di configurazione
        FunctionOutcome check = configurationsManagement.checkConf();

        if(check == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> File di configurazione non trovato oppure configurazioni mancanti");
            return FunctionOutcome.FAILURE;
        }

        //dal file di configurazione ho ricavato il serverHost e la serverPort => creo indirizzo al quale Client
        //si puo' connettere
        serverAddress = new InetSocketAddress(configurationsManagement.serverHost, configurationsManagement.serverPort);

        return FunctionOutcome.SUCCESS;
    }

    private static void requestsAndResponsesHandle(){

    }
}
