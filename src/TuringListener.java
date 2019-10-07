import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

public class TuringListener implements Runnable {
    private SocketAddress address;
    private ServerConfigurationsManagement configurationsManagement;
    private ServerDataStructures serverDataStructures;

    public TuringListener(){
        this.configurationsManagement = new ServerConfigurationsManagement();
        this.address = new InetSocketAddress(this.configurationsManagement.getServerHost(),
                                                                        this.configurationsManagement.getServerPort());
        this.serverDataStructures = new ServerDataStructures();
    }

    public void run() {

        //*************************************CREAZIONE THREADPOOL***************************************************//


        //*************************************CREAZIONE SHUTDOWNHOOK*************************************************//

        //In concomitanza dei segnali ( SIGINT) || (SIGQUIT) || (SIGTERM) si effettuare GRACEFUL SHUTDOWN del Server, ossia:
        //1. si soddisfanno tutte le richieste pendenti dei clients (rifiutando le nuove)
        //2. si liberano le risorse allocate
        //3. si fanno terminare tutti gli Workers e il Listener Thread
        //Per fare questo segnalo alla JVM che deve invocare il mio thread ShutDownHook come ultima istanza prima
        //di terminare il programma
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(Thread.currentThread())); //passo come argomento l'ID del Listener


        //*********************************APERTURA SERVERSOCKET E SELECTOR*******************************************//

        //provo ad aprire il ServerSocket
        //TRY-WITH-RESOURCES => e' try che si occupa di chiudere ServerSocket
        try (ServerSocketChannel server = ServerSocketChannel.open()) {

            //setto ServerSocket in modalita' NON-BLOCKING, per poter utilizzare selettore
            server.configureBlocking(false);

            //associo al ServerSocket l'indirizzo con cui Clients lo reperiranno
            server.socket().bind(address);

            //apro seletttore
            Selector selector = Selector.open();
            //registro ServerSocket al selettore
            SelectionKey serverSelectionKey = server.register(selector, SelectionKey.OP_ACCEPT);

            //*****************************************CICLO DI ASCOLTO**********************************************//

            while (true) {

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

                        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
                        Calendar calendar = Calendar.getInstance();
                        System.out.println();
                        System.out.println("[Turing] >> Accettata al tempo: |" + dateFormat.format(calendar.getTime()) + "| connessione con: " + client);
                        System.out.println();

                        //setto client in modalita' NON-BLOCKING, per poterlo utilizzare con il Selector
                        client.configureBlocking(false);

                        //registro client-socket all'operazione di lettura, per poter leggere sua prima richiesta
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);

                    }
                    if (key.isReadable()) {
                        //recupero client-socket codificato dal SelectionKey
                        SocketChannel client = (SocketChannel) key.channel();

                        if (key.attachment() == null) {

                        } else {

                        }

                        //registro client-socket all'operazione di scrittura, per potergli mandare risposta
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);

                    } else if (key.isWritable()) {
                        //recupero client-socket codificato dal SelectionKey
                        SocketChannel client = (SocketChannel) key.channel();

                        if (key.attachment() == null) {

                        } else {

                        }

                        //registro client-socket all'operazione di lettura, per poter leggere sua richiesta successiva
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
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
