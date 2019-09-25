public class ShutDownHook extends Thread{
    private Thread listenerThreadID;

    public ShutDownHook(Thread listenerThreadID){
        this.listenerThreadID = listenerThreadID;
    }

    /**
     * Funzione che implementa la logica per poter effettuare il Graceful ShutDown del Server:
     * 1. deallocazione delle strutture dati allocare dal Listener Thread
     * 2. terminazione del ThreadPool
     * 3. chiusa del SeverSocket (se non gia' effettuata dal try-with-resources)
     * 2. terminazione del Listener Thread
     */
    public void run(){

        //faccio terminare il Listener Thread
        this.listenerThreadID.interrupt();
    }
}
