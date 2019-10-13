import java.util.concurrent.ThreadPoolExecutor;

public class ShutdownHook extends Thread{
    private Thread listenerThreadID;
    private ThreadPoolExecutor threadPoolExecutor;

    public ShutdownHook(Thread listenerThreadID, ThreadPoolExecutor threadPoolExecutor){
        this.listenerThreadID = listenerThreadID;
        this.threadPoolExecutor = threadPoolExecutor;
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

        //faccio terminare il Listener Thread
        this.listenerThreadID.interrupt();

        System.out.println("[Turing] >> Fine fase di  GRACEFUL SHUTDOWN con successo");
    }
}
