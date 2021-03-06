import java.util.concurrent.ThreadPoolExecutor;

public class ServerShutdownHook extends Thread{
    /**
     * riferimento al thread TuringListener per farvi la join
     */
    private Thread listenerThreadID;
    /**
     * riferimento al ThreadPool
     */
    private ThreadPoolExecutor threadPoolExecutor;
    /**
     * Classe che raccoglie le variabili di configutrazione del Server
     */
    private ServerConfigurationsManagement serverConfigurationsManagement;

    /**
     * Costruttore della classe ServerShutdownHook
     * @param listenerThreadID listenerThreadID
     * @param threadPoolExecutor threadPoolExecutor
     */
    public ServerShutdownHook(Thread listenerThreadID, ThreadPoolExecutor threadPoolExecutor,
                              ServerConfigurationsManagement serverConfigurationsManagement){
        this.listenerThreadID = listenerThreadID;
        this.threadPoolExecutor = threadPoolExecutor;
        this.serverConfigurationsManagement = serverConfigurationsManagement;
    }

    /**
     * Funzione che implementa la logica per poter effettuare il Graceful ShutDown del Server:
     * 1. deallocazione delle strutture dati allocare dal Listener Thread
     * 2. terminazione del ThreadPool
     * 3. chiusa del SeverSocket (se non gia' effettuata dal try-with-resources)
     * 2. terminazione del Listener Thread
     */
    public void run(){

        System.out.println("[Turing] >> Inizio fase di GRACEFUL SHUTDOWN");

        //faccio terminare ThreadPool, con <<GRACEFUL SHUTDOWN>>:
        //1. rifiuto nuove richieste
        //2. soddisfo richieste pendenti nella coda di lavoro
        //3. attendo terminazione workers
        this.threadPoolExecutor.shutdown();

        System.out.println("[Turing] >> ThreadPool terminato");

        //cancello cartelle dal database
        this.serverConfigurationsManagement.deallocateServerConf();

        System.out.println("[Turing] >> Database svuotato");

        //interrompo TuringListener (se non gia' interroto)
        this.listenerThreadID.interrupt();

        try {
            this.listenerThreadID.join();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("[Turing] >> Listener Thread terminato");
        System.out.println("[Turing] >> Fine fase di  GRACEFUL SHUTDOWN con successo");

        System.out.println();
        System.out.println("[Turing] >> SERVER TURING (disTribUted collaboRative edItiNG) TERMINATO");
    }
}
