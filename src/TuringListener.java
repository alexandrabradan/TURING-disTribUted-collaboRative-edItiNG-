import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TuringListener implements Runnable {
    /**
     * Indirizzo Inet del Server
     */
    private SocketAddress address;
    private ServerConfigurationsManagement configurationsManagement;
    private ServerDataStructures serverDataStructures;
    private ThreadPoolExecutor threadPool;
    private SimpleDateFormat dateFormat;  //classe necessaria per recuperare ora attuale

    /**
     * Funzione che si occupa di attivare l'RMI per consentire agli utenti di registrarsi al servizio
     * @param RMIPort porta RMI da utilizzare per il aprire il registro RMI
     * @return SUCCESS se l'attivazione del servizio di registrazione tramite RMI ha avuto successo
     *         FAILURE altrimenti
     */
    private FunctionOutcome activateRMI(int RMIPort){
        //creo oggetto remoto
        TuringRegistrationInterface turingRegistrationRMI = new TuringRegistrationRMI(this.configurationsManagement,
                                                                                            this.serverDataStructures);
        //creo stub, che Client chiamera' per utilizzare oggetto remoto del Server
        TuringRegistrationInterface stub = null;
        try {
            //remoteObj = oggetto remoto del Server
            //port = porta utilizzata per esportare l'oggetto remoto sul Registro(=0 qualsiasi)
            stub = (TuringRegistrationInterface) UnicastRemoteObject.exportObject(turingRegistrationRMI,0);

            //creo un Registro locale del Server, nel quale andro' a memoriizzare il riferimento (stub) all'oggetto
            // remoto, reperibile dai Clients
            LocateRegistry.createRegistry(RMIPort); //portaRMI di default = 1099

            //reperiamo il Registro locale appena creato
            Registry reg = LocateRegistry.getRegistry(this.configurationsManagement.getServerHost(), RMIPort);

            //associamo una chiave univoca allo stub (riferimento dell'oggetto remoto)
            //e inseriamo la coppia (chiave, stub) nel Registro => stub ora e' reperibile per qualsiasi
            //Client in grado di localizzare questo Registro locale
            reg.rebind("TURING-RMI-REGISTRATION", stub);

            return FunctionOutcome.SUCCESS;
        } catch (RemoteException e) {
            e.printStackTrace();
            return FunctionOutcome.FAILURE;
        }
    }

    public TuringListener(ServerConfigurationsManagement configurationsManagement){
        this.configurationsManagement = configurationsManagement;
        this.address = new InetSocketAddress(this.configurationsManagement.getServerHost(),
                                                                        this.configurationsManagement.getServerPort());
        this.dateFormat = new SimpleDateFormat("hh:mm:ss");

        //*************************************ALLOCAZIONE STRUTTURE DATI *********************************************//
        System.out.println("[Turing] >> Fase di allocazione delle strutture dati");
        this.serverDataStructures = new ServerDataStructures();
        System.out.println("[Turing] >> Strutture dati allocate con successo");

        //*************************************CREAZIONE STUB PER REGISTRARE UTENTI***********************************//
        System.out.println("[Turing] >> Fase di attivazione dello stub RMI per le registrazioni");
        FunctionOutcome check = activateRMI(this.configurationsManagement.getRMIPort());

        if(check == FunctionOutcome.FAILURE){
            System.err.println("[Turing] >> Impossibile attivare stub RMI per la registrazione");
            Thread.currentThread().interrupt(); //segnalo al padre che Listener ha terminato sua esecuzione
        }

        System.out.println("[Turing] >> Stub RMI attivato con successo");

        //*************************************CREAZIONE THREADPOOL***************************************************//
        System.out.println("[Turing] >> Fase di creazione del ThreadPool");
        //dal file di configurazione ho ricavato numero workers da attivare
        int numWorkersInThreadPool = this.configurationsManagement.getNumWorkersInThreadPool();
        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numWorkersInThreadPool);

        System.out.println("[Turing] >> ThreadPool creato con successo");
    }

    public void addAgainSatisfiedSocketChannelsToSelector(Selector selector){

        System.out.println("Sono in addAgainSatisfiedSocketChannelsToSelector");

        //recupero SocketChannels soddisfatti da reinserire nel selettore
        BlockingQueue<SocketChannel> selectorKeysToReinsert =  this.serverDataStructures.getSelectorKeysToReinsert();

        for(SocketChannel socketChannel: this.serverDataStructures.getSelectorKeysToReinsert()){
            System.out.println("sock = " + socketChannel);
        }

        //rimuovo SocketChannels dalla BlockingQueue e li trasferisco in un vettore, per poter iterare tale
        //vettore e registrare nuovamente, uno a uno, i SocketChannels al selettore per leggere nuove richieste
        Vector<SocketChannel> socketChannelsList = new Vector<>();
        selectorKeysToReinsert.drainTo(socketChannelsList);

        System.out.println("socketChannelsList size = " + socketChannelsList.size());

        //@TODO verificare se devo usare "this.serverDataStructures.removeSelectorKeysToReinsert" (lo dovrebbe fare drainTo)

        for(SocketChannel socketChannel: socketChannelsList){
            try {
                socketChannel.register(selector, SelectionKey.OP_READ); //registro nuovamente SocketChannel lettura richieste
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public void run() {

        //*************************************CREAZIONE SHUTDOWNHOOK*************************************************//

        System.out.println("[Turing] >> Fase di creazione del ShutdownHook");

        //In concomitanza dei segnali ( SIGINT) || (SIGQUIT) || (SIGTERM) si effettuare GRACEFUL SHUTDOWN del Server, ossia:
        //1. si soddisfanno tutte le richieste pendenti dei clients (rifiutando le nuove)
        //2. si liberano le risorse allocate
        //3. si fanno terminare tutti gli Workers e il Listener Thread
        //Per fare questo segnalo alla JVM che deve invocare il mio thread ShutDownHook come ultima istanza prima
        //di terminare il programma
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread(), this.threadPool));


        //*********************************APERTURA SERVERSOCKET E SELECTOR*******************************************//

        System.out.println("[Turing] >> Fase di apertura del ServerSocket");

        //provo ad aprire il ServerSocket
        //TRY-WITH-RESOURCES => e' try che si occupa di chiudere ServerSocket
        try (ServerSocketChannel server = ServerSocketChannel.open()) {

            //setto ServerSocket in modalita' NON-BLOCKING, per poter utilizzare selettore
            server.configureBlocking(false);

            //associo al ServerSocket l'indirizzo con cui Clients lo reperiranno
            server.socket().bind(address);

            System.out.println("[Turing] >> ServerSokcet aperto con successo");
            System.out.println("[Turing] >> Fase di apertura del selettore");

            //apro seletttore
            Selector selector = Selector.open();

            System.out.println("[Turing] >> Selettore aperto con successo");
            System.out.println("[Turing] >> Fase di registrazione del ServerSocket al selettore");

            //registro ServerSocket al selettore
            SelectionKey serverSelectionKey = server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("[Turing] >> Registrazione del ServerSocket al selettore avvenuta con successo");

            //*****************************************CICLO DI ASCOLTO**********************************************//

            System.out.println();
            System.out.println("[Turing] >> Inizio ciclo di ascolto");

            while (!Thread.interrupted()) {

                //riaggiungo al selettore i SocketChannels che sono stati tolti per essere aggiunti alla coda di lavoro
                // e consentire agli workers di soddisfare la loro richiesta e l'invio dell'esito dell'operazione.
                //La riaggiunta permette la lettura di nuove richieste da parte di questi Clients
                addAgainSatisfiedSocketChannelsToSelector(selector);

                System.out.println("Sono nel ciclo di ascolto");

                //seleziono clients-sockets pronti per fare un'operazione di IO
                selector.select();

                //recupero lista clients-sockets pronti
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                //recupero iteratore per scorrere la lista dei channels pronti
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                //itero la lista dei clients-sockets pronti
                while (iterator.hasNext()) {

                    //recupero il clients-socket corrente
                    SelectionKey key = iterator.next();

                    //lo elimino dal Selected Keys Set(insieme del Selector che raccoglie Channel pronti per un'operazione)
                    //NON lo elimino dal Registered Keys Set(insieme del Selector che raccoglie Channels registarti a lui)
                    //perche' clients-sockets rimane registrato al Selector per operazioni future
                    iterator.remove();

                    //se ServerSocketChannel e' pronto per accettare nuove connessioni
                    if (key.isAcceptable() && key == serverSelectionKey) {

                        //recupero ServerSocketChannel codificato dal SelectionKey ottenuto in fase di registrazione
                        //del ServerSocket al Selector
                        ServerSocketChannel s = (ServerSocketChannel) key.channel();

                        SocketChannel client = s.accept();

                        //recupero ora atuale
                        Calendar calendar = Calendar.getInstance();
                        System.out.println();
                        System.out.println("[Turing] >> Accettata al tempo: |" + this.dateFormat.format(calendar.getTime())
                                                        + "| connessione con: " + client.getRemoteAddress().toString());
                        System.out.println();

                        //setto client in modalita' NON-BLOCKING, per poterlo utilizzare con il Selector
                        client.configureBlocking(false);

                        //registro client-socket all'operazione di lettura, per poter leggere sua prima richiesta
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);

                    }
                    if (key.isReadable()) {
                        //recupero client-socket codificato dal SelectionKey
                        SocketChannel client = (SocketChannel) key.channel();

                        //cancello SelectionKey del client-socket dal Selected Keys Set del selettore,
                        //per consentire ad un worker di solo di soddisfare e mandare l'esito della richiesta al Client
                        key.cancel();

                        //recupero ora atuale
                        Calendar calendar = Calendar.getInstance();
                        System.out.println();
                        System.out.println("[Turing] >> Ricevuta al tempo: |" + this.dateFormat.format(calendar.getTime())
                                + "| richiesta da: " + client.getRemoteAddress().toString());

                        //sottometto al ThreadPool il task che uno dei threads dovra' soddisfare, ossia:
                        //1. prelevare un Task dalla coda di lavoro
                        //2. legge il task prelevato (la richiesta di un Client)
                        //3. soddisfa la richiesta
                        //4. invia risposta di esito al Client
                        this.threadPool.submit(new TuringWorker(this.configurationsManagement, this.serverDataStructures,
                                                                                                            client));

                        //ri-registro del client-socket all'operazione di lettura verra' fatta non appena un Worker avra'
                        // soddisfato la richiesta attuale del Client, avra' inserito il client-socket nella coda apposita
                        // di ri-registrazione al selettore e il selettore controllando tale coda, non lo riregistrera'
                        //all'inizio di ogni ciclo di ascolto
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERR] >> Problemi I/O con ServerSocket");
            Thread.currentThread().interrupt(); //segnalo al padre che Listener ha terminato sua esecuzione
        }

    }
}
