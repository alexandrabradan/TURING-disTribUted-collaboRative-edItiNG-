import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ServerConfigurationsManagement {

    /**
     * DNS name server
     */
    private String serverHost;
    /**
     * porta su cui Server e' in ascolto
     */
    private int serverPort;
    /**
     * porta utilizzata per gli inviti
     */
    private int RMIPort;
    /**
     * porta utilizzata per i gruppi di chat
     */
    private int multicastPort;
    /**
     * tempo attesa multicast
     */
    private int connectionTimeout;
    /**
     * numero di threads nel ThreadPool
     */
    private int numWorkersInThreadPool;
    /**
     * path della directory dove Server salva documenti dei Clients
     */
    private String serverSaveDocumentsDirectory;


    /**
     * Classe che si occupa di gestire le operazioni sui files
     */
    private FileManagement fileManagement = new FileManagement();
    private String currentPath = fileManagement.getCurrentPath();

    /**
     * Costruttore della classe ServerConfigurationsManagement
     */
    public ServerConfigurationsManagement(){
        this.serverHost = "";
        this.serverPort = -1;
        this.RMIPort = -1;
        this.multicastPort = -1;
        this.connectionTimeout = -1;
        this.numWorkersInThreadPool = -1;
        this.serverSaveDocumentsDirectory = "";
    }

    /**
     * Funzione che resttuisce il DNS Name Server
     * @return DNS Name Server
     */
    public String getServerHost(){
        return this.serverHost;
    }

    /**
     * Funzione che restituisce la porta su cui Server e' in ascolto
     * @return porta su cui Server e' in ascolto
     */
    public int getServerPort(){
        return this.serverPort;
    }

    /**
     * Funzione che restituisce la
     * @return porta utilizzata per gli inviti
     */
    public int getRMIPort(){
        return this.RMIPort;
    }

    /**
     * Funzione che restituisce la porta utilizzata per i gruppi di chat
     * @return porta utilizzata per i gruppi di chat
     */
    public int getMulticastPort(){
        return this.multicastPort;
    }

    /**
     * Funzione che restituisce tempo attesa Client prima di affermare di non potersi connettere al Server
     * @return tempo attesa Client prima di affermare di non potersi connettere al Server
     */
    public int getConnectionTimeout(){
        return this.connectionTimeout;
    }

    /**
     * Funzione che restituisce numero di threads nel ThreadPool
     * @return numero di threads nel ThreadPool
     */
    public int getNumWorkersInThreadPool(){
        return this.numWorkersInThreadPool;
    }

    /**
     * Funzione che restituisce path della directory dove Server salva documenti dei Clients
     * @return path della directory dove Server salva documenti dei Clients
     */
    public String getServerSaveDocumentsDirectory(){
        return this.serverSaveDocumentsDirectory;
    }

    /**
     * Funzione che fa il parsing del file di configurazione passato come argomento
     * @param confFile path del file di configurazione da parsare
     * @return SUCCESS se il parsing e' andato a buon fine e tutte le variabili di configurazione sono lecite
     *         FAILURE se il file non esiste oppure ci sono stati dei problemi con il parsing/ valori parsati
     */
    public FunctionOutcome parseConf(String confFile){

        //confFile = currentPath + confFile;

        //verifico se il file passato come paramentro esiste
        boolean exist = this.fileManagement.checkEsistenceFile(confFile);

        if(!exist){
            System.err.println("[ERR] >> file di configurazione = " + confFile + " NON esistente");
            return FunctionOutcome.FAILURE;
        }

        //file esiste
        //provo a farne il parsing:
        //1. non considero le righe che sono vuote
        //2. non considero le righe commentate (iniziano con #)
        //3. verifico che le righe che non sono vuote e che non sono comentate contegano come parola chiave una delle
        //variabili di configurazione
        Path path = Paths.get(confFile);
        try {
            List<String> lines = null; //.get(0);

            lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            for (String line : lines) {
               if(!line.isEmpty() && !line.startsWith("#")){
                   String delimiter = "=";
                   String[] splitString = line.split(delimiter);
                   String key = splitString[0].trim(); //rimuovo spazi vuoti inizio e fine stringa
                   String value = splitString[1].trim(); //rimuovo spazi vuoti inizio e fine stringa

                   switch (key) {
                       case "serverHost":
                           this.serverHost = value;
                           break;
                       case "serverPort":
                           this.serverPort = Integer.parseInt(value);
                           break;
                       case "RMIPort":
                           this.RMIPort = Integer.parseInt(value);
                           break;
                       case "multicastPort":
                           this.multicastPort = Integer.parseInt(value);
                           break;
                       case "connectionTimeout":
                           this.connectionTimeout = Integer.parseInt(value);
                           break;
                       case "numWorkersInThreadPool":
                           this.numWorkersInThreadPool = Integer.parseInt(value);
                           break;
                       case "serverSaveDocumentsDirectory":
                           value = currentPath + value;
                           //value = currentPath + "/src" + value;
                           this.serverSaveDocumentsDirectory = value;
                           break;
                       default:
                           break;
                   }
               }
            }
        } catch (IOException e) {
                //e.printStackTrace();
                System.err.println("[ERR] >> Errore nel leggere file di configurazione = " + confFile);
                return FunctionOutcome.FAILURE;
        }
        // verifico se tutte le variabili di configurazione sono state inizializzate e se i valori sono leciti
        return checkConf();
    }

    /**
     * Funzione che verifica se le variabili di configurazione sono state inizializzate e se lo sono state, se i valori
     * sono ammissibili
     * @return SUCCESS se tutte le variabili di configurazione sono state inizializzate ed hanno valori leciti
     *         FAILURE se c'e' almeno una variabile di configurazione non inizializzata / inizializzata con un valore non
     *                 lecito
     */
    public FunctionOutcome checkConf(){
        if(this.serverHost.isEmpty()){
            System.err.println("[ERR] >> serverHost non inizializzao ");
            return FunctionOutcome.FAILURE;
        }
        else if(!this.serverHost.equals("localhost") && !this.serverHost.equals("127.0.0.1.")){
            System.err.println("[ERR] >> serverHost = " + this.serverHost + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.serverPort <= 1024){
            System.err.println("[ERR] >> serverPort = " + this.serverPort + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.RMIPort <= 1024){
            System.err.println("[ERR] >> RMIPort = " + this.RMIPort + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.multicastPort <= 1024){
            System.err.println("[ERR] >> multicastPort = " + this.multicastPort + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.connectionTimeout < 0){
            System.err.println("[ERR] >> connectionTimeout = " + this.connectionTimeout + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.numWorkersInThreadPool < 0){
            System.err.println("[ERR] >> numWorkersInThreadPool = " + this.numWorkersInThreadPool + " non valido");
            return FunctionOutcome.FAILURE;
        }
        else if(this.serverSaveDocumentsDirectory.isEmpty()){
            System.err.println("[ERR] >> serverSaveDocumentsDirectory non inizializzao ");
            return FunctionOutcome.FAILURE;
        }

        //variabili di configurazione inizializzate e lecite
        return FunctionOutcome.SUCCESS;
    }

    /**
     * Funzione che si occupa di allocare le risorse a seguito del parsing del file di configurazione.
     * In particolare:
     * 1. verifica se esistenza della cartella Turing_database, creandola altrimenti
     * 2. se le cartella soprastante esisteva gia', la svuota
     * N.B. NO PERSISTENZA DATI TRA UNA CONNESSIONE ED UN'ALTRA => PROTOCOLLO STATELESS
     * @return SUCCESS se cartella sono state svuotate, oppure se non esistevano sono state create
     *         FAILURE se la creazione di una cartella oppure lo svuotamento di una di esse non ha avuto successo
     */
    public FunctionOutcome allocateConf(){
        FunctionOutcome check1 = this.fileManagement.createDirectory(this.serverSaveDocumentsDirectory);

        if(check1 == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile creare cartella di configurazione");
            return FunctionOutcome.FAILURE;
        }

        return FunctionOutcome.SUCCESS; //creazione/svuotamento 2 cartelle andato a buon fine
    }

    /**
     * Funzione che si occupa di cancellare la cartella del Server
     */
    public void deallocateServerConf(){
        fileManagement.deleteDirectory(this.serverSaveDocumentsDirectory);
        this.fileManagement.createDirectory(this.serverSaveDocumentsDirectory);
    }

    /**
     * Funzione che stampa le variabili di configurazione estrappolate dal file di configurazione
     */
    public void showConf(){
        System.out.println();
        System.out.println("[Turing] >> Configurazioni con cui si stanno eseguendo il Server:");
        System.out.println("- Nome del Server = " + this.serverHost );
        System.out.println("- Porta di registrazione = " + this.serverPort );
        System.out.println( "- Porta utilizzata per gli inviti = " + this.RMIPort);
        System.out.println( "- Porta utilizzata per gli indirizzi di multicast = " + this.multicastPort);
        System.out.println("- Valore del Timeout = " + this.connectionTimeout);
        System.out.println("- Dimensione del ThreadPool = " + this.numWorkersInThreadPool);
        System.out.println("- Directory dove andare a salvare i file = " + this.serverSaveDocumentsDirectory);
        System.out.println();
    }
}
