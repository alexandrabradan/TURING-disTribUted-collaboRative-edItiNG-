import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TuringServer {
    /**
     * path relativo del file di configurazione del Server
     */
    private static String defaultConfFile = "/data/turingServer.conf";
    //private static String defaultConfFile = "/src/data/turingServer.conf";
    /**
     * classe che si occupa di fare il parsing del file di configurazione e memorizzarne i valori
     */
    private static ServerConfigurationsManagement configurationsManagement = new ServerConfigurationsManagement();

    /**
     * Ciclo di attivazione del Server
     */
    public static void main(String[] args){
        System.out.println("[Turing] >> SERVER TURING (disTribUted collaboRative edItiNG) AVVIATO");
        System.out.println();


        //se il file di configurazioni è inserito al momento dell'esecuzione prendo questo
        if(args.length>0) {
            System.out.println("[Turing] >> Fase di caricamento delle configurazioni del Server");
            String confFile = args[0];

            FunctionOutcome parse = configurationsManagement.parseConf(confFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Server");
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
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Server");
                System.exit(-1);
            }

            System.out.println("[Turing] >> Il server è stato eseguito con le configurazioni di default");
            System.out.println("[Turing] >> Se desideri personalizzare le configuarzioni, riesegui il codice inserendo tra gli argomenti il tuo file");
            System.out.println("[Turing] >> Per maggiori dettagli sul formato delle configurazioni, guardare il file <./data/turingServer.conf>");
        }

        //mostro a video le configurazioni con cui è stato eseguito il server Turing
        configurationsManagement.showConf();

        //alloco le risorse estrappolate dal file di configurazione
        FunctionOutcome allocate = configurationsManagement.allocateConf();

        if(allocate == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile allocare le risorse di configurazioni del Server");
            System.exit(-1);
        }

        //*************************************ALLOCAZIONE STRUTTURE DATI *********************************************//
        System.out.println("[Turing] >> Fase di allocazione delle strutture dati");
        ServerDataStructures serverDataStructures = new ServerDataStructures();
        System.out.println("[Turing] >> Strutture dati allocate con successo");

        //*************************************CREAZIONE THREADPOOL***************************************************//
        System.out.println("[Turing] >> Fase di creazione del ThreadPool");
        //dal file di configurazione ho ricavato numero workers da attivare
        int numWorkersInThreadPool = configurationsManagement.getNumWorkersInThreadPool();

        //alloco coda di lavoro
        LinkedBlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();

        //creo ThreadPool personalizzato (faccio questo per assegnare nomi desiderati agli Workers)
        ThreadPoolExecutor threadPool = new MyExecutor(numWorkersInThreadPool, numWorkersInThreadPool, 0L,
                TimeUnit.MILLISECONDS, workingQueue);

        System.out.println("[Turing] >> ThreadPool creato con successo");

        //***************************************CREAZIONE LISTENER THREAD*********************************************//

        TuringListener listener = new TuringListener(configurationsManagement, serverDataStructures, threadPool);
        Thread thread = new Thread(listener);
        thread.start();

        //*************************************CREAZIONE SHUTDOWNHOOK*************************************************//

        System.out.println("[Turing] >> Fase di creazione del ShutdownHook");

        //In concomitanza dei segnali ( SIGINT) || (SIGQUIT) || (SIGTERM) si effettuare GRACEFUL SHUTDOWN del Server, ossia:
        //1. si soddisfanno tutte le richieste pendenti dei clients (rifiutando le nuove)
        //2. si liberano le risorse allocate
        //3. si fanno terminare tutti gli Workers e il Listener Thread
        //Per fare questo segnalo alla JVM che deve invocare il mio thread ShutDownHook come ultima istanza prima
        //di terminare il programma
        Runtime.getRuntime().addShutdownHook(new ServerShutdownHook(thread, threadPool, configurationsManagement));

        System.out.println("[Turing] >> ShutdownHook creato con successo");
    }
}
